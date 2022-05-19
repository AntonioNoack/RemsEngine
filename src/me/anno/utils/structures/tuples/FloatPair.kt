package me.anno.utils.structures.tuples

class FloatPair(var first: Float, var second: Float) {

    constructor() : this(0f, 0f)

    fun set(first: Float, second: Float): FloatPair {
        this.first = first
        this.second = second
        return this
    }

    override fun toString(): String {
        return "($first,$second)"
    }

}