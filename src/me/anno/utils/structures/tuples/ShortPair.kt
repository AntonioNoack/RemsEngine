package me.anno.utils.structures.tuples

import me.anno.maths.Maths.clamp

data class ShortPair(var first: Short, var second: Short) {

    constructor() : this(0, 0)

    fun set(first: Short, second: Short): ShortPair {
        this.first = first
        this.second = second
        return this
    }

    fun set(first: Float, second: Float): ShortPair {
        this.first = clamp(first)
        this.second = clamp(second)
        return this
    }

    private fun clamp(value: Float): Short {
        return clamp(value.toInt(), -32768, 32767).toShort()
    }

    override fun toString(): String {
        return "($first,$second)"
    }
}