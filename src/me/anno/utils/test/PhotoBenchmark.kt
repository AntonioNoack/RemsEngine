package me.anno.utils.test

import me.anno.Engine
import me.anno.ecs.components.shaders.effects.FSR
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL.renderPurely
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Reduction
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.image.ImageGPUCache
import me.anno.image.ImageWriter
import me.anno.maths.Maths.mix
import me.anno.utils.OS.desktop
import org.joml.Vector2f
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
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
    val source = ImageGPUCache.getImage(desktop.getChild("papa/me/IMG_20220522_180047.jpg"), false)!!

    val steps = 100
    val scale0 = 1f
    val scale1 = 20f

    val scales = FloatArray(steps) {
        val f = it / (steps - 1f)
        exp(mix(ln(scale0), ln(scale1), f))
    }

    val differenceShader = Shader(
        "difference",
        coordsList,
        coordsVShader,
        uvList,
        listOf(
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

    val numPixels = source.w * source.h
    val points = ArrayList<Vector2f>()
    val useFSR = true

    renderPurely {
        val reconstructed = FBStack["full", source.w, source.h, 3, false, 1, false]
        val difference = FBStack["diff", source.w, source.h, 3, false, 1, false]
        val filtering = GPUFiltering.LINEAR
        for (scale in scales) {
            val sw = (source.w / scale).roundToInt()
            val sh = (source.h / scale).roundToInt()
            val scaledDown = FBStack["scaled", sw, sh, 3, false, 1, false]
            useFrame(scaledDown) {
                source.bind(0, filtering, Clamping.CLAMP)
                GFX.copy()
            }
            if (useFSR) {
                useFrame(reconstructed) {
                    FSR.upscale(
                        scaledDown.getTexture0(),
                        0, 0, reconstructed.w, reconstructed.h,
                        true, applyToneMapping = false
                    )
                    scaledDown.bindTexture0(0, filtering, Clamping.CLAMP)
                    GFX.copy()
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
        512, 512, true,
        0, -1, 5, points, "papa/scale.png"
    )

    Engine.requestShutdown()

}