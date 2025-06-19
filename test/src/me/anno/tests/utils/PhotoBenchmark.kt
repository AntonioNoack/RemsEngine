package me.anno.tests.utils

import me.anno.Engine
import me.anno.gpu.Blitting
import me.anno.gpu.shader.effects.FSR
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.jvm.HiddenOpenGLContext
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Reduction
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureCache
import me.anno.image.ImageWriter
import me.anno.maths.Maths.mix
import me.anno.utils.OS.desktop
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.Vector2f
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

fun main() {

    // to do load an image
    // to do scale it down properly
    // to do upscale it using FSR
    // to do compute the difference
    // to do draw that as a curve depending on the scale factor, probably logarithmic

    // the error grows linearly with the scale-down on a logarithmic scale
    // -> the sensor indeed is real

    HiddenOpenGLContext.createOpenGL()
    val source = TextureCache[desktop.getChild("papa/me/IMG_20220522_180047.jpg")].waitFor()!!

    val steps = 100
    val scale0 = 1f
    val scale1 = 20f

    val scales = FloatArray(steps) {
        val f = it / (steps - 1f)
        exp(mix(ln(scale0), ln(scale1), f))
    }

    val differenceShader = Shader(
        "difference", emptyList(), coordsUVVertexShader, uvList, listOf(
            Variable(GLSLType.S2D, "tex0"),
            Variable(GLSLType.S2D, "tex1"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ),
        "" +
                "void main(){\n" +
                "   vec3 diff = texture(tex0,uv).rgb - texture(tex1,uv).rgb;\n" +
                "   result = vec4(vec3(dot(diff,diff)),1.0);\n" +
                "}"
    ).apply { setTextureIndices("tex0", "tex1") }

    val numPixels = source.width * source.height
    val points = ArrayList<Vector2f>()
    val useFSR = true

    renderPurely {
        val reconstructed = FBStack["full", source.width, source.height, 3, false, 1, DepthBufferType.NONE]
        val difference = FBStack["diff", source.width, source.height, 3, false, 1, DepthBufferType.NONE]
        val filtering = Filtering.LINEAR
        for (scale in scales) {
            val sw = (source.width / scale).roundToIntOr()
            val sh = (source.height / scale).roundToIntOr()
            val scaledDown = FBStack["scaled", sw, sh, 3, false, 1, DepthBufferType.NONE]
            useFrame(scaledDown) {
                source.bind(0, filtering, Clamping.CLAMP)
                Blitting.copy(1, true)
            }
            if (useFSR) {
                useFrame(reconstructed) {
                    FSR.upscale(
                        scaledDown.getTexture0(),
                        0, 0, reconstructed.width, reconstructed.height,
                        flipY = true, applyToneMapping = false, withAlpha = false
                    )
                    scaledDown.bindTexture0(0, filtering, Clamping.CLAMP)
                    Blitting.copy(1, true)
                }
            }
            useFrame(difference) {
                differenceShader.use()
                val source2 = if (useFSR) reconstructed else scaledDown
                source2.bindTexture0(0, filtering, Clamping.CLAMP)
                source.bind(1, filtering, Clamping.CLAMP)
                flat01.draw(differenceShader)
            }
            val diff = sqrt(Reduction.reduce(difference.getTexture0(), Reduction.SUM).x / numPixels) * 255f
            println("$scale -> $diff, ${ln(diff)}")
            points.add(Vector2f(ln(scale), -diff * 0.1f))
        }
    }

    ImageWriter.writeImageCurve(
        512, 512, true, false,
        0, -1, 5, points, "papa/scale.png"
    )

    Engine.requestShutdown()
}