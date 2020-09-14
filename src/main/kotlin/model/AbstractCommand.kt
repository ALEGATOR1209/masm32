package model

data class AbstractCommand(
        val name: String,
        val opCode: Short,
        val source: AbstractOperand,
        val dest: AbstractOperand
)