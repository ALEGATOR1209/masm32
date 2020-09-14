package model

enum class Size(val bits: Int) {
    Byte(8), Word(16), Extended(32), Undefined(0);

    infix fun fits(other: Size) = when (this) {
        Byte -> other == Byte
        Word -> other == Byte || other == Word
        Extended -> other != Undefined
        Undefined -> false
    }
}
