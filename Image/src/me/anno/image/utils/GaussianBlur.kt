package me.anno.image.utils

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.drawing.GFXx3D.transformUniform
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths
import me.anno.utils.types.Booleans.withFlag
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4fArrayList
import kotlin.math.max
import kotlin.math.min

object GaussianBlur {

    private val LOGGER = LogManager.getLogger(GaussianBlur::class)

    private val shader3DGaussianBlur = Shader(
        "3d-blur", ShaderLib.v3DlMasked, ShaderLib.v3DMasked, ShaderLib.y3DMasked, listOf(
            Variable(GLSLType.S2D, "tex"),
            Variable(GLSLType.V2F, "stepSize"),
            Variable(GLSLType.V1F, "steps"),
            Variable(GLSLType.V1F, "threshold")
        ), "" +
                ShaderLib.brightness +
                "void main(){\n" +
                "   vec2 uv2 = uv.xy/uv.z * 0.5 + 0.5;\n" +
                "   vec4 color;\n" +
                "   float sum = 0.0;\n" +
                // test all steps for -pixelating*2 .. pixelating*2, then average
                "   int iSteps = max(0, int(2.7 * steps));\n" +
                "   if(iSteps == 0){\n" +
                "       color = texture(tex, uv2);\n" +
                "   } else {\n" +
                "       color = vec4(0.0);\n" +
                "       for(int i=-iSteps;i<=iSteps;i++){\n" +
                "           float fi = float(i);\n" +
                "           float relativeX = fi/steps;\n" +
                "           vec4 colorHere = texture(tex, uv2 + fi * stepSize);\n" +
                "           float weight = exp(-relativeX*relativeX);\n" +
                "           sum += weight;\n" +
                "           color += vec4(max(vec3(0.0), colorHere.rgb - threshold), colorHere.a) * weight;\n" +
                "       }\n" +
                "       color /= sum;\n" +
                "   }\n" +
                "   gl_FragColor = color;\n" +
                "}"
    )

    private fun draw3DGaussianBlur(
        stack: Matrix4fArrayList,
        size: Float, w: Int, h: Int,
        threshold: Float, isFirst: Boolean,
        isFullscreen: Boolean
    ) {
        val shader = shader3DGaussianBlur
        shader.use()
        transformUniform(shader, stack)
        if (isFirst) shader.v2f("stepSize", 0f, 1f / h)
        else shader.v2f("stepSize", 1f / w, 0f)
        shader.v1f("steps", size * h)
        shader.v1f("threshold", threshold)
        val buffer = if (isFullscreen) SimpleBuffer.flatLarge else SimpleBuffer.flat11
        buffer.draw(shader)
        GFX.check()
    }

    @JvmStatic
    fun gaussianBlur(
        image: FloatArray,
        w: Int, h: Int, i0: Int,
        stride: Int, thickness: Int,
        normalize: Boolean
    ): Boolean {
        // todo for small sizes, actually use a kernel
        // box blur 3x with a third of the thickness is a nice gaussian blur approximation :),
        // which in turn is a bokeh-blur approximation
        val f0 = (thickness / 3).withFlag(1) // even results move the image -> prefer odd f0's, so we don't double the error
        val f1 = thickness - 2 * (f0 - 1)
        if (f0 < 2 && f1 < 2) return false
        val tmp1 = FloatArray(w)
        val tmp2 = FloatArray(w * h)
        var x = 1
        // if the first row in the result is guaranteed to be zero,
        // we could use the image itself as buffer; (but only we waste space in the first place ->
        // don't optimize that case)
        if (f0 > 1) {
            BoxBlur.boxBlurX(image, w, h, i0, stride, f0, false, tmp1)
            BoxBlur.boxBlurY(image, w, h, i0, stride, f0, false, tmp1, tmp2)
            BoxBlur.boxBlurX(image, w, h, i0, stride, f0, false, tmp1)
            BoxBlur.boxBlurY(image, w, h, i0, stride, f0, false, tmp1, tmp2)
            x = Maths.sq(min(w, f0) * min(h, f0))
        }
        if (f1 > 1) {
            BoxBlur.boxBlurX(image, w, h, i0, stride, f1, false, tmp1)
            BoxBlur.boxBlurY(image, w, h, i0, stride, f1, false, tmp1, tmp2)
            x *= min(w, f1) * min(h, f1)
        }
        if (normalize) {
            BoxBlur.multiply(image, w, h, i0, stride, 1f / x)
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
        GFXState.useFrame(w, h, true, target, Renderer.copyRenderer) {
            target.clearDepth()
            draw3DGaussianBlur(localTransform, size, w, h, threshold, isFirst, isFullscreen)
        }
        target.bindTexture0(
            resultIndex,
            if (true || isFirst || size == pixelSize) Filtering.NEAREST
            else Filtering.LINEAR, Clamping.CLAMP
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

        GFXState.renderPurely {

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
                GFXState.useFrame(smallerW, smallerH, false, temp2, Renderer.copyRenderer) {
                    // temp2.clearColor(0, true)
                    // draw texture 0 (masked) onto temp2
                    // todo sample multiple times...
                    GFX.copy()
                    temp2.bindTrulyNearest(0)
                }
            }

            if (debug && Key.KEY_I in Input.keysDown) LOGGER.info("$w,$h -> $smallerW,$smallerH")

            drawBlur(
                FBStack["mask-gaussian-blur-0", smallerW, smallerH, 4, true, 1, DepthBufferType.NONE],
                smallerW, smallerH, 0, threshold, true,
                isFullscreen, localTransform, size, pixelSize
            )
            drawBlur(
                FBStack["mask-gaussian-blur-1", smallerW, smallerH, 4, true, 1, DepthBufferType.NONE],
                smallerW, smallerH, resultIndex, 0f, false,
                isFullscreen, localTransform, size, pixelSize
            )

        }
    }
}