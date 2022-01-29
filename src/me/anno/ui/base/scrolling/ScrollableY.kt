package me.anno.ui.base.scrolling

import me.anno.ui.Panel
import kotlin.math.max

interface ScrollableY {
    val h: Int
    val child: Panel
    var scrollPositionY: Float
    val maxScrollPositionY get() = max(0, child.minH - h)
}