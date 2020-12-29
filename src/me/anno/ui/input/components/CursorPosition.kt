package me.anno.ui.input.components

data class CursorPosition(val x: Int, val y: Int) : Comparable<CursorPosition> {
    override fun hashCode(): Int = x + y * 65536
    override fun compareTo(other: CursorPosition): Int = hashCode().compareTo(other.hashCode())
    override fun equals(other: Any?): Boolean {
        return other is CursorPosition && other.x == x && other.y == y
    }

    override fun toString() = "$x $y"
}