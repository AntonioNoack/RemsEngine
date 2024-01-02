package me.anno.ui.base.scrolling

import kotlin.math.max

interface ScrollableY {
    val height: Int
    val childSizeY: Long // if (child is LongScrollable) child.sizeY else child.minH.toLong()
    val scrollPositionY: Double
    val maxScrollPositionY get(): Long = max(0, childSizeY - height)
    val relativeSizeY get() = height.toDouble() / childSizeY
    var scrollHardnessY: Double
    val targetScrollPositionY: Double
    /**
     * returns the remaining scroll amount
     * */
    fun scrollY(delta: Double): Double
    fun scrollY(delta: Int) = scrollY(delta.toDouble())
}