package me.anno.gpu

import me.anno.gpu.GFXState.useFrame
import me.anno.utils.callbacks.I4U
import kotlin.math.max
import kotlin.math.min

object Clipping {

    @JvmStatic
    fun clip(x: Int, y: Int, w: Int, h: Int, render: () -> Unit) {
        // from the bottom to the top
        GFX.check()
        if (w < 1 || h < 1) return
        // val height = RenderState.currentBuffer?.h ?: height
        // val realY = height - (y + h)
        useFrame(x, y, w, h) {
            render()
        }
    }

    @JvmStatic
    fun clip2(x0: Int, y0: Int, x1: Int, y1: Int, render: () -> Unit) = clip(x0, y0, x1 - x0, y1 - y0, render)

    @JvmStatic
    fun clip2Save(x0: Int, y0: Int, x1: Int, y1: Int, render: () -> Unit) {
        val w = x1 - x0
        val h = y1 - y0
        if (w > 0 && h > 0) {
            clip(x0, y0, w, h, render)
        }
    }

    @JvmStatic
    fun clip2Dual(
        x0: Int, y0: Int, x1: Int, y1: Int,
        x2: Int, y2: Int, x3: Int, y3: Int,
        render: I4U // x0,y0,x1,y1
    ) {
        clip2Save(max(x0, x2), max(y0, y2), min(x1, x3), min(y1, y3)) {
            render.call(x2, y2, x3, y3)
        }
    }
}