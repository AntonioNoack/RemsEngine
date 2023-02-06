package me.anno.tests.poisson

import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.io.files.FileReference
import me.anno.utils.Sleep
import me.anno.video.VideoCreator
import kotlin.math.roundToInt

/**
 * GPU implementation of poisson reconstruction
 * todo some clamping seems to be incorrect...
 * */
@Suppress("unused")
class PoissonFramebuffer : PoissonReconstruction<Framebuffer> {

    companion object {
        @JvmField
        val dxShader = Shader(
            "dx", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.S2D, "src"),
                Variable(GLSLType.V2F, "delta")
            ), "" +
                    "void main(){\n" +
                    "   gl_FragColor = texture(src, uv+delta) - texture(src, uv-delta);\n" +
                    "}"
        )

        @JvmField
        val absDiffShader = Shader(
            "abs", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.S2D, "a"),
                Variable(GLSLType.S2D, "b")
            ), "void main(){ gl_FragColor = abs(texture(a,uv)-texture(b,uv)); }"
        ).apply { setTextureIndices("a", "b") }

        @JvmField
        val linearShader = Shader(
            "m*x+n", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.S2D, "src"),
                Variable(GLSLType.V2F, "mn")
            ), "" +
                    "void main(){\n" +
                    "   vec4 col = texture(src, uv);\n" +
                    "   gl_FragColor = vec4(mn.x*col.xyz+mn.y, col.w);\n" +
                    "}"
        )

        @JvmField
        val sumShader = Shader(
            "abs", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.S2D, "a"),
                Variable(GLSLType.S2D, "b"),
                Variable(GLSLType.S2D, "c")
            ), "void main(){ gl_FragColor = texture(a,uv)+texture(b,uv)+texture(c,uv); }"
        ).apply { setTextureIndices("a", "b", "c") }

        @JvmField
        val iterationShader = Shader(
            "abs", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.S2D, "src"),
                Variable(GLSLType.S2D, "dx"),
                Variable(GLSLType.S2D, "dy"),
                Variable(GLSLType.S2D, "blurred"),
                Variable(GLSLType.V2F, "dx1"),
                Variable(GLSLType.V2F, "dy1"),
                Variable(GLSLType.V2F, "dx2"),
                Variable(GLSLType.V2F, "dy2")
            ), "" +
                    ShaderFuncLib.noiseFunc +
                    "void main(){\n" +
                    "   vec4 a0  = texture(src, uv);\n" +
                    "   vec3 a1  = texture(src, uv-dx2).rgb;\n" +
                    "   vec3 a2  = texture(src, uv-dy2).rgb;\n" +
                    "   vec3 a3  = texture(src, uv+dx2).rgb;\n" +
                    "   vec3 a4  = texture(src, uv+dy2).rgb;\n" +
                    "   vec3 dxp = texture(dx,  uv+dx1).rgb;\n" +
                    "   vec3 dyp = texture(dy,  uv+dy1).rgb;\n" +
                    "   vec3 dxm = texture(dx,  uv-dx1).rgb;\n" +
                    "   vec3 dym = texture(dy,  uv-dy1).rgb;\n" +
                    "   vec3 t0  = ((a1 + a2 + a3 + a4) + ((dxm - dxp) + (dym - dyp))) * 0.25;\n" +
                    // with the slow path, ((dxm - dxp) + (dym - dyp)) should be multiplied by about 1.25 to keep the true dx/dy, or it will be washed out
                    // "   vec3 t1  = texture(blurred, uv).rgb;\n" +
                    // "   gl_FragColor = vec4(mix(a0.rgb,t0,0.75),a0.a);\n" +
                    // "   gl_FragColor = vec4(mix(a0.rgb,mix(t0,t1,0.05),0.75),a0.a);\n" +
                    "   gl_FragColor = vec4(clamp(t0,0.0,1.0),a0.a);\n" +
                    "}"
        ).apply { setTextureIndices("src", "dx", "dy", "blurred") }

        @JvmField
        val unsignedBlur = Shader(
            "signed-blur", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.S2D, "src"),
                Variable(GLSLType.V2F, "delta"),
                Variable(GLSLType.V1F, "sigma"),
                Variable(GLSLType.V1I, "steps")
            ), "" +
                    "void main(){\n" +
                    "   float sum = 0.0;\n" +
                    "   vec3 color = vec3(0.0);\n" +
                    "   for(int i=-steps;i<=steps;i++){\n" +
                    "       float fi = float(i);\n" +
                    "       float relativeX = fi/sigma;\n" +
                    "       vec3 colorHere = texture(src, uv + fi * delta).rgb;\n" +
                    "       float weight = exp(-relativeX*relativeX);\n" +
                    "       sum += weight;\n" +
                    "       color += colorHere * weight;\n" +
                    "   }\n" +
                    "   color /= sum;\n" + // could be precomputed
                    "   gl_FragColor = vec4(color, 1.0);\n" +
                    "}"
        )

        @JvmField
        val signedBlur = Shader(
            "signed-blur", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.S2D, "src"),
                Variable(GLSLType.V2F, "delta"),
                Variable(GLSLType.V1F, "sigma"),
                Variable(GLSLType.V1I, "steps")
            ), "" +
                    "void main(){\n" +
                    "   float sum = 0.5;\n" +
                    "   vec3 color = vec3(0.0);\n" +
                    "   for(int i=1;i<=steps;i++){\n" +
                    "       float fi = float(i);\n" +
                    "       vec2 duv = fi * delta;\n" +
                    "       float relativeX = fi/sigma;\n" +
                    "       float weight = exp(-relativeX*relativeX);\n" +
                    "       color += (texture(src, uv - duv).rgb - texture(src, uv + duv).rgb) * weight;\n" +
                    "       sum += weight;\n" +
                    "   }\n" +
                    "   color /= sum;\n" + // could be precomputed
                    "   gl_FragColor = vec4(color, 1.0);\n" +
                    "}"
        )
    }

    override fun Framebuffer.next(): Framebuffer {
        return FBStack["pf", w, h, 3, true, 1, false]
    }

    fun Framebuffer.dx(dx: Int, dy: Int, dst: Framebuffer): Framebuffer {
        useFrame(dst) {
            val shader = dxShader
            shader.use()
            shader.v2f("delta", dx / (w - 1f), dy / (h - 1f))
            bindTrulyNearest(0)
            flat01.draw(shader)
        }
        return dst
    }

    override fun Framebuffer.dx(dst: Framebuffer): Framebuffer {
        return dx(1, 0, dst)
    }

    override fun Framebuffer.dy(dst: Framebuffer): Framebuffer {
        return dx(0, 1, dst)
    }

    private fun Framebuffer.blur(sigma: Float, dx: Int, dy: Int, dst: Framebuffer, shader: Shader): Framebuffer {
        useFrame(dst) {
            shader.use()
            shader.v2f("delta", dx.toFloat() / w, dy.toFloat() / h)
            shader.v1f("sigma", sigma)
            shader.v1i("steps", (sigma * 3f).roundToInt())
            bindTrulyNearest(0)
            flat01.draw(shader)
            GFX.check()
        }
        return dst
    }

    override fun Framebuffer.blurX(sizeOf1Sigma: Float, dst: Framebuffer): Framebuffer {
        return blur(sizeOf1Sigma, 1, 0, dst, unsignedBlur)
    }

    override fun Framebuffer.blurY(sizeOf1Sigma: Float, dst: Framebuffer): Framebuffer {
        return blur(sizeOf1Sigma, 0, 1, dst, unsignedBlur)
    }

    override fun Framebuffer.blurXSigned(sizeOf1Sigma: Float, dst: Framebuffer): Framebuffer {
        return blur(sizeOf1Sigma, 1, 0, dst, signedBlur)
    }

    override fun Framebuffer.blurYSigned(sizeOf1Sigma: Float, dst: Framebuffer): Framebuffer {
        return blur(sizeOf1Sigma, 0, 1, dst, signedBlur)
    }

    override fun Framebuffer.added(b: Framebuffer, c: Framebuffer, dst: Framebuffer): Framebuffer {
        useFrame(dst) {
            val shader = sumShader
            shader.use()
            bindTrulyNearest(0)
            b.bindTrulyNearest(1)
            c.bindTrulyNearest(2)
            flat01.draw(shader)
        }
        return dst
    }

    override fun Framebuffer.added(m: Float, n: Float, dst: Framebuffer): Framebuffer {
        useFrame(dst) {
            val shader = linearShader
            shader.use()
            shader.v2f("mn", m, n)
            bindTrulyNearest(0)
            flat01.draw(shader)
        }
        return dst
    }

    override fun iterate(
        src: Framebuffer,
        dst: Framebuffer,
        dx: Framebuffer,
        dy: Framebuffer,
        blurred: Framebuffer
    ): Framebuffer {
        useFrame(dst) {
            val shader = iterationShader
            shader.use()
            shader.v2f("dx1", 1f / src.w, 0f)
            shader.v2f("dy1", 0f, 1f / src.h)
            shader.v2f("dx2", 2f / src.w, 0f)
            shader.v2f("dy2", 0f, 2f / src.h)
            src.bindTrulyNearest(0)
            dx.bindTrulyNearest(1)
            dy.bindTrulyNearest(2)
            blurred.bindTrulyNearest(3)
            flat01.draw(shader)
        }
        return dst
    }

    override fun Framebuffer.absDifference(other: Framebuffer, dst: Framebuffer): Framebuffer {
        useFrame(dst) {
            val shader = absDiffShader
            shader.use()
            bindTrulyNearest(0)
            flat01.draw(shader)
        }
        return dst
    }

    override fun Framebuffer.toImage(): Image {
        return createImage(flipY = false, withAlpha = false)
    }

    override fun Framebuffer.copyInto(dst: Framebuffer): Framebuffer {
        useFrame(dst) {
            GFX.copy(this)
        }
        return dst
    }

    override fun Framebuffer.renderVideo(iterations: Int, dst: FileReference, run: (Long) -> Framebuffer) {
        var ctr = 0L
        VideoCreator.renderVideo(w, h, 5.0, dst, iterations.toLong(), false, { callback ->
            val result = run(ctr++)
            callback(result.getTexture0() as Texture2D)
        }, null)
        while (ctr <= iterations) {
            GFX.workGPUTasks(true)
            Sleep.sleepShortly(false)
        }
    }

}