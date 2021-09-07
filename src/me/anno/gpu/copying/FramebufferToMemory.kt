package me.anno.gpu.copying

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D.Companion.packAlignment
import me.anno.utils.Color
import org.joml.Vector4f
import org.lwjgl.opengl.GL11
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import kotlin.math.min

object FramebufferToMemory {

    /**
     * copies a framebuffer into a buffered image;
     * no matter the size of the framebuffer (which is otherwise limited)
     * */
    fun createBufferedImageFromFramebuffer(
        width: Int, height: Int,
        clearColor: Vector4f?,
        renderSection: (x: Int, y: Int, w: Int, h: Int) -> Unit
    ): BufferedImage {
        val image = BufferedImage(width, height, 1)
        val dataBuffer = image.raster.dataBuffer
        cloneFromFramebuffer(width, height, clearColor, renderSection) { length, sourceIndex, buffer, bufferIndex ->
            for (x in 0 until length) {
                val si = (x + sourceIndex) * 4
                val di = x + bufferIndex
                val argb = Color.rgba(buffer[si], buffer[si + 1], buffer[si + 2], buffer[si + 3])
                dataBuffer.setElem(di, argb)
            }
        }
        return image
    }

    fun cloneFromFramebuffer(
        width: Int, height: Int,
        clearColor: Vector4f?,
        renderSection: (x: Int, y: Int, w: Int, h: Int) -> Unit,
        fillLine: (length: Int, sourceIndex: Int, buffer: ByteBuffer, bufferIndex: Int) -> Unit
    ) {

        GFX.check()

        val wi = GFX.width
        val hi = GFX.height

        val buffer = ByteBuffer.allocateDirect(wi * hi * 4)

        if (clearColor != null) {
            GL11.glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w)
        }

        GFX.check()

        for (x0 in 0 until width step wi) {
            for (y0 in 0 until height step hi) {

                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
                renderSection(x0, y0, width, height)

                GL11.glFlush(); GL11.glFinish() // wait for everything to be drawn
                packAlignment(4 * wi)
                GL11.glReadPixels(0, 0, wi, hi, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer)
                GFX.check()

                val w2 = min(wi, width - x0)
                val h2 = min(hi, height - y0)

                val baseIndex = x0 + width * y0

                for (y in 0 until h2) {
                    val sourceIndex = wi * y
                    val bufferIndex = baseIndex + width * y
                    fillLine(w2, sourceIndex, buffer, bufferIndex)
                }

            }
        }

        MemoryUtil.memFree(buffer)

    }

}