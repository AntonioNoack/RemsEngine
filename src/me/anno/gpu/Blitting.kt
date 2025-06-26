package me.anno.gpu

import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.FlatShaders.DEPTH_MASK_MULTIPLIER
import me.anno.gpu.shader.FlatShaders.DONT_READ_DEPTH
import me.anno.gpu.shader.FlatShaders.MULTISAMPLED_COLOR_FLAG
import me.anno.gpu.shader.FlatShaders.MULTISAMPLED_DEPTH_FLAG
import me.anno.gpu.shader.FlatShaders.copyShaderAnyToAny
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureHelper.getNumChannels
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.toInt

/**
 * Blitting is the process of copying the data from one texture onto another one.
 * There is glBlit, but I don't want to use it, because it's very limited and doesn't exist in DirectX 11.
 * */
object Blitting {

    @JvmStatic
    fun copyColor(buffer: IFramebuffer, isSRGB: Boolean) {
        copyColor(buffer.getTexture0MS(), isSRGB)
    }

    @JvmStatic
    fun copyColor(src: ITexture2D, isSRGB: Boolean) {
        Frame.bind()
        src.bindTrulyNearest(0)
        copyColor(src.samples, isSRGB)
    }

    @JvmStatic
    fun copyColorWithSpecificAlpha(alpha: Float, samples: Int, isSRGB: Boolean) {
        copyColorAndDepth(
            samples, monochrome = false,
            1, DONT_READ_DEPTH,
            convertLinearToSRGB = isSRGB,
            convertSRGBToLinear = isSRGB,
            alphaOverride = alpha
        )
    }

    @JvmStatic
    fun copyColor(samples: Int, isSRGB: Boolean) {
        copyColorAndDepth(
            samples, monochrome = false,
            1, DONT_READ_DEPTH,
            convertLinearToSRGB = isSRGB,
            convertSRGBToLinear = isSRGB,
            alphaOverride = null
        )
    }

    @JvmStatic
    fun copyColorAndDepth(
        color: ITexture2D, depth: ITexture2D, depthMask: Int,
        convertSRGBToLinear: Boolean,
        convertLinearToSRGB: Boolean
    ) {
        Frame.bind()
        color.bindTrulyNearest(0)
        depth.bindTrulyNearest(1)
        val monochrome = getNumChannels(color.internalFormat) == 1
        copyColorAndDepth(
            color.samples, monochrome,
            depth.samples, depthMask,
            convertLinearToSRGB,
            convertSRGBToLinear,
            null
        )
    }

    @JvmStatic
    fun copyColorAndDepth(
        color: ITexture2D, depth: ITexture2D, depthMask: Int,
        isSRGB: Boolean
    ) {
        copyColorAndDepth(
            color, depth, depthMask,
            convertSRGBToLinear = isSRGB,
            convertLinearToSRGB = isSRGB
        )
    }

    @JvmStatic
    fun copyColorAndDepth(
        colorSamples: Int, monochrome: Boolean,
        depthSamples: Int, depthMask: Int,
        convertSRGBToLinear: Boolean, // x²
        convertLinearToSRGB: Boolean, // sqrt(x)
        alphaOverride: Float?
    ) {
        GFX.check()
        assertTrue(depthMask in 0..3 || depthMask == 4)
        val idx = (colorSamples > 1).toInt(MULTISAMPLED_COLOR_FLAG) or
                (depthSamples > 1).toInt(MULTISAMPLED_DEPTH_FLAG) or
                (depthMask * DEPTH_MASK_MULTIPLIER)
        val shader = copyShaderAnyToAny[idx]
        shader.use()
        shader.v1b("convertSRGBToLinear", convertSRGBToLinear)
        shader.v1b("convertLinearToSRGB", convertLinearToSRGB)
        shader.v1b("overrideAlpha", alphaOverride != null)
        shader.v1f("newAlpha", alphaOverride ?: 1f)
        shader.v1b("monochrome", monochrome)
        shader.v1i("colorSamples", colorSamples)
        shader.v1i("depthSamples", depthSamples)
        shader.v1i("targetSamples", GFXState.currentBuffer.samples)
        bindDepthUniforms(shader)
        SimpleBuffer.flat01.draw(shader)
        GFX.check()
    }

    /**
     * Copy depth, retain color
     * */
    @JvmStatic
    fun copyDepth(depth: ITexture2D, depthMask: Int) {
        Frame.bind()
        depth.bindTrulyNearest(1)
        copyDepth(depth.samples, depthMask)
    }

    /**
     * Copy depth, retain color
     * */
    @JvmStatic
    fun copyDepth(depthSamples: Int, depthMask: Int) {
        GFXState.colorMask.use(0) {
            copyColorAndDepth(
                colorSamples = 1, monochrome = false,
                depthSamples, depthMask,
                convertSRGBToLinear = false,
                convertLinearToSRGB = false,
                alphaOverride = null
            )
        }
    }

    /**
     * Copy color, retain alpha channel
     * */
    @JvmStatic
    fun copyNoAlpha(buffer: ITexture2D, isSRGB: Boolean) {
        Frame.bind()
        buffer.bindTrulyNearest(0)
        copyNoAlpha(buffer.samples, isSRGB)
    }

    /**
     * Copy color, retain alpha channel;
     * could also be implemented with GFXState.colorMask(~COLOR_MASK_A)
     * */
    @JvmStatic
    fun copyNoAlpha(samples: Int, isSRGB: Boolean) {
        copyColorWithSpecificAlpha(1f, samples, isSRGB)
    }
}