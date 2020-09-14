package converters

import converters.utils.*
import model.*
import java.math.BigInteger
import kotlin.experimental.and
import kotlin.experimental.or

class Masm32Converter(private val dictionary: Dictionary) : Converter {
    override fun commandFromString(string: String): List<Command> {
        val s = string.trim().toUpperCase()
        val name = s.substringBefore(' ')
        val dest = s.substringAfter(name).substringBefore(',').trim()
        val source = s.substringAfter(',').trim().let { if (it == s) "" else it }

        return filterCommands(dictionary getCommandsWithName name, source, dest).map {
            formCommand(it, source, dest)
        }.ifEmpty {
            commandNotFound()
        }
    }

    private fun filterCommands(vars: List<AbstractCommand>, source: String, dest: String): List<AbstractCommand> {
        val sourceOp = getAbstractOperand(source) ?: AbstractOperand.NoOperand
        val destOp = getAbstractOperand(dest) ?: commandNotFound()

        if (!destOp.fits(sourceOp)) commandNotFound()
        return vars.filter { (sourceOp canCastTo it.source) and (destOp canCastTo it.dest) }
    }

    private fun getAbstractOperand(op: String) = when {
        isRegister(op) -> AbstractOperand.findRegister(op) ?: commandNotFound()
        isMemory(op) -> parseMemory(op) ?: commandNotFound()
        isConstant(op) -> getMinPossibleConstant(op.toInt(16)) ?: commandNotFound()
        else -> null
    }

    private fun formCommand(template: AbstractCommand, source: String, dest: String) = when (template.dest.type) {
        OperandType.Register -> commandRegDest(template, source, dest)
        OperandType.Memory -> commandMemDest(template, source, dest)
        OperandType.Undefined -> Command(opCode = template.opCode)
        else -> commandNotFound()
    }

    private fun commandRegDest(template: AbstractCommand, source: String, dest: String): Command {
        val reg = AbstractOperand.findRegister(dest) ?: commandNotFound()
        return when (template.source.type) {
            OperandType.Register -> commandRegReg(template, source, reg)
            OperandType.Memory -> commandRegMem(template, source, reg)
            OperandType.Constant -> commandRegConst(template, source, reg)
            OperandType.Undefined -> Command(
                    opCode = template.opCode,
                    name = template.name,
                    dest = Operand(template.dest.name, template.dest.type, template.dest.size)
            )
        }
    }

    private fun commandRegConst(template: AbstractCommand, source: String, dest: AbstractOperand) = Command(
            opCode = template.opCode,
            immediate = source.toInt(16),
            name = template.name,
            source = Operand(source, template.source.type, template.source.size),
            dest = Operand(dest.name, dest.type, dest.size)
    )

    private fun commandRegReg(template: AbstractCommand, source: String, dest: AbstractOperand): Command {
        var modRm: Short = 0b11_000_000
        var shifted = (dest.addressOr0().toInt() shl 3).toShort()
        modRm = modRm or shifted
        val sourceReg = AbstractOperand.findRegister(source) ?: commandNotFound()
        shifted = sourceReg.addressOr0()
        modRm = modRm or shifted

        return Command(
                opCode = template.opCode,
                modRm = modRm,
                source = Operand(sourceReg.name, sourceReg.type, sourceReg.size),
                dest = Operand(dest.name, dest.type, dest.size),
                name = template.name
        )
    }

    private fun commandRegMem(template: AbstractCommand, source: String, dest: AbstractOperand): Command {
        val opSource = Operand(source, template.source.type, template.source.size)
        val split = splitMemoryAddress(source)

        return when (split.size) {
            1 -> commandRegMem1(template, split[0].substringAfter('[').substringBefore(']'), dest)
            2 -> commandRegMem2(template, opSource, dest, split)
            3 -> commandRegMem3(template, opSource, dest, split)
            else -> commandNotFound()
        }
    }

