package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib.bindWhite
import me.anno.video.formats.gpu.GPUFrame
import org.joml.Vector4f

/**
 * Renders a two-color linear gradient;
 * */
object DrawGradients {

    val flatShaderGradient = ShaderLib.createShader(
        "flatShaderGradient", listOf(
            Variable(GLSLType.V2F, "coords", VariableMode.ATTR),
            Variable(GLSLType.V4F, "posSize"),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.V4F, "uvs"),
            Variable(GLSLType.V4F, "lColor"),
            Variable(GLSLType.V4F, "rColor"),
            Variable(GLSLType.V1B, "inXDirection"),
        ), "" +
                ShaderLib.yuv2rgb +
                "void main(){\n" +
                "   gl_Position = matMul(transform, vec4((posSize.xy + coords * posSize.zw)*2.0-1.0, 0.0, 1.0));\n" +
                "   color = (inXDirection ? coords.x : coords.y) < 0.5 ? lColor : rColor;\n" +
                "   color = color * color;\n" + // srgb -> linear
                "   uv = mix(uvs.xy, uvs.zw, coords);\n" +
                "}", listOf(
            Variable(GLSLType.V2F, "uv"), Variable(GLSLType.V4F, "color")
        ), listOf(
            Variable(GLSLType.V1I, "code"),
            Variable(GLSLType.S2D, "tex0"),
            Variable(GLSLType.S2D, "tex1"),
        ), "" +
                ShaderLib.yuv2rgb +
                "void main(){\n" +
                "   vec4 texColor;\n" +
                "   if(uv.x >= 0.0 && uv.x <= 1.0){\n" +
                "       switch(code){\n" +
                "           case 0: texColor = texture(tex0, uv).gbar;break;\n" + // ARGB
                "           case 1: texColor = texture(tex0, uv).bgra;break;\n" + // BGRA
                "           case 2: \n" +
                "               vec3 yuv = vec3(texture(tex0, uv).r, texture(tex1, uv).xy);\n" +
                "               texColor = vec4(yuv2rgb(yuv), 1.0);\n" +
                "               break;\n" + // YUV
                "           default: texColor = texture(tex0, uv);\n" + // RGBA
                "       }\n" +
                "   } else texColor = vec4(1.0);\n" +
                "   gl_FragColor = sqrt(color) * texColor;\n" +
                "}", listOf("tex0", "tex1")
    )

    fun drawRectGradient(
        x: Int, y: Int, w: Int, h: Int,
        leftColor: Vector4f, rightColor: Vector4f,
        inXDirection: Boolean = true
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShaderGradient.value
        shader.use()
        bindWhite(0)
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4f("lColor", leftColor)
        shader.v4f("rColor", rightColor)
        shader.v1i("code", -1)
        shader.v1b("inXDirection", inXDirection)
        flat01.draw(shader)
        GFX.check()
    }

    fun drawRectGradient(
        x: Int, y: Int, w: Int, h: Int,
        leftColor: Int, rightColor: Int,
        inXDirection: Boolean = true
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShaderGradient.value
        shader.use()
        bindWhite(0)
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4f("lColor", leftColor)
        shader.v4f("rColor", rightColor)
        shader.v1i("code", -1)
        shader.v1b("inXDirection", inXDirection)
        flat01.draw(shader)
        GFX.check()
    }

    @Suppress("unused")
    fun drawRectGradient(
        x: Int, y: Int, w: Int, h: Int,
        leftColor: Vector4f, rightColor: Vector4f,
        frame: GPUFrame, uvs: Vector4f,
        inXDirection: Boolean = true
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShaderGradient.value
        shader.use()
        frame.bind(0, Filtering.TRULY_LINEAR, Clamping.CLAMP)
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4f("lColor", leftColor)
        shader.v4f("rColor", rightColor)
        shader.v4f("uvs", uvs)
        shader.v1i("code", frame.code)
        shader.v1b("inXDirection", inXDirection)
        flat01.draw(shader)
        GFX.check()
    }

    @Suppress("unused")
    fun drawRectGradient(
        x: Int, y: Int, w: Int, h: Int, leftColor: Int, rightColor: Int,
        frame: GPUFrame, uvs: Vector4f,
        inXDirection: Boolean = true
    ) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = flatShaderGradient.value
        shader.use()
        frame.bind(0, Filtering.TRULY_LINEAR, Clamping.CLAMP)
        GFXx2D.posSize(shader, x, y, w, h)
        shader.v4f("lColor", leftColor)
        shader.v4f("rColor", rightColor)
        shader.v4f("uvs", uvs)
        shader.v1i("code", frame.code)
        shader.v1b("inXDirection", inXDirection)
        flat01.draw(shader)
        GFX.check()
    }
}