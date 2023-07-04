package me.anno.ui.base.scrolling

import kotlin.math.max

interface ScrollableX {
    val width: Int
    val childSizeX: Long // if (child is LongScrollable) child.sizeX else child.minW.toLong()
    val scrollPositionX: Double
    val maxScrollPositionX get(): Long = max(0, childSizeX - width)
    val relativeSizeX get() = width.toDouble() / childSizeX
    var scrollHardnessX: Double
    val targetScrollPositionX: Double
    fun scrollX(delta: Double): Double
    fun scrollX(delta: Int) = scrollX(delta.toDouble())
}