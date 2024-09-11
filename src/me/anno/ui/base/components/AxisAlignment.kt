package me.anno.ui.base.components

@Suppress("unused")
enum class AxisAlignment(val id: Int, val xName: String, val yName: String) {
    MIN(0, "Left", "Top"),
    CENTER(1, "Center", "Center"),
    MAX(2, "Right", "Bottom"),
    FILL(3, "Fill", "Fill");

    fun getOffset(parentSize: Int, childSize: Int): Int {
        return when (this) {
            MIN, FILL -> 0
            CENTER -> (parentSize - childSize).shr(1)
            MAX -> parentSize - childSize
        }
    }

    fun getSize(parentSize: Int, childSize: Int): Int {
        return if (this == FILL) parentSize else childSize
    }

    fun getAnchor(offset: Int, size: Int): Int = offset + getOffset(size, 0)

    companion object {
        @JvmStatic
        fun find(id: Int) = entries.firstOrNull { it.id == id }
    }
}