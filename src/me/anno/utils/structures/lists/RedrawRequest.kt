package me.anno.utils.structures.lists

import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.ui.Panel

data class RedrawRequest(val panel: Panel, val p0: Int, val p1: Int) {
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
}
