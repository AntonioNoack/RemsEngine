package me.anno.ui.base.scrolling

interface LongScrollable {

    val sizeX: Long
    val sizeY: Long

    fun setExtraScrolling(vx: Long, vy: Long)

}