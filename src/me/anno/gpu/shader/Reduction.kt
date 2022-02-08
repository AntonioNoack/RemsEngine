package me.anno.gpu.shader

import me.anno.gpu.GFX
import me.anno.gpu.GFX.flat01
import me.anno.gpu.OpenGL.renderPurely
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.ShaderLib.simplestVertexShader2
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2D.Companion.packAlignment
import me.anno.image.ImageGPUCache
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.utils.Color.toHexColor
import me.anno.utils.LOGGER
import me.anno.utils.OS
import me.anno.utils.pooling.JomlPools
import org.joml.Vector4f
import org.joml.Vector4fc
import org.lwjgl.opengl.GL11C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

object Reduction {

    data class Operation(
        val name: String,
        val startValue: Vector4fc,
        val function: String,
        val normalize: Boolean,
        val kotlinImpl: (sum: Vector4f, addend: Vector4f) -> Unit,
    ) {
        constructor(
            name: String, startValue: Float, function: String, normalize: Boolean,
            kotlinImpl: (a: Vector4f, b: Vector4f) -> Unit
        ) : this(name, Vector4f(startValue), function, normalize, kotlinImpl)

        constructor(
            name: String, startValue: Float, normalize: Boolean, function: String,
            kotlinImpl: (a: Float, b: Float) -> Float
        ) : this(name, Vector4f(startValue), function, normalize, { a, b ->
            a.set(
                kotlinImpl(a.x, b.x),
                kotlinImpl(a.y, b.y),
                kotlinImpl(a.z, b.z),
                kotlinImpl(a.w, b.w)
            )
        })

        constructor(
            name: String, startValue: Float, function: String,
            kotlinImpl: (a: Float, b: Float) -> Float
        ) : this(name, Vector4f(startValue), function, false, { a, b ->
            a.set(
                kotlinImpl(a.x, b.x),
                kotlinImpl(a.y, b.y),
                kotlinImpl(a.z, b.z),
                kotlinImpl(a.w, b.w)
            )
        })
    }

    /** finds the maximum value of all, so the brightest pixel */
    val MAX = Operation("max", 1e-38f, "max(a,b)") { a, b -> max(a, b) }

    /** finds the minimum value of all, so the darkest pixel */
    val MIN = Operation("min", 1e38f, "min(a,b)") { a, b -> min(a, b) }

    /** finds the sum of all pixels; use AVG, if you want to know the average */
    val SUM = Operation("sum", 0f, "a+b") { a, b -> a + b }

    /** finds the maximum amplitude */
    val MAX_ABS = Operation("max-abs", 0f, "max(abs(a),abs(b))") { a, b -> max(abs(a), abs(b)) }

    /** finds the maximum amplitude, but keeps the sign */
    val MAX_ABS_SIGNED = Operation(
        "max-abs-signed", 0f,
        "vec4(abs(a.x)>abs(b.x)?a.x:b.x," +
                "abs(a.y)>abs(b.y)?a.y:b.y," +
                "abs(a.z)>abs(b.z)?a.z:b.z," +
                "abs(a.w)>abs(b.w)?a.w:b.w)"
    ) { a, b -> if (abs(a) > abs(b)) a else b }

    /** finds the average value/color */
    val AVG = Operation("avg", 0f, "a+b", true) { a, b -> a.add(b) }

    private const val reduction = 16

    private val buffer = ByteBuffer.allocateDirect(reduction * reduction * 4 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private val shaderByType = HashMap<Operation, Shader>()

    fun reduce(texture: ITexture2D, op: Operation, dst: Vector4f = Vector4f()): Vector4f {

        GFX.checkIsGFXThread()

        val buffer = buffer
        val shader = shaderByType.getOrPut(op) {
            val v0 = "vec4(${op.startValue.x()}, ${op.startValue.y()}, ${op.startValue.z()}, ${op.startValue.w()})"
            Shader(
                "reduce-${op.name}", null, simplestVertexShader2, emptyList(), "" +
                        "uniform sampler2D src;\n" +
                        "#define reduce(a,b) ${op.function}\n" +
                        "void main(){\n" +
                        "   ivec2 uv = ivec2(gl_FragCoord.xy);\n" +
                        "   ivec2 inSize = ivec2(textureSize(src, 0));\n" +
                        "   ivec2 uv0 = uv * $reduction, uv1 = min(uv0 + $reduction, inSize);\n" +
                        "   vec4 result = $v0;\n" +
                        // strided access is more efficient on GPUs, so iterate over y
                        "   for(int x=uv0.x;x<uv1.x;x++){\n" +
                        // the reduction is split into a xy-hierarchy,
                        // so numerical issues accumulate slower (many small numbers would still be added with this approach)
                        "       vec4 resultX = $v0;\n" +
                        "       for(int y=uv0.y;y<uv1.y;y++){\n" +
                        "           vec4 value = texelFetch(src, ivec2(x,y), 0);\n" +
                        "           resultX = reduce(resultX, value);\n" +
                        "       }\n" +
                        "       result = reduce(result, resultX);\n" +
                        "   }\n" +
                        "   gl_FragColor = result;\n" +
                        "}\n"
            )
        }

        GFX.check()

        var srcTexture = texture
        while (srcTexture.w > reduction || srcTexture.h > reduction) {

            // reduce
            shader.use()

            val w = ceilDiv(srcTexture.w, reduction)
            val h = ceilDiv(srcTexture.h, reduction)

            val dstFramebuffer = FBStack["reduction", w, h, TargetType.FloatTarget4, 1, false]
            useFrame(dstFramebuffer, Renderer.copyRenderer) {
                renderPurely {
                    srcTexture.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                    flat01.draw(shader)
                }
            }

            srcTexture = dstFramebuffer.getColor0()

        }

        GFX.check()

        // read pixel
        GL11C.glFlush(); GL11C.glFinish() // wait for everything to be drawn
        packAlignment(4)
        srcTexture.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

        val target = when (srcTexture) {
            is Texture2D -> srcTexture.target
            else -> GL11C.GL_TEXTURE_2D
        }

        buffer.position(0)
        GL11C.glGetTexImage(target, 0, GL11C.GL_RGBA, GL11C.GL_FLOAT, buffer)
        GFX.check()

        val impl = op.kotlinImpl

        // performance shouldn't matter, because IO between CPU and GPU will be SLOW
        val remainingPixels = srcTexture.w * srcTexture.h
        dst.set(op.startValue)
        val tmp = JomlPools.vec4f.create()
        for (i in 0 until remainingPixels) {
            val i4 = i * 4
            tmp.set(buffer[i4], buffer[i4 + 1], buffer[i4 + 2], buffer[i4 + 3])
            impl(dst, tmp)
        }
        JomlPools.vec4f.sub(1)

        if (op.normalize) {
            dst.div((texture.w * texture.h).toFloat())
        }

        return dst
    }

    /**
     * this is a test & sample on how to compute the average color of an image
     * */
    @JvmStatic
    fun main(args: Array<String>) {
        HiddenOpenGLContext.createOpenGL()
        val fileReference = OS.pictures.getChild("4k.jpg")
        val image = ImageGPUCache.getImage(fileReference, false)!!
        LOGGER.info(reduce(image, AVG).toHexColor())
    }

}