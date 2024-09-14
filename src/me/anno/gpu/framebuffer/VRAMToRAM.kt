package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.shader.FlatShaders
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D.Companion.setReadAlignment
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.Texture3D
import me.anno.image.raw.IntImage
import me.anno.utils.Color
import me.anno.utils.pooling.Pools
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector4f
import org.lwjgl.opengl.GL46C.GL_READ_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_RGBA
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL46C.glFinish
import org.lwjgl.opengl.GL46C.glFlush
import org.lwjgl.opengl.GL46C.glReadPixels
import java.nio.ByteBuffer
import kotlin.math.min

object VRAMToRAM {

    fun interface SectionRenderer {
        fun render(x: Int, y: Int, width: Int, height: Int)
    }

    fun interface LineFiller {
        fun fill(length: Int, sourceIndex: Int, buffer: ByteBuffer, bufferIndex: Int)
    }

    /**
     * this is a function, which works in screen space rather than UI space!!
     * */
    fun drawTexturePure(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, ignoreAlpha: Boolean,
        applyToneMapping: Boolean = false
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        // we could use an easier shader here
        val shader = when (texture) {
            is Texture2DArray -> FlatShaders.flatShader2DArraySlice
            is Texture3D -> FlatShaders.flatShaderTexture3D
            is CubemapTexture -> FlatShaders.flatShaderCubemap
            else -> FlatShaders.flatShaderTexture
        }.value
        shader.use()
        GFXx2D.posSize(shader, x, GFX.viewportHeight - y, w, -h)
        shader.v4f("color", -1)
        shader.v1i("alphaMode", ignoreAlpha.toInt())
        shader.v1b("applyToneMapping", applyToneMapping)
        shader.v1f("layer", 0f)
        GFXx2D.noTiling(shader)
        texture.bind(0)
        SimpleBuffer.flat01.draw(shader)
        GFX.check()
    }

    val zero = Vector4f(0f)

    /**
     * copies a framebuffer into an int image;
     * no matter the size of the framebuffer (which is otherwise limited)
     * */
    fun createImage(
        width: Int, height: Int,
        clearColor: Vector4f?,
        flipY: Boolean,
        withAlpha: Boolean,
        renderer: SectionRenderer
    ): IntImage = createImage(IntImage(width, height, withAlpha), clearColor, flipY, renderer)

    /**
     * copies a framebuffer into an int image;
     * no matter the size of the framebuffer (which is otherwise limited)
     * */
    fun createImage(
        image: IntImage,
        clearColor: Vector4f?,
        flipY: Boolean,
        renderer: SectionRenderer
    ): IntImage {
        val dataBuffer = image.data
        cloneFromFramebuffer(
            image.width, image.height,
            clearColor, flipY, renderer
        ) { length, sourceIndex, buffer, bufferIndex ->
            for (x in 0 until length) {
                val srcI = (x + sourceIndex) * 4
                val dstI = x + bufferIndex
                dataBuffer[dstI] = Color.rgba(buffer[srcI], buffer[srcI + 1], buffer[srcI + 2], buffer[srcI + 3])
            }
        }
        return image
    }

    fun cloneFromFramebuffer(
        width: Int, height: Int,
        clearColor: Vector4f?,
        flipY: Boolean,
        renderer: SectionRenderer,
        lineFiller: LineFiller
    ) {

        GFX.check()

        val window = GFX.activeWindow!!
        val wi = window.width
        val hi = window.height

        val buffer = Pools.byteBufferPool[wi * hi * 4, false, false]

        GFX.check()

        useFrame(0, 0, wi, hi, NullFramebuffer, Renderer.copyRenderer) {

            setReadAlignment(width)

            val hm1 = height - 1

            for (x0 in 0 until width step wi) {
                for (y0 in 0 until height step hi) {

                    val partW = min(wi, width - x0)
                    val partH = min(hi, height - y0)

                    if (clearColor != null) NullFramebuffer.clearColor(clearColor)
                    renderer.render(x0, y0, wi, hi)

                    // wait for everything to be drawn
                    glFlush()
                    glFinish()

                    buffer.position(0)

                    Framebuffer.bindFramebuffer(GL_READ_FRAMEBUFFER, 0)
                    glReadPixels(0, 0, partW, partH, GL_RGBA, GL_UNSIGNED_BYTE, buffer)

                    GFX.check()

                    for (y in 0 until partH) {
                        val srcIndex = partW * y
                        val dstIndex = x0 + width * (if (flipY) hm1 - (y0 + y) else y0 + y)
                        lineFiller.fill(partW, srcIndex, buffer, dstIndex)
                    }
                }
            }

        }

        Pools.byteBufferPool.returnBuffer(buffer)
    }
}