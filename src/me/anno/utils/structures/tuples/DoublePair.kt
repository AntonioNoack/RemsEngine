package me.anno.utils.structures.tuples

class DoublePair(var first: Double, var second: Double) {

    constructor() : this(0.0, 0.0)

    fun set(first: Double, second: Double): DoublePair {
        this.first = first
        this.second = second
        return this
    }

    override fun toString(): String {
        return "($first,$second)"
    }

}