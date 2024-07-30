package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.FlatShaders
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import org.joml.Vector4f

object DrawStriped {

    val flatShaderStriped = Shader(
        "flatShaderStriped", FlatShaders.coordsPosSize, FlatShaders.coordsPosSizeVShader,
        emptyList(), listOf(
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V1I, "offset"),
            Variable(GLSLType.V1I, "stride")
        ), "" +
                "void main(){\n" +
                "   int x = int(gl_FragCoord.x);\n" +
                "   if(x % stride != offset) discard;\n" +
                "   gl_FragColor = color;\n" +
                "}"
    )

    @Suppress("unused")
    fun drawRectStriped(x: Int, y: Int, w: Int, h: Int, offset: Int, stride: Int, color: Vector4f) {
        if (w == 0 || h == 0) return
        val shader = flatShaderStriped
        shader.use()
        shader.v4f("color", color)
        drawRectStriped(x, y, w, h, offset, stride)
    }

    fun drawRectStriped(x: Int, y: Int, w: Int, h: Int, offset: Int, stride: Int, color: Int) {
        if (w == 0 || h == 0) return
        val shader = flatShaderStriped
        shader.use()
        shader.v4f("color", color)
        drawRectStriped(x, y, w, h, offset, stride)
    }

    private fun drawRectStriped(x: Int, y: Int, w: Int, h: Int, offset: Int, stride: Int) {
        val shader = flatShaderStriped
        GFX.check()
        GFXx2D.posSize(shader, x, y, w, h)
        var o = offset % stride
        if (o < 0) o += stride
        shader.v1i("offset", o)
        shader.v1i("stride", stride)
        SimpleBuffer.flat01.draw(shader)
        GFX.check()
    }
}