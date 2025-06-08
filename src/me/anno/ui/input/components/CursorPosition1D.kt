package me.anno.ui.input.components

class CursorPosition1D(var x: Int = 0) : Comparable<CursorPosition1D> {

    override fun hashCode(): Int = x

    override fun compareTo(other: CursorPosition1D): Int = hashCode().compareTo(other.hashCode())

    override fun equals(other: Any?): Boolean {
        return other is CursorPosition1D && other.x == x
    }

    fun set(x: Int) {
        this.x = x
    }

    fun set(cp: CursorPosition1D) {
        this.x = cp.x
    }

    fun contains(end: CursorPosition1D, x: Int): Boolean {
        return x in this.x until end.x
    }

    override fun toString() = x.toString()
}