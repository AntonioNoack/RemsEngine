package me.anno.utils.structures.tuples

class DoublePair(var first: Double, var second: Double) {

    fun set(first: Double, second: Double): DoublePair {
        this.first = first
        this.second = second
        return this
    }

    override fun toString() = "($first,$second)"
    operator fun component1() = first
    operator fun component2() = second
    override fun hashCode() = first.hashCode() * 31 + second.hashCode()
    override fun equals(other: Any?): Boolean {
        return this === other || (other is DoublePair && other.first == first && other.second == second)
    }

}