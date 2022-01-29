package me.anno.ui.base.scrolling

import me.anno.ui.Panel
import kotlin.math.max

interface ScrollableX {
    val w: Int
    val child: Panel
    var scrollPositionX: Float
    val maxScrollPositionX get() = max(0, child.minW - w)
}