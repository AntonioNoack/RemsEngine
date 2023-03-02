package me.anno.utils.structures.tuples

class IntPair(val first: Int, val second: Int) {
    override fun toString() = "($first,$second)"
    operator fun component1() = first
    operator fun component2() = second
    override fun hashCode() = first.hashCode() * 31 + second.hashCode()
    override fun equals(other: Any?): Boolean {
        return this === other || (other is IntPair && other.first == first && other.second == second)
    }
}