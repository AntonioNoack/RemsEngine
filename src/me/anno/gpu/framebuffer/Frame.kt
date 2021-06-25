package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.RenderState
import org.lwjgl.opengl.GL11.glViewport
import kotlin.math.abs

object Frame {

    fun bind() {
        val index = RenderState.framebuffer.size - 1
        bind(
            RenderState.currentBuffer,
            RenderState.changeSizes[index],
            RenderState.xs[index],
            RenderState.ys[index],
            RenderState.ws[index],
            RenderState.hs[index]
        )
    }

    fun bindMaybe() {
        bind()
    }

    fun reset() {
        lastPtr = -2 - abs(lastPtr)
    }

    fun invalidate() {
        reset()
    }

    var lastX = 0
    var lastY = 0
    var lastW = 0
    var lastH = 0
    var lastPtr = -1

    fun bind(buffer: Framebuffer?, changeSize: Boolean, x: Int, y: Int, w: Int, h: Int) {

        if (buffer != null && buffer.pointer <= 0) {
            buffer.ensure()
        }

        val ptr = buffer?.pointer ?: -1
        // println("$ptr/$lastPtr")
        if (ptr != lastPtr || lastX != x || lastY != y || lastW != w || lastH != h) {

            if (buffer != null) {
                if (changeSize) {
                    buffer.bindDirectly(w, h, false)
                } else {
                    buffer.bindDirectly(false)
                }
            } else {
                Framebuffer.bindNullDirectly()
            }

            val x1 = x - GFX.deltaX
            val y1 = y - GFX.deltaY

            val height = buffer?.h ?: GFX.height

            // this is mirrored
            glViewport(x1, height - (y1 + h), w, h)

            lastX = x
            lastY = y
            lastW = w
            lastH = h
            lastPtr = ptr

            GFX.windowX = x
            GFX.windowY = y//GFX.height - (y + h)
            GFX.windowWidth = w
            GFX.windowHeight = h

        }
    }


}