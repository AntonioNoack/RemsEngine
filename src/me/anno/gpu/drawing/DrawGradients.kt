package me.anno.gpu.drawing

import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.YUVHelper.yuv2rgb
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.utils.structures.maps.LazyMap
import me.anno.video.formats.gpu.GPUFrame
import org.joml.Vector4f

/**
 * Renders a two-color linear gradient;
 * */
object DrawGradients {

    private val flatShaderGradient = LazyMap<ShaderStage?, BaseShader> { key ->
        ShaderLib.createShader(
            "flatShaderGradient", listOf(
                Variable(GLSLType.V2F, "positions", VariableMode.ATTR),
                Variable(GLSLType.V4F, "posSize"),
                Variable(GLSLType.M4x4, "transform"),
                Variable(GLSLType.V4F, "uvs"),
                Variable(GLSLType.V4F, "lColor"),
                Variable(GLSLType.V4F, "rColor"),
                Variable(GLSLType.V1B, "inXDirection"),
            ), "" +
                    yuv2rgb +
                    "void main() {\n" +
                    "   gl_Position = matMul(transform, vec4((posSize.xy + positions * posSize.zw)*2.0-1.0, 0.0, 1.0));\n" +
                    "   linColor = pow((inXDirection ? positions.x : positions.y) < 0.5 ? lColor : rColor, vec4(2.0));\n" + // srgb -> linear
                    "   finalUV = mix(uvs.xy, uvs.zw, positions);\n" +
                    "}", listOf(
                Variable(GLSLType.V2F, "finalUV"), Variable(GLSLType.V4F, "linColor")
            ), key?.variables?.filter { !it.isOutput } ?: emptyList(), "" +
                    yuv2rgb +
                    "vec4 getTexture(sampler2D tex, vec2 uv) {\n" +
                    "   return texture(tex, uv);\n" + // tiling isn't needed
                    "}\n" +
                    // used by I420Frame and in Rem's Studio, duv ~ 1/textureSize
                    "vec4 getTexture(sampler2D tex, vec2 uv, vec2 duv) {\n" +
                    "   return texture(tex, uv);\n" + // tiling isn't needed
                    "}\n" +
                    "void main(){\n" +
                    (if (key != null) {
                        "" +
                                "vec4 color;\n" + key.body +
                                "gl_FragColor = sqrt(linColor) * color;\n"
                    } else {
                        "gl_FragColor = sqrt(linColor);\n"
                    }) +
                    "}", emptyList()
        )
    }

    fun drawRectGradient(
        x: Int, y: Int, w: Int, h: Int,
        leftColor: Vector4f, rightColor: Vector4f,
        inXDirection: Boolean = true
    ) {
        if (w == 0 || h == 0) return
        val shader = flatShaderGradient[null].value
        bind(shader, x, y, w, h, inXDirection)
        shader.v4f("lColor", leftColor)
        shader.v4f("rColor", rightColor)
        flat01.draw(shader)
    }

    fun drawRectGradient(
        x: Int, y: Int, w: Int, h: Int,
        leftColor: Int, rightColor: Int,
        inXDirection: Boolean = true
    ) {
        if (w == 0 || h == 0) return
        val shader = flatShaderGradient[null].value
        bind(shader, x, y, w, h, inXDirection)
        shader.v4f("lColor", leftColor)
        shader.v4f("rColor", rightColor)
        flat01.draw(shader)
    }

    @Suppress("unused")
    fun drawRectGradient(
        x: Int, y: Int, w: Int, h: Int,
        leftColor: Vector4f, rightColor: Vector4f,
        frame: GPUFrame, uvs: Vector4f,
        inXDirection: Boolean = true
    ) {
        if (w == 0 || h == 0) return
        val shader = flatShaderGradient[frame.getShaderStage()].value
        bind(shader, x, y, w, h, uvs, inXDirection)
        frame.bind(0, Filtering.TRULY_LINEAR, Clamping.CLAMP)
        shader.v4f("lColor", leftColor)
        shader.v4f("rColor", rightColor)
        flat01.draw(shader)
    }

    @Suppress("unused")
    fun drawRectGradient(
        x: Int, y: Int, w: Int, h: Int, leftColor: Int, rightColor: Int,
        frame: GPUFrame, uvs: Vector4f,
        inXDirection: Boolean = true
    ) {
        if (w == 0 || h == 0) return
        val shader = flatShaderGradient[frame.getShaderStage()].value
        bind(shader, x, y, w, h, uvs, inXDirection)
        frame.bind(0, Filtering.TRULY_LINEAR, Clamping.CLAMP)
        shader.v4f("lColor", leftColor)
        shader.v4f("rColor", rightColor)
        flat01.draw(shader)
    }

    private fun bind(
        shader: Shader,
        x: Int, y: Int, w: Int, h: Int,
        inXDirection: Boolean
    ) {
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v1b("inXDirection", inXDirection)
    }

    private fun bind(
        shader: Shader,
        x: Int, y: Int, w: Int, h: Int,
        uvs: Vector4f,
        inXDirection: Boolean
    ) {
        bind(shader, x, y, w, h, inXDirection)
        shader.v4f("uvs", uvs)
    }
}