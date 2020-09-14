package model

enum class AbstractOperand(
        val type: OperandType,
        val size: Size,
        val address: Short? = null
) {
    NoOperand(OperandType.Undefined, Size.Undefined),

    Constant8(OperandType.Constant, Size.Byte),
    Constant16(OperandType.Constant, Size.Word),
    Constant32(OperandType.Constant, Size.Extended),

    Memory8(OperandType.Memory, Size.Byte),
    Memory16(OperandType.Memory, Size.Word),
    Memory32(OperandType.Memory, Size.Extended),

    Reg8(OperandType.Register, Size.Byte),
    Reg16(OperandType.Register, Size.Word),
    Reg32(OperandType.Register, Size.Extended),

    AL(OperandType.Register, Size.Byte, 0b000), AX(OperandType.Register, Size.Word, 0b000), EAX(OperandType.Register, Size.Extended, 0b000),
    CL(OperandType.Register, Size.Byte, 0b001), CX(OperandType.Register, Size.Word, 0b001), ECX(OperandType.Register, Size.Extended, 0b001),
    DL(OperandType.Register, Size.Byte, 0b010), DX(OperandType.Register, Size.Word, 0b010), EDX(OperandType.Register, Size.Extended, 0b010),
    BL(OperandType.Register, Size.Byte, 0b011), BX(OperandType.Register, Size.Word, 0b011), EBX(OperandType.Register, Size.Extended, 0b011),
    AH(OperandType.Register, Size.Byte, 0b100), SP(OperandType.Register, Size.Word, 0b100), ESP(OperandType.Register, Size.Extended, 0b100),
    CH(OperandType.Register, Size.Byte, 0b101), BP(OperandType.Register, Size.Word, 0b101), EBP(OperandType.Register, Size.Extended, 0b101),
    BH(OperandType.Register, Size.Byte, 0b110), SI(OperandType.Register, Size.Word, 0b110), ESI(OperandType.Register, Size.Extended, 0b110),
    DH(OperandType.Register, Size.Byte, 0b111), DI(OperandType.Register, Size.Word, 0b111), EDI(OperandType.Register, Size.Extended, 0b111);

    fun isReg() = type == OperandType.Register
    fun isMem() = type == OperandType.Memory
    fun isConst() = type == OperandType.Constant
    fun addressOr0() = address ?: 0

    infix fun fits(other: AbstractOperand): Boolean {
        if (isConst()) return false
        if (other == NoOperand) return true
        return this.size fits other.size
    }

    infix fun canCastTo(other: AbstractOperand) = when {
        this.type != other.type -> false
        type == OperandType.Register -> canCastRegister(other)
        this == NoOperand && other == NoOperand -> true
        else -> other.size fits this.size
    }

    private fun canCastRegister(other: AbstractOperand) = when {
        this.size != other.size -> false
        this == other -> true
        this == getGeneralizedReg(other) -> true
        getGeneralizedReg(this) == other -> true
        else -> false
    }

    companion object {
        fun getRegisters(): List<AbstractOperand> = values().filter { it.type == OperandType.Register }
        fun findRegister(name: String): AbstractOperand? = values().find {
            it.type == OperandType.Register && it.name == name
        }
        fun getGeneralizedReg(reg: AbstractOperand): AbstractOperand = when (reg.size) {
            Size.Byte -> Reg8
            Size.Word -> Reg16
            Size.Extended -> Reg32
            else -> throw IllegalArgumentException("Register of size Undefined")
        }
    }
}
