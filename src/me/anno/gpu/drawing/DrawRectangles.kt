package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.GFX.flat01
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.drawing.GFXx2D.transform
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.FlatShaders.flatShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.Color.a
import me.anno.utils.Color.toVecRGBA
import org.joml.Vector4f

object DrawRectangles {

    // to do support textures for batching with Map<Texture, Buffer>()?

    val batch = object : Batch(
        "rectBatch", flat01, listOf(
            Attribute("instancePosSize", AttributeType.FLOAT, 4),
            Attribute("instanceColor", AttributeType.FLOAT, 4),
        )
    ) {
        private val flatShaderBatching = BaseShader(
            "rectShader", ShaderLib.coordsList + listOf(
                Variable(GLSLType.V4F, attributes[0].name, VariableMode.ATTR),
                Variable(GLSLType.V4F, attributes[1].name, VariableMode.ATTR),
                Variable(GLSLType.M4x4, "transform")
            ), "" +
                    "void main(){\n" +
                    "   vec4 posSize = instancePosSize;\n" +
                    "   gl_Position = matMul(transform, vec4(posSize.xy + coords * posSize.zw, 0.0, 1.0));\n" +
                    "   color1 = instanceColor;\n" +
                    "}", listOf(Variable(GLSLType.V4F, "color1")), emptyList(), "" +
                    "void main(){\n" +
                    "   gl_FragColor = color1;\n" +
                    "}"
        )

        override fun bindShader(): Shader {
            val shader = flatShaderBatching.value
            shader.use()
            shader.m4x4("transform", transform)
            return shader
        }
    }

    // x,y,w,h (could all be u16),
    // rgba in f32
    private val color = Vector4f()

    private fun addRect(x: Float, y: Float, w: Float, h: Float, color: Vector4f) {
        addRect(x, y, w, h, color.x, color.y, color.z, color.w)
    }

    var sx = 0f
    var sy = 0f
    var dx = 0f
    var dy = 0f

    private fun addRect(x: Float, y: Float, w: Float, h: Float, r: Float, g: Float, b: Float, a: Float) {
        val data = batch.data
        data.putFloat(x * sx + dx)
        data.putFloat(y * sy + dy)
        data.putFloat(w * sx)
        data.putFloat(h * sy)
        data.putFloat(r)
        data.putFloat(g)
        data.putFloat(b)
        data.putFloat(a)
        batch.next()
    }

    fun startBatch() {
        batch.start()
        updateBatchCoords()
    }

    fun updateBatchCoords() {
        sx = +2f / GFX.viewportWidth
        sy = -2f / GFX.viewportHeight
        dx = -1f - GFX.viewportX * sx
        dy = +1f - GFX.viewportY * sy
    }

    fun finishBatch() {
        batch.finish()
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Vector4f) {
        drawRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color)
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        drawRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color)
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int) {
        drawRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
    }

    fun drawRect(x: Float, y: Float, w: Float, h: Float) {
        if (w == 0f || h == 0f) return
        if (batch.active) {
            addRect(x, y, w, h, color)
        } else {
            val shader = flatShader.value
            shader.use()
            GFXx2D.posSize(shader, x, y, w, h)
            flat01.draw(shader)
        }
    }

    fun drawRect(x: Float, y: Float, w: Float, h: Float, color: Vector4f) {
        if (batch.active) {
            this.color.set(color)
            addRect(x, y, w, h, color)
        } else {
            GFX.check()
            val shader = flatShader.value
            shader.use()
            GFXx2D.posSize(shader, x, y, w, h)
            shader.v4f("color", color)
            flat01.draw(shader)
            GFX.check()
        }
    }

    fun drawRect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        if (batch.active) {
            color.toVecRGBA(this.color)
            addRect(x, y, w, h, this.color)
        } else {
            GFX.check()
            val shader = flatShader.value
            shader.use()
            GFXx2D.posSize(shader, x, y, w, h)
            shader.v4f("color", color)
            flat01.draw(shader)
            GFX.check()
        }
    }

    fun drawBorder(x: Int, y: Int, w: Int, h: Int, color: Int, thicknessX: Int = 1, thicknessY: Int = thicknessX) {
        if (color.a() == 0) return
        drawRect(x, y, w, thicknessY, color)
        drawRect(x, y + h - thicknessY, w, thicknessY, color)
        drawRect(x, y, thicknessX, h, color)
        drawRect(x + w - thicknessX, y, thicknessX, h, color)
    }

    fun drawBorder(x: Int, y: Int, w: Int, h: Int, color: Int, size: Int) {
        GFXx2D.flatColor(color)
        drawRect(x, y, w, size)
        drawRect(x, y + h - size, w, size)
        drawRect(x, y + size, size, h - 2 * size)
        drawRect(x + w - size, y + size, size, h - 2 * size)
    }

}