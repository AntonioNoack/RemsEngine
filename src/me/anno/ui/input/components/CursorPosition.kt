package me.anno.ui.input.components

class CursorPosition(var x: Int, var y: Int) : Comparable<CursorPosition> {
    constructor() : this(0, 0)

    override fun hashCode(): Int = x + y * 65536

    override fun compareTo(other: CursorPosition): Int = hashCode().compareTo(other.hashCode())

    override fun equals(other: Any?): Boolean {
        return other is CursorPosition && other.x == x && other.y == y
    }

    fun set(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun set(cp: CursorPosition) {
        this.x = cp.x
        this.y = cp.y
    }

    fun contains(end: CursorPosition, x: Int, y: Int): Boolean {
        return when {
            y < this.y || y > end.y -> false
            this.y == end.y -> x in this.x until end.x
            y == this.y -> x >= this.x
            y == end.y -> x < end.x
            else -> true
        }
    }

    override fun toString() = "$x $y"

}