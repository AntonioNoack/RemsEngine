package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.RenderState
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11.glViewport
import kotlin.math.abs

object Frame {

    private val LOGGER = LogManager.getLogger(Frame::class)

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

    fun bind(buffer: IFramebuffer?, changeSize: Boolean, x: Int, y: Int, w0: Int, h0: Int) {

        if (buffer != null && buffer.pointer <= 0) {
            buffer.ensure()
        }

        val w = if (w0 < 0) buffer?.w ?: GFX.width else w0
        val h = if (h0 < 0) buffer?.h ?: GFX.height else h0

        val ptr = buffer?.pointer ?: -1
        // LOGGER.info("$ptr/$lastPtr")
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

            val width = buffer?.w ?: GFX.width
            val height = buffer?.h ?: GFX.height
            if (w > width || h > height) LOGGER.warn("Viewport cannot be larger than frame! $w > $width || $h > $height, frame: $buffer")

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