    private fun commandRegMem1(template: AbstractCommand, source: String, dest: AbstractOperand): Command {
        var modRm: Short
        var sib: Short? = null
        var displacement: Int? = null
        val indexReg = AbstractOperand.findRegister(source)

        when {
            // mov eax, [00112233]
            isMemoryAddress(source) -> {
                modRm = 0b00_000_101 // rm = 101 -> Disp32
                val fieldReg = (dest.addressOr0().toInt() shl 3).toShort()
                modRm = modRm or fieldReg
                displacement = source.toInt(16)
            }
            // mov eax, [ebx]
            indexReg != null -> {
                if (indexReg in stackRegisters) commandNotFound()
                modRm = 0b00_000_000
                val fieldReg = (dest.addressOr0().toInt() shl 3).toShort()
                val fieldRm = indexReg.addressOr0()
                modRm = modRm or fieldReg or fieldRm
            }
            // mov eax, [ebx * 2]
            source.contains('*') -> {
                val (ss, reg) = parseMultiplication(source)
                val fieldReg = (dest.addressOr0().toInt() shl 3).toShort()
                val fieldRm: Short = 0b00_000_100 // rm = 100 -> Sib

                modRm = 0
                modRm = modRm or fieldReg or fieldRm

                val fieldIndex = (reg.addressOr0().toInt() shl 3).toShort()
                val fieldBase: Short = 0b101 // base = 101 -> Disp32 + ss * index

                sib = 0
                sib = sib or ss or fieldIndex or fieldBase

                displacement = 0
            }
            else -> commandNotFound()
        }

        return Command(
                opCode = template.opCode,
                modRm = modRm,
                sib = sib,
                displacement = displacement,
                name = template.name,
                source = Operand("[$source]", template.source.type, template.source.size),
                dest = Operand(dest.name, dest.type, dest.size)
        )
    }

    private fun commandRegMem2(
            template: AbstractCommand,
            source: Operand,
            dest: AbstractOperand,
            memory: List<String>
    ): Command {
        if (dest in stackRegisters) commandNotFound()

        var modRm: Short?
        var sib: Short? = null
        var displacement: Int? = null
        val reg1 = AbstractOperand.findRegister(memory[0])
        val reg2 = AbstractOperand.findRegister(memory[1])
        when {
            // mov eax, [ebx][00112233]
            reg1 != null && isMemoryAddress(memory[1]) -> {
                if (reg1 in stackRegisters) commandNotFound()
                modRm = 0b10_000_000 // mod = 10 -> reg + Disp32
                val fieldReg = (dest.addressOr0().toInt() shl 3).toShort()
                val fieldRm = reg1.addressOr0()
                modRm = modRm or fieldReg or fieldRm
                displacement = memory[1].toInt(16)
            }
            // mov eax, [00112233][ebx]
            reg2 != null && isMemoryAddress(memory[0]) -> {
                if (reg2 in stackRegisters) commandNotFound()
                modRm = 0b10_000_000 // mod = 10 -> reg + Disp32
                val fieldReg = (dest.addressOr0().toInt() shl 3).toShort()
                val fieldRm = reg2.addressOr0()
                modRm = modRm or fieldReg or fieldRm
                displacement = memory[0].toInt(16)
            }
            // mov eax, [ebx][ecx]
            reg1 != null && reg2 != null -> {
                if (reg1 in stackRegisters || reg2 in stackRegisters) commandNotFound()

                modRm = 0
                val fieldReg = (dest.addressOr0().toInt() shl 3).toShort()
                val fieldRm: Short = 0b100 // rm = 100 -> Sib
                modRm = modRm or fieldReg or fieldRm

                sib = 0
                val index = (reg1.addressOr0().toInt() shl 3).toShort()
                val base = reg2.addressOr0()

                sib = sib or index or base
            }
            // mov eax, [ebx * 8][ecx]
            (memory[0].contains('*') && reg2 != null) || (memory[1].contains('*') && reg1 != null) -> {
                if (reg1 in stackRegisters || reg2 in stackRegisters) commandNotFound()

                val ss: Short
                val index: Short
                val base: Short
                if (memory[0].contains('*')) {
                    val (scale, reg) = parseMultiplication(memory[0])
                    if (reg in stackRegisters) commandNotFound()
                    ss = scale
                    index = (reg.addressOr0().toInt() shl 3).toShort()
                    base = reg2?.addressOr0() ?: 0
                } else {
                    val (scale, reg) = parseMultiplication(memory[1])
                    if (reg in stackRegisters) commandNotFound()
                    ss = scale
                    index = (reg.addressOr0().toInt() shl 3).toShort()
                    base = reg1?.addressOr0() ?: 0
                }

                modRm = 0
                val fieldReg = (dest.addressOr0().toInt() shl 3).toShort()
                val fieldRm: Short = 0b100 // rm = 100 -> Sib
                modRm = modRm or fieldReg or fieldRm

                sib = 0
                sib = sib or ss or index or base
            }
            // mov eax, [ebx * 2][00112233]
            (memory[0].contains('*') && isMemoryAddress(memory[1])) ||
            (memory[1].contains('*') && isMemoryAddress(memory[0])) -> {
                val (ss, reg) = parseMultiplication(if (memory[0].contains('*')) memory[0] else memory[1])
                if (reg in stackRegisters) commandNotFound()
                val index = (reg.addressOr0().toInt() shl 3).toShort()
                displacement = (if (memory[0].contains('*')) memory[1] else memory[0]).toInt(16)

                modRm = 0
                val fieldReg = (dest.addressOr0().toInt() shl 3).toShort()
                val fieldRm: Short = 0b100 // rm = 100 -> Sib
                modRm = modRm or fieldReg or fieldRm

                val base: Short = 0b101 // base = 101 -> ss * index + Disp32
                sib = 0
                sib = sib or ss or index or base
            }
            else -> commandNotFound()
        }

        return Command(
                opCode = template.opCode,
                modRm = modRm,
                sib = sib,
                displacement = displacement,
                name = template.name,
                source = source,
                dest = Operand(dest.name, dest.type, dest.size)
        )
    }

