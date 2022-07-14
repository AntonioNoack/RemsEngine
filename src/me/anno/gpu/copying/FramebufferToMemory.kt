package me.anno.gpu.copying

import me.anno.engine.ECSRegistry
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.drawing.DrawGradients
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.FlatShaders
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2D.Companion.readAlignment
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.Color
import me.anno.utils.OS
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector4f
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL30C
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import kotlin.math.min

object FramebufferToMemory {

    /**
     * this is a function, which works in screen space rather than UI space!!
     * */
    private fun drawTexturePure(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, ignoreAlpha: Boolean,
        applyToneMapping: Boolean = false
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = FlatShaders.flatShaderTexture.value
        shader.use()
        GFXx2D.posSize(shader, x, GFX.viewportHeight - y, w, -h)
        GFXx2D.defineAdvancedGraphicalFeatures(shader)
        shader.v4f("color", -1)
        shader.v1i("alphaMode", ignoreAlpha.toInt())
        shader.v1b("applyToneMapping", applyToneMapping)
        GFXx2D.noTiling(shader)
        val tex = texture as? Texture2D
        texture.bind(
            0,
            tex?.filtering ?: GPUFiltering.NEAREST,
            tex?.clamping ?: Clamping.CLAMP
        )
        GFX.flat01.draw(shader)
        GFX.check()
    }

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
        return createBufferedImage(w, h, fb.getTexture0(), flipY, withAlpha)
    }

    fun createImage(w: Int, h: Int, framebuffer: Framebuffer, flipY: Boolean, withAlpha: Boolean): IntImage {
        return createImage(w, h, framebuffer.getTexture0(), flipY, withAlpha)
    }

    fun createBufferedImage(w: Int, h: Int, texture: ITexture2D, flipY: Boolean, withAlpha: Boolean): BufferedImage {
        return createBufferedImage(w, h, zero, flipY, withAlpha) { x2, y2, _, _ ->
            drawTexturePure(-x2, -y2, w, h, texture, !withAlpha)
        }
    }

    fun createImage(w: Int, h: Int, texture: ITexture2D, flipY: Boolean, withAlpha: Boolean): IntImage {
        return createImage(w, h, zero, flipY, withAlpha) { x2, y2, _, _ ->
            drawTexturePure(-x2, -y2, w, h, texture, !withAlpha)
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

        val window = GFX.activeWindow!!
        val wi = window.width
        val hi = window.height

        val buffer = Texture2D.bufferPool[wi * hi * 4, false, false]

        if (clearColor != null) {
            glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w)
        }

        GFX.check()

        readAlignment(width)

        useFrame(0, 0, wi, hi, null, Renderer.colorRenderer) {

            val hm1 = height - 1

            for (x0 in 0 until width step wi) {
                for (y0 in 0 until height step hi) {

                    val partW = min(wi, width - x0)
                    val partH = min(hi, height - y0)

                    if (clearColor != null) glClear(GL_COLOR_BUFFER_BIT)
                    renderSection(x0, y0, wi, hi)

                    // wait for everything to be drawn
                    glFlush()
                    glFinish()

                    buffer.position(0)

                    Framebuffer.bindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, 0)
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

}