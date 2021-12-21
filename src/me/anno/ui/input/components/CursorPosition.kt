package me.anno.ui.input.components

class CursorPosition(var x: Int, var y: Int) : Comparable<CursorPosition> {

    override fun hashCode(): Int = x + y * 65536

    override fun compareTo(other: CursorPosition): Int = hashCode().compareTo(other.hashCode())

    override fun equals(other: Any?): Boolean {
        return other is CursorPosition && other.x == x && other.y == y
    }

    fun set(x: Int, y: Int){
        this.x = x
        this.y = y
    }

    fun set(cp: CursorPosition){
        this.x = cp.x
        this.y = cp.y
    }

    override fun toString() = "$x $y"

}