package me.anno.ui.base.scrolling

import kotlin.math.max

interface ScrollableX {
    val w: Int
    val childSizeX: Long // if (child is LongScrollable) child.sizeX else child.minW.toLong()
    val scrollPositionX: Double
    val maxScrollPositionX get(): Long = max(0, childSizeX - w)
    val relativeSizeX get() = w.toDouble() / childSizeX
    fun scrollX(delta: Double)
    fun scrollX(delta: Int) = scrollX(delta.toDouble())
}