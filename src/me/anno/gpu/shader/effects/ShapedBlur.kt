package me.anno.gpu.shader.effects

import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.io.Streams.read0String
import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32F
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.hasFlag
import me.anno.io.Streams.readNBytes2
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object ShapedBlur {

    val fileName = "libHPIFvSA.bin"

    val filters by lazy {
        val map: HashMap<String, Lazy<Pair<Shader, Int>>> = HashMap()
        getReference("res://shaders/$fileName").inputStream { it, e ->
            e?.printStackTrace()
            if (it != null) map.putAll(loadFilters(it))
        }
        map
    }

    fun loadFilters(input: InputStream): Map<String, Lazy<Pair<Shader, Int>>> {
        val result = HashMap<String, Lazy<Pair<Shader, Int>>>()
        while (true) {
            val name = input.read0String()
            if (name.isEmpty()) break
            val data = input.readNBytes2(input.readLE16(), true)
            result[name] = lazy { decompress(ByteArrayInputStream(data)) }
        }
        while (true) {
            val name = input.read0String()
            if (name.isEmpty()) break
            val data = input.readNBytes2(input.readLE16(), true)
            result[name] = lazy { decompressGaussian(ByteArrayInputStream(data)) }
        }
        return result
    }

    fun applyFilter(
        src0: ITexture2D,
        shader: Shader,
        stages: Int,
        fp: TargetType,
        scale0: Float,
        gamma: Float
    ): ITexture2D {
        var src: ITexture2D = src0
        val afp = arrayOf(fp)
        val dst0 = FBStack["d0", src.width, src.height, afp, 1, DepthBufferType.NONE]
        val dst1 = FBStack["d1", src.width, src.height, afp, 1, DepthBufferType.NONE]
        shader.use()
        for (i in 0 until stages) {
            val target = if (i.hasFlag(1)) dst1 else dst0
            useFrame(target) {
                shader.v1i("uPass", i)
                shader.v2f("duv", scale0 / src.width, -scale0 / src.height)
                shader.v3f("inGamma", if (i > 0) 1f else gamma)
                shader.v3f("outGamma", if (i < stages - 1) 1f else 1f / gamma)
                src.bind(0, Filtering.TRULY_LINEAR, Clamping.CLAMP)
                flat01.draw(shader)
                src = target.getTextureI(0)
            }
        }
        return src
    }

    fun <J> decompress(
        stages: Int,
        gaussian: Boolean,
        filter: (Int) -> Pair<Int, J>,
        sample: (Int, J, Vector3f) -> Unit
    ): Pair<Shader, Int> {
        val src = StringBuilder()
        val tmp = Vector3f()
        src.append(
            "void main(){\n" +
                    "   vec3 color = vec3(0.0);\n" +
                    "   switch(uPass){\n"
        )
        for (i in 0 until stages) {
            val (length, vJ) = filter(i)
            src.append("case ").append(i).append(": color = (")
            for (j in 0 until length) {
                sample(j, vJ, tmp)
                val px = tmp.x
                val py = tmp.y
                if (gaussian) {
                    src.append("+pow(textureLod(tex,uv+duv*vec2(").append(px).append(',').append(py)
                        .append("),0).rgb,inGamma)")
                } else {
                    val weight = tmp.z
                    val sign = if (weight < 0f) '-' else '+'
                    src.append(sign).append("pow(textureLod(tex,uv+duv*vec2(").append(px).append(',').append(py)
                        .append("),0).rgb,inGamma)*").append(abs(weight))
                }
            }
            if (gaussian) src.append(")*").append(1f / length).append(";break;\n")
            else src.append(");break;\n")
        }
        src.append(
            "   }\n" +
                    "   color = max(color,vec3(0.0));\n" +
                    "   result = vec4(pow(color,outGamma),1.0);\n" +
                    "}\n"
        )
        return Shader(
            "filter", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1I, "uPass"),
                Variable(GLSLType.V2F, "duv"),
                Variable(GLSLType.V3F, "inGamma"),
                Variable(GLSLType.V3F, "outGamma"),
                Variable(GLSLType.S2D, "tex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ),
            src.toString()
        ) to stages
    }

    fun decompress(src: InputStream): Pair<Shader, Int> {
        val stages = src.read()
        val stageLengths = ByteArray(stages)
        for (i in 0 until stages) {
            stageLengths[i] = src.read().toByte()
        }
        return decompress(stages, false, { i ->
            val dx = src.readLE32F()
            val sx = 1f / src.readLE32F()
            val dy = src.readLE32F()
            val sy = 1f / src.readLE32F()
            val dz = src.readLE32F()
            val sz = 1f / src.readLE32F()
            stageLengths[i].toInt() to floatArrayOf(sx, dx, sy, dy, sz, dz)
        }, { _, d, dst ->
            val px = src.read() * d[0] + d[1]
            val py = src.read() * d[2] + d[3]
            val weight = src.read() * d[4] + d[5]
            dst.set(px, py, weight)
        })
    }

    fun decompressGaussian(src: InputStream): Pair<Shader, Int> {
        val stages = src.read()
        return decompress(stages, true, {
            val da = src.readLE32F()
            val r = src.readLE32F()
            val length = src.read()
            val weight = 1f / length
            length to Vector4f(da, TAUf / length, r, weight)
        }, { j, d, dst ->
            val a = d.x + j * d.y
            val px = cos(a) * d.z
            val py = sin(a) * d.z
            dst.set(px, py, d.w)
        })
    }
}