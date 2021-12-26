package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import org.lwjgl.opengl.GL11.glViewport
import java.lang.IllegalArgumentException
import kotlin.math.abs

object Frame {

    fun bind() {
        val index = OpenGL.framebuffer.size - 1
        bind(
            OpenGL.currentBuffer,
            OpenGL.changeSizes[index],
            OpenGL.xs[index],
            OpenGL.ys[index],
            OpenGL.ws[index],
            OpenGL.hs[index]
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

    fun bind(buffer: IFramebuffer?, changeSize: Boolean, x: Int, y: Int, w0: Int, h0: Int) {

        if (buffer != null && buffer.pointer <= 0) {
            buffer.ensure()
        }

        // made more ugly, but trying to avoid allocations as much as possible :)
        var w = w0
        var h = h0
        if (w0 < 0 || h0 < 0) {// auto
            if (buffer != null) {
                w = buffer.w
                h = buffer.h
            } else {
                w = GFX.width
                h = GFX.height
            }
        }

        var ptr = -1
        if (buffer != null) ptr = buffer.pointer

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

            var availableWidth = GFX.width
            var availableHeight = GFX.height
            if (buffer != null) {
                availableWidth = buffer.w
                availableHeight = buffer.h
            }

            if (w > availableWidth || h > availableHeight){
                IllegalArgumentException("Viewport cannot be larger than frame! $w > $availableWidth || $h > $availableHeight, change size: $changeSize, frame: $buffer")
                    .printStackTrace()
            }

            // this is mirrored
            glViewport(x1, availableHeight - (y1 + h), w, h)

            lastX = x
            lastY = y
            lastW = w
            lastH = h
            lastPtr = ptr

            GFX.windowX = x
            GFX.windowY = y
            GFX.windowWidth = w
            GFX.windowHeight = h

        }
    }


}