    private fun commandRegMem3(
            template: AbstractCommand,
            source: Operand,
            dest: AbstractOperand,
            memory: List<String>
    ): Command {
        if (dest in stackRegisters) commandNotFound()

        var modRm: Short?
        var displacement: Int? = null

        modRm = 0b10 shl 6
        val fieldReg = (dest.addressOr0().toInt() shl 3).toShort()
        val fieldRm: Short = 0b100 // rm = 100 -> Sib
        modRm = modRm or fieldReg or fieldRm

        var ss: Short = 0
        var index: Short? = null
        var base: Short? = null
        memory.forEach {
            if (isMemoryAddress(it)) displacement = it.toInt(16)
            else if (it.contains('*')) {
                if (index != null) commandNotFound()
                val (scale, reg) = parseMultiplication(it)
                if (reg in stackRegisters) commandNotFound()
                ss = scale
                index = (reg.addressOr0().toInt() shl 3).toShort()
            } else {
                val reg = AbstractOperand.findRegister(it) ?: commandNotFound()

                if (base != null) {
                    if (index != null) commandNotFound()
                    index = (reg.addressOr0().toInt() shl 3).toShort()
                } else base = reg.addressOr0()
            }
        }

        return Command(
                opCode = template.opCode,
                modRm = modRm,
                sib = ss or index!! or base!!,
                displacement = displacement,
                name = template.name,
                source = source,
                dest = Operand(dest.name, dest.type, dest.size)
        )
    }

    private fun parseMultiplication(source: String): Pair<Short, AbstractOperand> {
        val first = source.substringBefore('*').trim()
        val second = source.substringAfter('*').trim()
        val find = { reg: String -> AbstractOperand.findRegister(reg) }

        val reg = find(first) ?: find(second) ?: commandNotFound()
        if (reg in stackRegisters) commandNotFound()
        val scale = first.toIntOrNull() ?: second.toIntOrNull() ?: commandNotFound()

        val ss = (when (scale) {
            1 -> 0b00
            2 -> 0b01
            4 -> 0b10
            8 -> 0b11
            else -> commandNotFound()
        } shl 6).toShort()

        return ss to reg
    }

