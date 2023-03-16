package me.anno.ui.base.constraints

@Suppress("unused")
enum class AxisAlignment(val id: Int, val xName: String, val yName: String){
    MIN(0, "Left", "Top"){
        override fun getOffset(parentW: Int, minW: Int) = 0 },
    CENTER(1, "Center", "Center"){
        override fun getOffset(parentW: Int, minW: Int) = (parentW - minW) / 2 },
    MAX(2, "Right", "Bottom"){
        override fun getOffset(parentW: Int, minW: Int) = parentW - minW },
    FILL(3, "Fill", "Fill"){
        override fun getOffset(parentW: Int, minW: Int) = 0
    };
    abstract fun getOffset(parentW: Int, minW: Int): Int
    companion object {
        @JvmStatic
        fun find(id: Int) = values().firstOrNull { it.id == id }
    }
}