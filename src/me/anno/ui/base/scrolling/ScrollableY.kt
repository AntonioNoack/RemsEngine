package me.anno.ui.base.scrolling

import me.anno.ui.Panel
import kotlin.math.max

interface ScrollableY {
    val h: Int
    val child: Panel
    var scrollPositionY: Double
    val maxScrollPositionY get(): Long = max(0, child.minH - h).toLong()
    fun scrollY(delta: Double)
    fun scrollY(delta: Int) = scrollY(delta.toDouble())
}