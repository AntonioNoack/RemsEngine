package me.anno.utils.structures.tuples

class ShortPair(var first: Short, var second: Short) {

    constructor() : this(0, 0)

    fun set(first: Short, second: Short): ShortPair {
        this.first = first
        this.second = second
        return this
    }

    override fun toString(): String {
        return "($first,$second)"
    }

}