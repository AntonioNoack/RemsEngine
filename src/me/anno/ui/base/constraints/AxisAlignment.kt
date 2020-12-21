package me.anno.ui.base.constraints

enum class AxisAlignment(val id: Int, val xName: String, val yName: String){
    MIN(-1, "Left", "Top"){
        override fun getOffset(parentW: Int, minW: Int): Int = 0 },
    CENTER(0, "Center", "Center"){
        override fun getOffset(parentW: Int, minW: Int): Int = (parentW - minW) / 2 },
    MAX(1, "Right", "Bottom"){
        override fun getOffset(parentW: Int, minW: Int): Int = parentW - minW };
    abstract fun getOffset(parentW: Int, minW: Int): Int
    companion object {
        fun find(id: Int) = values().firstOrNull { it.id == id }
    }
}