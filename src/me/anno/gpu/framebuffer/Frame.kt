package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.GFX.offsetX
import me.anno.gpu.GFX.offsetY
import me.anno.gpu.OpenGL
import org.lwjgl.opengl.GL11.glViewport
import kotlin.math.abs
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

        GFX.check()

        if (buffer != null && buffer.pointer <= 0 && (!changeSize || w0 < 0 || h0 < 0)) {
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

        if (ptr != lastPtr || lastX != x || lastY != y || lastW != w || lastH != h ||
            (changeSize && buffer != null && (buffer.w != w || buffer.h != h))
        ) {

            if (buffer != null) {
                if (changeSize) {
                    buffer.bindDirectly(w, h, false)
                } else {
                    buffer.bindDirectly(false)
                }
            } else {
                Framebuffer.bindNullDirectly()
            }

            val offsetX = if (buffer is Framebuffer) buffer.offsetX else offsetX
            val offsetY = if (buffer is Framebuffer) buffer.offsetY else offsetY
            val x1 = x - offsetX
            val y1 = y - offsetY


            var availableWidth = GFX.width
            var availableHeight = GFX.height
            if (buffer != null) {
                availableWidth = buffer.w
                availableHeight = buffer.h
            }

            val y2 = availableHeight - (y1 + h)
            if (x1 + w > availableWidth || y2 + h > availableHeight) {
                IllegalArgumentException(
                    "Viewport cannot be larger than frame! $x1 + $w > $availableWidth || " +
                            "$y2 + $h > $availableHeight, $x1 < 0 || $y2 < 0, " +
                            "change size: $changeSize, frame: $buffer, ($x $y) += ($w $h) | - ($offsetX $offsetY), " +
                            OpenGL.framebuffer.joinToString { (it as? Framebuffer)?.name.toString() } + ", ${buffer.toString()}"
                ).printStackTrace()
            }

            // this is mirrored
            glViewport(
                x1, y2,
                min(w, availableWidth),
                min(h, availableHeight)
            )

            lastX = x
            lastY = y
            lastW = w
            lastH = h
            lastPtr = ptr

            GFX.viewportX = x
            GFX.viewportY = y
            GFX.viewportWidth = w
            GFX.viewportHeight = h

        }

        GFX.check()

    }


}