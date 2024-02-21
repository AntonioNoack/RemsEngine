package me.anno.utils.structures.tuples

data class FloatPair(var first: Float, var second: Float) {
    fun set(first: Float, second: Float): FloatPair {
        this.first = first
        this.second = second
        return this
    }
    override fun toString(): String {
        return "($first,$second)"
    }
}