    private fun commandMemDest(template: AbstractCommand, source: String, dest: String): Command {
        var modRm: Short? = null
        var sib: Short? = null
        var displacement: Int?
        var immediate: Int? = null

        when (template.source.type) {
            OperandType.Register -> {
                val reg = AbstractOperand.findRegister(source) ?: commandNotFound()
                if (reg != template.source) modRm = reg.addressOr0().toInt().shl(3).toShort()
            }
            OperandType.Constant -> immediate = source.toInt(16)
            else -> commandNotFound()
        }

        val blocks = splitMemoryAddress(dest)
        if (blocks.size > 3) commandNotFound()
        val blockWithReg = blocks.find { isRegister(it) }
        val blockWithConst = blocks.find { it.toIntOrNull(16) != null }
        val blockWithMul = blocks.find { it.contains('*') || (isRegister(it) && it != blockWithReg) }

        val baseReg = blockWithReg?.let { AbstractOperand.findRegister(it) }
        val const = blockWithConst?.toInt(16)
        val (ss, indexReg) = blockWithMul?.let {
            if (it.contains('*')) parseMultiplication(it) else Pair<Short?, AbstractOperand?>(0b00, AbstractOperand.findRegister(it))
        } ?: Pair<Short?, AbstractOperand?>(null, null)

        displacement = const

        if (indexReg == null) {
            if (modRm != null) {
                val rm: Short = baseReg?.addressOr0() ?: 0b101 // rm = 101 -> Disp32
                val mod: Short = (if (displacement != null && rm != (0b101).toShort()) 0b10 else 0b00).shl(6).toShort()
                modRm = modRm or mod or rm
            } else if (displacement != null) modRm = 0b00000101
        } else {
            val mod: Short = (if (displacement != null && baseReg != null) 0b10 else 0b00).shl(6).toShort()
            val rm: Short = 0b100
            if (modRm == null) modRm = 0
            modRm = modRm or mod or rm

            val index = indexReg.addressOr0().toInt().shl(3).toShort()
            val base = baseReg?.addressOr0() ?: (0b101).apply { // base = 101 -> Disp32
                if (displacement == null) displacement = 0
            }.toShort()

            sib = ss!! or index or base
        }

        return Command(
                opCode = template.opCode,
                modRm = modRm,
                sib = sib,
                displacement = displacement,
                immediate = immediate,
                name = template.name,
                source = Operand(source, template.source.type, template.source.size),
                dest = Operand(dest, template.dest.type, template.dest.size)
        )
    }

    private fun commandNotFound(): Nothing = error(ERROR_COMMAND_NOT_FOUND)

    override fun commandFromCode(code: BigInteger): List<Command> {
        var str = code.toString(16)
        val opCode = str.substring(0, 2).toShort(16)
        str = str.substring(2)

        return dictionary.commands
                .filter { it.opCode == opCode }
                .mapNotNull {
                    try { parseCommandFromCode(it, str) }
                    catch (e: Exception) { null }
                }.also { if (it.isEmpty()) error("No commands") }
    }

