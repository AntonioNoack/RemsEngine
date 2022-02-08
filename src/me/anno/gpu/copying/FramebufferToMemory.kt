package me.anno.gpu.copying

import me.anno.engine.ECSRegistry
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.drawing.DrawGradients
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2D.Companion.packAlignment
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.Color
import me.anno.utils.OS
import org.joml.Vector4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL11C.*
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

object FramebufferToMemory {

    fun createBufferedImage(framebuffer: Framebuffer, flipY: Boolean, withAlpha: Boolean): BufferedImage {
        return createBufferedImage(framebuffer.w, framebuffer.h, framebuffer, flipY, withAlpha)
    }

    fun createImage(framebuffer: Framebuffer, flipY: Boolean, withAlpha: Boolean): IntImage {
        return createImage(framebuffer.w, framebuffer.h, framebuffer, flipY, withAlpha)
    }

    fun createBufferedImage(texture: ITexture2D, flipY: Boolean, withAlpha: Boolean): BufferedImage {
        return createBufferedImage(texture.w, texture.h, texture, flipY, withAlpha)
    }

    fun createImage(texture: ITexture2D, flipY: Boolean, withAlpha: Boolean): IntImage {
        return createImage(texture.w, texture.h, texture, flipY, withAlpha)
    }

    fun createBufferedImage(w: Int, h: Int, fb: Framebuffer, flipY: Boolean, withAlpha: Boolean): BufferedImage {
        return createBufferedImage(w, h, fb.getColor0(), flipY, withAlpha)
    }

    fun createImage(w: Int, h: Int, framebuffer: Framebuffer, flipY: Boolean, withAlpha: Boolean): IntImage {
        return createImage(w, h, framebuffer.getColor0(), flipY, withAlpha)
    }

    fun createBufferedImage(w: Int, h: Int, texture: ITexture2D, flipY: Boolean, withAlpha: Boolean): BufferedImage {
        return createBufferedImage(w, h, zero, flipY, withAlpha) { x2, y2, _, _ ->
            DrawTextures.drawTexturePure(-x2, -y2, w, h, texture, !withAlpha)
        }
    }

    fun createImage(w: Int, h: Int, texture: ITexture2D, flipY: Boolean, withAlpha: Boolean): IntImage {
        return createImage(w, h, zero, flipY, withAlpha) { x2, y2, _, _ ->
            DrawTextures.drawTexturePure(-x2, -y2, w, h, texture, !withAlpha)
        }
    }

    private val zero = Vector4f(0f)

    /**
     * copies a framebuffer into a buffered image;
     * no matter the size of the framebuffer (which is otherwise limited)
     * */
    fun createBufferedImage(
        width: Int, height: Int,
        clearColor: Vector4f?,
        flipY: Boolean,
        withAlpha: Boolean,
        renderSection: (x: Int, y: Int, w: Int, h: Int) -> Unit
    ): BufferedImage {
        val image = BufferedImage(width, height, if (withAlpha) 2 else 1)
        val dataBuffer = image.raster.dataBuffer
        cloneFromFramebuffer(
            width,
            height,
            clearColor,
            flipY,
            renderSection
        ) { length, sourceIndex, buffer, bufferIndex ->
            for (x in 0 until length) {
                val si = (x + sourceIndex) * 4
                val di = x + bufferIndex
                val argb = Color.rgba(buffer[si], buffer[si + 1], buffer[si + 2], buffer[si + 3])
                dataBuffer.setElem(di, argb)
            }
        }
        return image
    }

    /**
     * copies a framebuffer into an int image;
     * no matter the size of the framebuffer (which is otherwise limited)
     * */
    fun createImage(
        width: Int, height: Int,
        clearColor: Vector4f?,
        flipY: Boolean,
        withAlpha: Boolean,
        renderSection: (x: Int, y: Int, w: Int, h: Int) -> Unit
    ): IntImage {
        val dataBuffer = IntArray(width * height)
        val image = IntImage(width, height, dataBuffer, withAlpha)
        cloneFromFramebuffer(
            width,
            height,
            clearColor,
            flipY,
            renderSection
        ) { length, sourceIndex, buffer, bufferIndex ->
            for (x in 0 until length) {
                val si = (x + sourceIndex) * 4
                val di = x + bufferIndex
                val argb = Color.rgba(buffer[si], buffer[si + 1], buffer[si + 2], buffer[si + 3])
                dataBuffer[di] = argb
            }
        }
        return image
    }

    fun cloneFromFramebuffer(
        width: Int, height: Int,
        clearColor: Vector4f?,
        flipY: Boolean,
        renderSection: (x: Int, y: Int, w: Int, h: Int) -> Unit,
        fillLine: (length: Int, sourceIndex: Int, buffer: ByteBuffer, bufferIndex: Int) -> Unit
    ) {

        GFX.check()

        val wi = GFX.width
        val hi = GFX.height

        val buffer = Texture2D.bufferPool[wi * hi * 4, false]

        if (clearColor != null) {
            glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w)
        }

        GFX.check()

        packAlignment(width)

        useFrame(0, 0, wi, hi, false, null, Renderer.colorRenderer) {

            val hm1 = height - 1

            for (x0 in 0 until width step wi) {
                for (y0 in 0 until height step hi) {

                    val partW = min(wi, width - x0)
                    val partH = min(hi, height - y0)

                    if (clearColor != null) glClear(GL_COLOR_BUFFER_BIT)
                    renderSection(x0, y0, wi, hi)

                    glFlush()
                    glFinish() // wait for everything to be drawn
                    buffer.position(0)
                    glReadPixels(0, 0, partW, partH, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
                    GFX.check()

                    for (y in 0 until partH) {
                        val srcIndex = partW * y
                        val dstIndex = x0 + width * (if (flipY) hm1 - (y0 + y) else y0 + y)
                        fillLine(partW, srcIndex, buffer, dstIndex)
                    }
                }
            }

        }

        Texture2D.bufferPool.returnBuffer(buffer)

    }

    @JvmStatic
    fun main(args: Array<String>) {
        // test odd widths
        // fixed the bug :)
        val s = 31
        ECSRegistry.initWithGFX(s + s.and(1))
        val fb = FBStack["", s, s, 4, false, 1, false]
        useFrame(fb) {
            val black = 255 shl 24
            // random color, so we can observe changes in the preview icon
            val color = (Math.random() * (1 shl 24)).toInt() or 0x333333
            DrawGradients.drawRectGradient(
                0, 0, s, s, color or black, black
            )
        }
        val image = createImage(fb, false, withAlpha = false)
        image.write(getReference(OS.desktop, "odd.png"))
    }

}