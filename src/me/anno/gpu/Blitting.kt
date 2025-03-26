package me.anno.gpu

import me.anno.gpu.GFXState.blendMode
import me.anno.gpu.GFXState.depthMode
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.FlatShaders.copyShader
import me.anno.gpu.shader.FlatShaders.copyShaderAnyToAny
import me.anno.gpu.shader.FlatShaders.copyShaderMS
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureHelper.getNumChannels
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.toInt

/**
 * Blitting is the process of copying the data from one texture onto another one.
 * */
object Blitting {

    @JvmStatic
    fun copy(buffer: IFramebuffer, isSRGB: Boolean) {
        copy(buffer.getTexture0MS(), isSRGB)
    }

    @JvmStatic
    fun copy(src: ITexture2D, isSRGB: Boolean) {
        Frame.bind()
        src.bindTrulyNearest(0)
        copy(src.samples, isSRGB)
    }

    @JvmStatic
    fun copy(alpha: Float, samples: Int, isSRGB: Boolean) {
        GFX.check()
        val shader = if (samples > 1) copyShaderMS else copyShader
        shader.use()
        shader.v1b("sRGB", isSRGB)
        shader.v1i("samples", samples)
        shader.v1f("alpha", alpha)
        SimpleBuffer.flat01.draw(shader)
        GFX.check()
    }

    @JvmStatic
    fun copy(samples: Int, isSRGB: Boolean) {
        GFX.check()
        val shader = if (samples > 1) copyShaderMS else copyShader
        shader.use()
        shader.v1b("sRGB", isSRGB)
        shader.v1i("samples", samples)
        shader.v1f("alpha", 1f)
        SimpleBuffer.flat01.draw(shader)
        GFX.check()
    }

    @JvmStatic
    fun copyColorAndDepth(color: ITexture2D, depth: ITexture2D, depthMask: Int, isSRGB: Boolean) {
        Frame.bind()
        color.bindTrulyNearest(0)
        depth.bindTrulyNearest(1)
        val monochrome = getNumChannels(color.internalFormat) == 1
        copyColorAndDepth(
            color.samples, monochrome,
            depth.samples, depthMask,
            isSRGB
        )
    }

    @JvmStatic
    fun copyColorAndDepth(colorSamples: Int, monochrome: Boolean, depthSamples: Int, depthMask: Int, isSRGB: Boolean) {
        GFX.check()
        assertTrue(depthMask in 0..3)
        val idx = (colorSamples > 1).toInt(8) or (depthSamples > 1).toInt(4) + depthMask
        val shader = copyShaderAnyToAny[idx]
        shader.use()
        shader.v1b("sRGB", isSRGB)
        shader.v1b("monochrome", monochrome)
        shader.v1i("colorSamples", colorSamples)
        shader.v1i("depthSamples", depthSamples)
        shader.v1i("targetSamples", GFXState.currentBuffer.samples)
        bindDepthUniforms(shader)
        SimpleBuffer.flat01.draw(shader)
        GFX.check()
    }

    @JvmStatic
    fun copyNoAlpha(buffer: ITexture2D, isSRGB: Boolean) {
        Frame.bind()
        buffer.bindTrulyNearest(0)
        copyNoAlpha(buffer.samples, isSRGB)
    }

    @JvmStatic
    fun copyNoAlpha(samples: Int, isSRGB: Boolean) {
        GFX.check()
        blendMode.use(BlendMode.DST_ALPHA) {
            val depthModeI = if (GFX.supportsClipControl) DepthMode.ALWAYS
            else DepthMode.FORWARD_ALWAYS
            depthMode.use(depthModeI) {
                val shader = if (samples > 1) copyShaderMS else copyShader
                shader.use()
                shader.v1b("sRGB", isSRGB)
                shader.v1i("samples", samples)
                shader.v1f("alpha", 1f)
                SimpleBuffer.flat01.draw(shader)
            }
        }
        GFX.check()
    }
}