package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.GFX.offsetX
import me.anno.gpu.GFX.offsetY
import me.anno.gpu.OpenGL
import org.lwjgl.opengl.GL11C
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

        GFX.check()

        if (buffer != null && buffer.pointer <= 0 && (!changeSize || w0 < 0 || h0 < 0)) {
            buffer.ensure()
        }

        // made more ugly, but trying to avoid allocations as much as possible :)
        var w = w0
        var h = h0
        val window = GFX.activeWindow!!
        if (w0 < 0 || h0 < 0) {// auto
            if (buffer != null) {
                w = buffer.w
                h = buffer.h
            } else {
                w = window.width
                h = window.height
            }
        }

        var ptr = 0
        if (buffer != null) ptr = buffer.pointer

        if (ptr != lastPtr || lastX != x || lastY != y || lastW != w || lastH != h ||
            (changeSize && buffer != null && (buffer.w != w || buffer.h != h))
        ) {

            if (buffer != null) {
                if (changeSize) {
                    buffer.bindDirectly(w, h)
                } else {
                    buffer.bindDirectly()
                }
            } else {
                Framebuffer.bindNullDirectly()
            }

            var offsetX = offsetX
            var offsetY = offsetY
            if (buffer is Framebuffer) {
                offsetX = buffer.offsetX
                offsetY = buffer.offsetY
            }

            val localX = x - offsetX
            val localY = y - offsetY

            var availableWidth = window.width
            var availableHeight = window.height
            if (buffer != null) {
                availableWidth = buffer.w
                availableHeight = buffer.h
            }

            GFX.viewportX = x
            GFX.viewportY = y

            var x2 = localX
            var y2 = availableHeight - (localY + h)
            if (x2 + w > availableWidth || y2 + h > availableHeight || x2 < 0 || y2 < 0) {
                /*val exception = IllegalArgumentException(
                    "Viewport cannot be larger than frame! $x2 + $w > $availableWidth || " +
                            "$y2 + $h > $availableHeight, $x2 < 0 || $y2 < 0, " +
                            "cs?: $changeSize, ($x $y) += ($w $h) | - ($offsetX $offsetY), " +
                            buffer.toString()
                )*/
                //if (buffer == null) {
               // GFX.viewportX -= min(x2, 0)
               // GFX.viewportY += min(y2, 0)
                x2 = max(x2, 0)
                y2 = max(y2, 0)
                w = min(w, availableWidth - x2)
                h = min(h, availableHeight - y2)

                // exception.printStackTrace()
                // } else throw exception
            }

            GFX.viewportWidth = w
            GFX.viewportHeight = h

            // this is mirrored
            GL11C.glViewport(x2, y2, w, h)

            lastX = x
            lastY = y
            lastW = w
            lastH = h
            lastPtr = ptr

        }

        GFX.check()

    }


}