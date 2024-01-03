package me.anno.ui.base.components

@Suppress("unused")
enum class AxisAlignment(val id: Int, val xName: String, val yName: String) {
    MIN(0, "Left", "Top") {
        override fun getOffset(parentSize: Int, childSize: Int) = 0
        override fun getSize(parentSize: Int, childSize: Int): Int = childSize
    },
    CENTER(1, "Center", "Center") {
        override fun getOffset(parentSize: Int, childSize: Int) = (parentSize - childSize) / 2
        override fun getSize(parentSize: Int, childSize: Int): Int = childSize
    },
    MAX(2, "Right", "Bottom") {
        override fun getOffset(parentSize: Int, childSize: Int) = parentSize - childSize
        override fun getSize(parentSize: Int, childSize: Int): Int = childSize
    },
    FILL(3, "Fill", "Fill") {
        override fun getOffset(parentSize: Int, childSize: Int) = 0
        override fun getSize(parentSize: Int, childSize: Int): Int = parentSize
    };

    abstract fun getOffset(parentSize: Int, childSize: Int): Int
    abstract fun getSize(parentSize: Int, childSize: Int): Int

    companion object {
        @JvmStatic
        fun find(id: Int) = entries.firstOrNull { it.id == id }
    }
}