package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.GFX.flat01
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.GFXx2D.transform
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.FlatShaders.flatShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.Color.a
import me.anno.utils.Color.toVecRGBA
import org.joml.Vector4f
import org.lwjgl.opengl.GL20C

object DrawRectangles {

    // to do support textures for batching with Map<Texture, Buffer>()?

    private var batching = false

    private val flatShaderBatching = BaseShader(
        "flatShader", ShaderLib.coordsList + listOf(
            Variable(GLSLType.V4F, "posSize", VariableMode.ATTR),
            Variable(GLSLType.V4F, "color", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform")
        ), "void main(){\n" +
                "   gl_Position = transform * vec4(posSize.xy + coords * posSize.zw, 0.0, 1.0);\n" +
                "   color1 = color;\n" +
                "}", listOf(Variable(GLSLType.V4F, "color1")), emptyList(), "" +
                "void main(){\n" +
                "   gl_FragColor = color1;\n" +
                "}"
    )

    // x,y,w,h (could all be u16),
    // rgba in f32
    private const val batchSize = 65536
    private var batchCount = 0
    private val color = Vector4f()
    private val buffer = StaticBuffer(
        listOf(
            Attribute("posSize", AttributeType.FLOAT, 4),
            Attribute("color", AttributeType.FLOAT, 4),
        ), batchSize, GL20C.GL_DYNAMIC_DRAW
    )

    init {
        buffer.createNioBuffer()
        buffer.nioBuffer!!.position(batchSize * buffer.stride)
        buffer.ensureBuffer() // maximum size :)
    }

    private fun drawBatching() {
        val shader = flatShaderBatching.value
        shader.use()
        shader.m4x4("transform", transform)
        buffer.ensureBufferWithoutResize()
        flat01.drawInstanced(shader, buffer)
        buffer.clear()
        batchCount = 0
    }

    private fun addRect(x: Float, y: Float, w: Float, h: Float, color: Vector4f) {
        addRect(x, y, w, h, color.x, color.y, color.z, color.w)
    }

    var sx = 0f
    var sy = 0f
    var dx = 0f
    var dy = 0f

    private fun addRect(x: Float, y: Float, w: Float, h: Float, r: Float, g: Float, b: Float, a: Float) {
        val batch = buffer.nioBuffer!!
        batch.putFloat(x * sx + dx)
        batch.putFloat(y * sy + dy)
        batch.putFloat(w * sx)
        batch.putFloat(h * sy)
        batch.putFloat(r)
        batch.putFloat(g)
        batch.putFloat(b)
        batch.putFloat(a)
        if (++batchCount >= batchSize) {
            drawBatching()
        }
    }

    fun startBatch() {
        if (batching) throw IllegalStateException("Already batching")
        batching = true
        updateBatchCoords()
    }

    fun updateBatchCoords() {
        sx = +2f / GFX.viewportWidth
        sy = -2f / GFX.viewportHeight
        dx = -1f - GFX.viewportX * sx
        dy = +1f - GFX.viewportY * sy
    }

    fun finishBatch() {
        if (batchCount > 0) {
            drawBatching()
        }
        batching = false
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
        if (batching) {
            addRect(x, y, w, h, color)
        } else {
            val shader = flatShader.value
            shader.use()
            GFXx2D.posSize(shader, x, y, w, h)
            flat01.draw(shader)
        }
    }

    fun drawRect(x: Float, y: Float, w: Float, h: Float, color: Vector4f) {
        if (batching) {
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
        if (batching) {
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