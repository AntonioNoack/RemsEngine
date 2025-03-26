package me.anno.utils.structures.lists

import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.ui.Panel
import kotlin.math.max
import kotlin.math.min

data class RedrawRequest(var panel: Panel, var p0: Int, var p1: Int) {
    constructor(panel: Panel, x0: Int, y0: Int, x1: Int, y1: Int) :
            this(panel, getSize(x0, y0), getSize(x1, y1))

    // packing/unpacking saves a little memory
    val x0 get() = getSizeX(p0)
    val x1 get() = getSizeX(p1)
    val y0 get() = getSizeY(p0)
    val y1 get() = getSizeY(p1)

    fun contains(other: RedrawRequest): Boolean {
        return x0 <= other.x0 && y0 <= other.y0 &&
                x1 >= other.x1 && y1 >= other.y1
    }

    fun overlaps(x2: Int, y2: Int, x3: Int, y3: Int): Boolean {
        return x1 >= x2 && y1 >= y2 && x0 <= x3 && y0 <= y3
    }

    fun union(x2: Int, y2: Int, x3: Int, y3: Int) {
        p0 = getSize(min(x0, x2), min(y0, y2))
        p1 = getSize(max(x1, x3), max(y1, y3))
    }
}
