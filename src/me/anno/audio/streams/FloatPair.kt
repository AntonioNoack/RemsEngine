package me.anno.audio.streams

class FloatPair(var left: Float, var right: Float) {

    constructor() : this(0f, 0f)

    fun set(left: Float, right: Float): FloatPair {
        this.left = left
        this.right = right
        return this
    }

}