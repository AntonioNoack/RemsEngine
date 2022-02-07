package me.anno.audio.streams

class ShortPair(var left: Short, var right: Short) {

    constructor() : this(0, 0)

    fun set(left: Short, right: Short): ShortPair {
        this.left = left
        this.right = right
        return this
    }

}