package converters.utils

import converters.BYTE_PTR
import converters.DWORD_PTR
import converters.WORD_PTR
import model.AbstractOperand
import model.Command
import model.Size
import java.math.BigInteger

fun isRegister(source: String): Boolean = AbstractOperand.findRegister(source) != null
fun isMemory(source: String): Boolean = source.contains(Regex("\\[[a-z0-9* ]+]", RegexOption.IGNORE_CASE))
fun isConstant(source: String): Boolean = source.toIntOrNull(16) != null

fun parseMemory(mem: String): AbstractOperand? = when {
    mem.startsWith(BYTE_PTR) -> AbstractOperand.Memory8
    mem.startsWith(WORD_PTR) -> AbstractOperand.Memory16
    mem.startsWith(DWORD_PTR) -> AbstractOperand.Memory32
    isMemory(mem) -> AbstractOperand.Memory32
    else -> null
}

fun getMinPossibleConstant(num: Int) = when {
    num.toString(2).length < 8 -> AbstractOperand.Constant8
    num.toString(2).length < 16 -> AbstractOperand.Constant16
    num.toString(2).length < 32 -> AbstractOperand.Constant32
    else -> null
}

fun isMemoryAddress(str: String) = str.matches(Regex("[0-9a-f]{1,8}", RegexOption.IGNORE_CASE))

fun splitMemoryAddress(source: String) = source.split(Regex("]\\[")).map {
    it.replace("[", "").replace("]", "")
}

fun reverseInt(num: BigInteger, bits: Int): String {
    val str = num.toString(16).let {
        "0".repeat((bits / 4) - it.length) + it
    }
    return str.mapIndexedNotNull { i, _ -> if (i % 2 == 1 || i == str.length - 1) null else str.substring(i, i + 2) }
            .asReversed()
            .joinToString("")
}

val stackRegisters = listOf(AbstractOperand.EBP, AbstractOperand.ESP)
