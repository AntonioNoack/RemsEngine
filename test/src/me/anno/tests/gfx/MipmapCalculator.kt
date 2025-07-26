package me.anno.tests.gfx

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GLNames
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureHelper
import org.lwjgl.opengl.GL46C
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * OpenGLs mipmap generation runs on the CPU: it blocks, and is slow (upto 27ms for 4096² on a RTX 3070/Ryzen 5 2600)
 *   -> create a compute shader, which does the calculation for us
 *
 *   -> same performance issues -> glGenerateMipmap is not the origin of the issues :(
 * */
object MipmapCalculator {

    /**
     * bind the texture, the slot doesn't matter
     * @return whether the texture was actively bound
     * */
    fun forceBindTexture(mode: Int, pointer: Int): Boolean {
        Texture2D.boundTextures[Texture2D.activeTextureSlot] = 0
        return Texture2D.bindTexture(mode, pointer)
    }

    val shader = Shader(
        "mipmap", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList, listOf(
            Variable(GLSLType.S2D, "srcTex"),
            Variable(GLSLType.V1F, "lod"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "" +
                "void main() {\n" +
                "   result = textureLod(srcTex, uv, lod);\n" +
                "}\n"
    )

    private fun next(size: Int): Int {
        return max(size ushr 1, 1)
    }

    private fun allocateLevels(texture: Texture2D): Int {
        var width = texture.width
        var height = texture.height
        for (level in 1 until 32) {
            width = next(width)
            height = next(height)
            val format = texture.internalFormat
            GFX.check()
            GL46C.glTexImage2D(
                texture.target, level, format, width, height, 0,
                when (TextureHelper.getNumChannels(format)) {
                    1 -> GL46C.GL_RED
                    2 -> GL46C.GL_RG
                    3 -> GL46C.GL_RGB
                    4 -> GL46C.GL_RGBA
                    else -> throw NotImplementedError()
                }, GL46C.GL_UNSIGNED_BYTE, null as ByteBuffer?
            )
            GFX.check()
            if (width == 1 && height == 1) {
                return level + 1
            }
        }
        return 32
    }

    fun generateMipmaps(texture: Texture2D) {
        var width = texture.width
        var height = texture.height
        if (width <= 1 && height <= 1) return
        if (TextureHelper.getNumChannels(texture.internalFormat) == 3) {
            // LOGGER.warn("RGB textures aren't supported!")
            GL46C.glGenerateMipmap(texture.target)
        } else {

            val target = texture.target

            Texture2D.activeSlot(0) // for the shader calculation
            forceBindTexture(target, texture.pointer)
            texture.filtering

            val numLevels = allocateLevels(texture)

            val framebuffer = GL46C.glGenFramebuffers()
            Framebuffer.bindFramebuffer(GL46C.GL_FRAMEBUFFER, framebuffer)

            GL46C.glTexParameteri(target, GL46C.GL_TEXTURE_MIN_FILTER, GL46C.GL_LINEAR)
            GL46C.glTexParameteri(target, GL46C.GL_TEXTURE_MAG_FILTER, GL46C.GL_LINEAR)

            // disable blending & depth testing
            GFXState.renderPurely {
                // render
                val shader = shader
                shader.use()
                for (level in 1 until numLevels) {

                    width = next(width)
                    height = next(height)

                    GL46C.glFramebufferTexture2D(
                        GL46C.GL_FRAMEBUFFER, GL46C.GL_COLOR_ATTACHMENT0,
                        target, texture.pointer, level
                    )
                    val state = GL46C.glCheckFramebufferStatus(GL46C.GL_FRAMEBUFFER)
                    if (state != GL46C.GL_FRAMEBUFFER_COMPLETE) {
                        throw RuntimeException("Framebuffer is incomplete: ${GLNames.getErrorTypeName(state)}")
                    }

                    GL46C.glViewport(0, 0, width, height)
                    shader.v1f("lod", level - 1f)
                    SimpleBuffer.flat01.draw(shader)
                }
            }

            Framebuffer.bindFramebuffer(GL46C.GL_FRAMEBUFFER, 0)
            GL46C.glDeleteFramebuffers(framebuffer)
            // invalidate framebuffer
            Frame.invalidate()
            GFX.check()
        }
    }
}