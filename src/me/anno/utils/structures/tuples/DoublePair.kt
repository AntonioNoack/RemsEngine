package me.anno.utils.structures.tuples

@Suppress("unused")
data class DoublePair(var first: Double, var second: Double) {
    override fun toString(): String = "($first,$second)"
    fun set(first: Double, second: Double): DoublePair {
        this.first = first
        this.second = second
        return this
    }
}