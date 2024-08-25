package me.anno.utils.structures.lists

import me.anno.ui.Panel

data class RedrawRequest(val panel: Panel, val x0: Int, val y0: Int, val x1: Int, val y1: Int) {
    fun contains(other: RedrawRequest): Boolean {
        return x0 <= other.x0 && y0 <= other.y0 &&
                x1 >= other.x1 && y1 >= other.y1
    }
}
