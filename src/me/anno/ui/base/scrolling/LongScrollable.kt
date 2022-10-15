package me.anno.ui.base.scrolling

/**
 * for panels that need extra long scrolling, e.g., through gigabytes of raw data or millions of files
 * */
interface LongScrollable {

    val sizeX: Long
    val sizeY: Long

    fun setExtraScrolling(vx: Long, vy: Long)

}