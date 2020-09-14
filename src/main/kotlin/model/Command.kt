package model

data class Command(
        val prefix: Short? = null,
        val opCode: Short,
        val modRm: Short? = null,
        val sib: Short? = null,
        val displacement: Int? = null,
        val immediate: Int? = null,
        val name: String? = null,
        val source: Operand? = null,
        val dest: Operand? = null
)

fun Command.toCommandString() = (name ?: "[NAME NOT PROVIDED]") +
        (dest?.let { " " + it.value } ?: "") +
        (source?.let { ", " + it.value } ?: "")

private fun Command.reversedInt(num: Int?): String {
    if (num == null || source == null) return ""
    val str = num.toString(16).let {
        "0".repeat((source.size.bits / 4) - it.length) + it
    }
    return str.mapIndexedNotNull { i, _ -> if (i % 2 == 1 || i == str.length - 1) null else str.substring(i, i + 2) }
            .asReversed()
            .joinToString("")
}

private val Command.dispFieldString: String
    get() = reversedInt(displacement)
private val Command.immFieldString: String
    get() = reversedInt(immediate)

private fun Short.toHex(symbols: Int) = toString(16).let { "0".repeat(symbols - it.length) + it }

fun Command.toCodeString() = (prefix?.toHex(2) ?: "") +
        opCode.toHex(2) +
        (modRm?.toHex(2) ?: "") +
        (sib?.toHex(2) ?: "") +
        dispFieldString + immFieldString
