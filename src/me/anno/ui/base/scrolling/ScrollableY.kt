package me.anno.ui.base.scrolling

import me.anno.ui.base.Panel
import kotlin.math.max

interface ScrollableY {
    val h: Int
    val child: Panel
    var scrollPosition: Float
    val maxScrollPosition get() = max(0, child.minH - h)
}