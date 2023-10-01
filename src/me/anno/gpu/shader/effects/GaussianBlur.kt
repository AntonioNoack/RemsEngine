package me.anno.gpu.shader.effects

import me.anno.gpu.GFX
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.image.BoxBlur
import me.anno.image.BoxBlur.multiply
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.sq
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4fArrayList
import kotlin.math.max
import kotlin.math.min

object GaussianBlur {

    private val LOGGER = LogManager.getLogger(GaussianBlur::class)

    @JvmStatic
    fun gaussianBlur(
        image: FloatArray,
        w: Int, h: Int, i0: Int,
        stride: Int, thickness: Int,
        normalize: Boolean
    ): Boolean {
        // box blur 3x with a third of the thickness is a nice gaussian blur approximation :),
        // which in turn is a bokeh-blur approximation
        val f0 = thickness / 3
        val f1 = thickness - 2 * f0
        if (f0 < 2 && f1 < 2) return false
        val tmp = FloatArray(w)
        val tmp2 = FloatArray(w * (h - (thickness + 1).shr(1)))
        var x = 1
        // if the first row in the result is guaranteed to be zero,
        // we could use the image itself as buffer; (but only we waste space in the first place ->
        // don't optimize that case)
        if (f0 > 1) {
            BoxBlur.boxBlurX(image, w, h, i0, stride, f0, false, tmp)
            BoxBlur.boxBlurY(image, w, h, i0, stride, f0, false, tmp, tmp2)
            BoxBlur.boxBlurX(image, w, h, i0, stride, f0, false, tmp)
            BoxBlur.boxBlurY(image, w, h, i0, stride, f0, false, tmp, tmp2)
            x = sq(min(w, f0) * min(h, f0))
        }
        if (f1 > 1) {
            BoxBlur.boxBlurX(image, w, h, i0, stride, f1, false, tmp)
            BoxBlur.boxBlurY(image, w, h, i0, stride, f1, false, tmp, tmp2)
            x *= min(w, f1) * min(h, f1)
        }
        if (normalize) {
            multiply(image, w, h, i0, stride, 1f / x)
        }
        return true
    }

    private fun drawBlur(
        target: IFramebuffer, w: Int, h: Int, resultIndex: Int,
        threshold: Float, isFirst: Boolean,
        isFullscreen: Boolean,
        localTransform: Matrix4fArrayList, size: Float, pixelSize: Float
    ) {
        // step1
        useFrame(w, h, true, target, Renderer.copyRenderer) {
            target.clearDepth()
            GFXx3D.draw3DGaussianBlur(localTransform, size, w, h, threshold, isFirst, isFullscreen)
        }
        target.bindTexture0(
            resultIndex,
            if (true || isFirst || size == pixelSize) GPUFiltering.NEAREST
            else GPUFiltering.LINEAR, Clamping.CLAMP
        )
    }

    fun draw(
        src: IFramebuffer,
        pixelSize: Float, w: Int, h: Int, resultIndex: Int,
        threshold: Float, isFullscreen: Boolean,
        localTransform: Matrix4fArrayList
    ) {

        src.bindTrulyNearest(0)

        var size = pixelSize

        renderPurely {

            val steps = pixelSize * h
            val subSteps = (steps / 10f).toInt()

            var smallerW = w
            var smallerH = h

            val debug = false

            // sample down for large blur sizes for performance reasons
            if (debug && subSteps > 1) {
                // smallerW /= 2
                // smallerH /= 2
                smallerW = max(10, w / subSteps)
                if (debug && Key.KEY_J in Input.keysDown) smallerH = max(10, h / subSteps)
                // smallerH /= 2
                // smallerH = max(10, h / subSteps)
                size = pixelSize * smallerW / w
                // draw image on smaller thing...
                val temp2 = FBStack["mask-gaussian-blur-2", smallerW, smallerH, 4, true, 1, DepthBufferType.NONE]
                useFrame(smallerW, smallerH, false, temp2, Renderer.copyRenderer) {
                    // temp2.clearColor(0, true)
                    // draw texture 0 (masked) onto temp2
                    // todo sample multiple times...
                    GFX.copy()
                    temp2.bindTrulyNearest(0)
                }
            }

            if (debug && Key.KEY_I in Input.keysDown) LOGGER.info("$w,$h -> $smallerW,$smallerH")

            drawBlur(
                FBStack["mask-gaussian-blur-0", smallerW, smallerH, 4, true, 1, DepthBufferType.NONE], smallerW, smallerH,
                0, threshold, true, isFullscreen,
                localTransform, size, pixelSize
            )
            drawBlur(
                FBStack["mask-gaussian-blur-1", smallerW, smallerH, 4, true, 1, DepthBufferType.NONE], smallerW, smallerH,
                resultIndex, 0f, false, isFullscreen,
                localTransform, size, pixelSize
            )

        }

    }

}