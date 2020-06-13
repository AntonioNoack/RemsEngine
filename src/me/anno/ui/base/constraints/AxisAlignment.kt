package me.anno.ui.base.constraints

enum class AxisAlignment {
    MIN {
        override fun getValue(parentW: Int, minW: Int): Int = 0
    }, CENTER {
        override fun getValue(parentW: Int, minW: Int): Int = (parentW - minW) / 2
    }, MAX {
        override fun getValue(parentW: Int, minW: Int): Int = parentW - minW
    };
    abstract fun getValue(parentW: Int, minW: Int): Int
}