package me.anno.ui.base.components

@Suppress("unused")
enum class AxisAlignment(val id: Int, val xName: String, val yName: String) {
    MIN(0, "Left", "Top") {
        override fun getOffset(parentW: Int, minW: Int) = 0
        override fun getWidth(parentW: Int, minW: Int): Int = minW
    },
    CENTER(1, "Center", "Center") {
        override fun getOffset(parentW: Int, minW: Int) = (parentW - minW) / 2
        override fun getWidth(parentW: Int, minW: Int): Int = minW
    },
    MAX(2, "Right", "Bottom") {
        override fun getOffset(parentW: Int, minW: Int) = parentW - minW
        override fun getWidth(parentW: Int, minW: Int): Int = minW
    },
    FILL(3, "Fill", "Fill") {
        override fun getOffset(parentW: Int, minW: Int) = 0
        override fun getWidth(parentW: Int, minW: Int): Int = parentW
    };

    abstract fun getOffset(parentW: Int, minW: Int): Int
    abstract fun getWidth(parentW: Int, minW: Int): Int

    companion object {
        @JvmStatic
        fun find(id: Int) = entries.firstOrNull { it.id == id }
    }
}