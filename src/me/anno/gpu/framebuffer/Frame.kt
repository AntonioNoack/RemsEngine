package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.GFX.offsetX
import me.anno.gpu.GFX.offsetY
import me.anno.gpu.GFXState
import org.lwjgl.opengl.GL11C
import kotlin.math.max
import kotlin.math.min

object Frame {

    fun bind() {
        val index = GFXState.framebuffer.size - 1
        bind(
            GFXState.currentBuffer,
            GFXState.changeSizes[index],
            GFXState.xs[index],
            GFXState.ys[index],
            GFXState.ws[index],
            GFXState.hs[index]
        )
    }

    fun reset() {
        lastPtr = -1
    }

    fun invalidate() {
        reset()
    }

    private var lastX = 0
    private var lastY = 0
    private var lastW = 0
    private var lastH = 0
    var lastPtr = -1

    fun bind(framebuffer: IFramebuffer, changeSize: Boolean, x: Int, y: Int, w0: Int, h0: Int) {

        GFX.check()

        if (framebuffer != NullFramebuffer && framebuffer.pointer == 0 && (!changeSize || w0 < 0 || h0 < 0)) {
            framebuffer.ensure()
        }

        GFX.check()

        // made more ugly, but trying to avoid allocations as much as possible :)
        var w = w0
        var h = h0
        if (w0 < 0 || h0 < 0) {// auto
            w = framebuffer.w
            h = framebuffer.h
        }

        val ptr = framebuffer.pointer
        if (ptr != lastPtr || lastX != x || lastY != y || lastW != w || lastH != h ||
            (changeSize && framebuffer != NullFramebuffer && (framebuffer.w != w || framebuffer.h != h))
        ) {

            if (changeSize) {
                framebuffer.bindDirectly(w, h)
            } else {
                framebuffer.bindDirectly()
            }

            var offsetX = offsetX
            var offsetY = offsetY
            if (framebuffer is Framebuffer) {
                offsetX = framebuffer.offsetX
                offsetY = framebuffer.offsetY
            }

            val localX = x - offsetX
            val localY = y - offsetY

            val availableWidth = framebuffer.w
            val availableHeight = framebuffer.h

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