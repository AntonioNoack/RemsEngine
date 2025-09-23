package me.anno.ui.input.components

class CursorPosition(var x: Int, var y: Int) : Comparable<CursorPosition> {
    constructor() : this(0, 0)

    override fun hashCode(): Int = x + y.shl(16)

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

    fun contains(end: CursorPosition, xi: Int, yi: Int): Boolean {
        return when {
            yi < y || yi > end.y -> false
            y == end.y -> xi in x until end.x
            yi == y -> xi >= x
            yi == end.y -> xi < end.x
            else -> true
        }
    }

    override fun toString() = "$x $y"

}