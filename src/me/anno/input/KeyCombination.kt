package me.anno.input

class KeyCombination(val key: Int, val modifiers: Int, val type: Type){

    val hash = key.shl(8) + modifiers * 3 + type.hash

    enum class Type(val hash: Int){
        WHILE_DOWN(0),
        ON_REPEAT(1),
        ON_PRESS(2)
    }

    override fun hashCode() = hash
    override fun equals(other: Any?): Boolean {
        return other is KeyCombination &&
                other.hash == hash &&
                other.key == key &&
                other.modifiers == modifiers &&
                other.type == type
    }

    companion object {
        fun parse(data: String): KeyCombination? {
            return null
        }
    }

}