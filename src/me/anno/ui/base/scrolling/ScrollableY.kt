package me.anno.ui.base.scrolling

import kotlin.math.max

interface ScrollableY {
    val h: Int
    val childSizeY: Long // if (child is LongScrollable) child.sizeY else child.minH.toLong()
    val scrollPositionY: Double
    val maxScrollPositionY get(): Long = max(0, childSizeY - h)
    val relativeSizeY get() = h.toDouble() / childSizeY
    var scrollHardnessY: Double
    val targetScrollPositionY: Double
    fun scrollY(delta: Double)
    fun scrollY(delta: Int) = scrollY(delta.toDouble())
}