    private fun parseCommandFromCode(command: AbstractCommand, code: String): Command {
        var str = code
        val nip = { n: Int -> str.let {
            val part = it.substring(0, n)
            str = str.substring(n)
            part
        }}

        val opCode = command.opCode
        val name = command.name
        var modRm: Short? = null
        var sib: Short? = null
        var displacement: Int? = null
        var immediate: Int? = null
        var opDest: Operand? = null
        var opSource: Operand? = null
        val inverted = if (command.dest.isMem()) true else (opCode and 0b000000010) == (0).toShort()

        val destIsReg = command.dest.let { it.isReg() && it != AbstractOperand.getGeneralizedReg(it) }
        val sourceIsConst = command.source.isConst() || command.source == AbstractOperand.NoOperand
        val hasModRm = !(destIsReg and sourceIsConst)
        var hasSib = false
        var hasDisp = false
        var hasImmediate = false

        if (command.source.isConst()) hasImmediate = true

        if (hasModRm) {
            modRm = nip(2).toShort(16)
            val mod = (modRm and 0b11_000_000).toInt().shr(6).toShort()
            val reg = (modRm and 0b00_111_000).toInt().shr(3).toShort()
            val rm = modRm and 0b00_000_111

            when {
                mod == (0b00).toShort() && command.source.isMem() -> {
                    if (rm == (0b101).toShort()) hasDisp = true
                    else if (rm == (0b100).toShort()) hasSib = true
                }
                mod in 0b01..0b10 -> {
                    if (!command.source.isMem() && !command.dest.isMem()) commandNotFound()
                    hasDisp = true
                }
                mod == (0b11).toShort() -> {
                    if (!command.source.isReg() && !command.dest.isReg()) commandNotFound()
                }
            }

            val opReg = AbstractOperand.getRegisters().find {
                it.address == reg && it.size == (if (inverted) command.source.size else command.dest.size)
            } ?: commandNotFound()
            if (inverted) opSource = Operand(opReg.name, opReg.type, opReg.size)
            else opDest = Operand(opReg.name, opReg.type, opReg.size)

            if (rm == (0b100).toShort()) {
                with(if (inverted) command.dest else command.source) {
                    when {
                        isMem() -> hasSib = true
                        isReg() && size == Size.Byte -> {
                            val rmReg = AbstractOperand.getRegisters().find {
                                it.address == rm && it.size == (if (inverted) command.dest.size else command.source.size)
                            } ?: commandNotFound()
                            if (inverted) opDest = Operand(rmReg.name, rmReg.type, rmReg.size)
                            else opSource = Operand(rmReg.name, rmReg.type, rmReg.size)
                        }
                        else -> commandNotFound()
                    }
                }
            }
            else if (rm == (0b101).toShort()) {
                if (!command.source.isConst() && !command.source.isMem() && !command.dest.isMem()) commandNotFound()
                hasDisp = true
            } else {
                val rmReg = AbstractOperand.getRegisters().find {
                    it.address == rm && it.size == (if (inverted) command.dest.size else command.source.size)
                } ?: commandNotFound()
                val operand = if (mod == (0b11).toShort()) Operand(rmReg.name, rmReg.type, rmReg.size)
                else Operand("[${rmReg.name}]", OperandType.Memory, Size.Extended)
                if (inverted) opDest = operand else opSource = operand
            }
        } else {
            opDest = Operand(command.dest.name, command.dest.type, command.dest.size)
            if (command.source.isConst()) hasImmediate = true
        }

        if (hasSib) {
            sib = nip(2).toShort(16)
            val ss = (sib and 0b11_000_000).toInt().shr(6)
            val index = sib.toInt().shr(3).and(0b111).toShort()
            val base = sib and 0b00_000_111

            val scale = when (ss) {
                0b00 -> ""
                0b01 -> "*2"
                0b10 -> "*4"
                0b11 -> "*8"
                else -> commandNotFound()
            }

            val indexReg = AbstractOperand.getRegisters().find {
                it.address == index && it.size == Size.Extended
            } ?: commandNotFound()
            val baseReg = AbstractOperand.getRegisters().find {
                it.address == base && it.size == Size.Extended
            } ?: commandNotFound()
            val operand: Operand
            val mod = modRm!!.and(0b11_000_000).toInt().shr(6)
            when (baseReg) {
                AbstractOperand.ESP -> commandNotFound()
                AbstractOperand.EBP -> {
                    if (mod == 0b00) {
                        hasDisp = true
                        operand = Operand(
                                "[${indexReg.name}$scale]",
                                OperandType.Memory,
                                if (inverted) command.dest.size else command.source.size
                        )
                    } else commandNotFound()
                }
                else -> {
                    if (mod in 0b01..0b10) hasDisp = true
                    operand = Operand(
                            "[${indexReg.name}$scale][${baseReg.name}]",
                            OperandType.Memory,
                            if (inverted) command.dest.size else command.source.size
                    )
                }
            }
            if (inverted) opDest = operand else opSource = operand
        }

        if (hasDisp) {
            val dispStr = reverseInt(nip(8).toBigInteger(16), Size.Extended.bits)
            displacement = dispStr.toInt(16)
            var operand = (if (inverted) opDest else opSource)
            if (displacement != 0) {
                operand = if (operand == null) Operand(
                        "[$dispStr]",
                        OperandType.Memory,
                        if (inverted) command.dest.size else command.source.size
                ) else Operand(operand.value + "[$dispStr]", operand.type, operand.size)
                if (inverted) opDest = operand else opSource = operand
            }
        }

        if (hasImmediate) {
            if (!command.source.isConst()) commandNotFound()
            val bits = command.source.size.bits
            immediate = reverseInt(nip(bits / 4).toBigInteger(16), bits).toInt(16)
            opSource = Operand(immediate.toString(16), command.source.type, command.source.size)
        }

        return Command(
                opCode = opCode,
                modRm = modRm,
                sib = sib,
                displacement = displacement,
                immediate = immediate,
                name = name,
                source = opSource,
                dest = opDest
        )
    }
}
