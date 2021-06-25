package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.RenderSettings
import org.lwjgl.opengl.GL11.glViewport

object Frame {

    fun bind() {
        val index = RenderSettings.framebuffer.size - 1
        bind(
            RenderSettings.currentBuffer,
            RenderSettings.changeSizes[index],
            RenderSettings.xs[index],
            RenderSettings.ys[index],
            RenderSettings.ws[index],
            RenderSettings.hs[index]
        )
    }

    fun bindMaybe() {
        bind()
    }

    fun reset() {
        lastPtr = -2
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

        if(buffer != null && buffer.pointer <= 0){
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

            glViewport(x - GFX.deltaX, y - GFX.deltaY, w, h)
            lastX = x
            lastY = y
            lastW = w
            lastH = h
            lastPtr = ptr

            GFX.windowX = x
            GFX.windowY = GFX.height - (y + h)
            GFX.windowWidth = w
            GFX.windowHeight = h

        }
    }


}