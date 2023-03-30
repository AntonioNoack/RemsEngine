package me.anno.utils.structures.tuples

import me.anno.maths.Maths.clamp

class ShortPair(var first: Short, var second: Short) {

    constructor() : this(0, 0)

    fun set(first: Short, second: Short): ShortPair {
        this.first = first
        this.second = second
        return this
    }

    fun set(first: Float, second: Float): ShortPair {
        this.first = clamp(first.toInt(), -32768, 32767).toShort()
        this.second = clamp(second.toInt(), -32768, 32767).toShort()
        return this
    }

    override fun toString(): String {
        return "($first,$second)"
    }

}