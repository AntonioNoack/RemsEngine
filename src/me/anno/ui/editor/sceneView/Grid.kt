package me.anno.ui.editor.sceneView

import me.anno.config.DefaultConfig.style
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.gpu.ShaderLib.shader3D
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.Shader
import me.anno.objects.GFXTransform
import me.anno.objects.Transform.Companion.xAxis
import me.anno.objects.Transform.Companion.yAxis
import me.anno.objects.Transform.Companion.zAxis
import me.anno.utils.Maths.distance
import me.anno.utils.Maths.pow
import me.anno.utils.Maths.sq
import me.anno.utils.types.Vectors.avg
import me.anno.utils.types.Vectors.minus
import me.anno.utils.types.Vectors.toVec3f
import org.joml.*
import org.lwjgl.opengl.GL20.GL_LINES
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.sqrt


// todo grid add mode is broken
// todo color input window is broken
// todo the elk guy is broken somehow

object Grid {

    private val xAxisColor = style.getColor("grid.axis.x.color", 0xff7777 or black)
    private val yAxisColor = style.getColor("grid.axis.y.color", 0x77ff77 or black)
    private val zAxisColor = style.getColor("grid.axis.z.color", 0x7777ff or black)

    private val gridBuffer = StaticBuffer(listOf(Attribute("attr0", 3), Attribute("attr1", 2)), 201 * 4)
    private val lineBuffer = StaticBuffer(listOf(Attribute("attr0", 3), Attribute("attr1", 2)), 2)

    init {

        lineBuffer.put(1f, 0f, 0f, 0f, 0f)
        lineBuffer.put(-1f, 0f, 0f, 0f, 0f)

        for (i in -100..100) {
            val v = 0.01f * i
            gridBuffer.put(v, 1f, 0f, 0f, 0f)
            gridBuffer.put(v, -1f, 0f, 0f, 0f)
            gridBuffer.put(1f, v, 0f, 0f, 0f)
            gridBuffer.put(-1f, v, 0f, 0f, 0f)
        }

    }

    fun drawSmoothLine(
        x0: Float, y0: Float, x1: Float, y1: Float,
        x: Int, y: Int, w: Int, h: Int, color: Int, alpha: Float
    ) {
        drawSmoothLine(x0 - x, y0 - y, x1 - x, y1 - y, w, h, color, alpha)
    }

    fun drawSmoothLine(
        x0: Float, y0: Float, x1: Float, y1: Float,
        w: Int, h: Int, color: Int, alpha: Float
    ) {
        if (y0 == y1) {
            drawLine0W(x0, y0, x1, y1, w, h, color, alpha)
        } else {
            val actualAlpha = alpha * 0.2f
            val nx = (y1 - y0)
            val ny = -(x1 - x0)
            val len = 0.25f / sqrt(nx * nx + ny * ny)
            for (di in -2..2) {
                val dx = nx * len * di
                val dy = ny * len * di
                drawLine0W(
                    x0 + dx, y0 + dy,
                    x1 + dx, y1 + dy,
                    w, h, color, actualAlpha
                )
            }
        }
    }

    fun drawLineXW(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x: Int, y: Int,
        w: Int, h: Int, color: Int, alpha: Float
    ) {

        drawLine0W(x0 - x, y0 - y, x1 - x, y1 - y, w, h, color, alpha)

    }

    fun drawLine0W(
        x0: Float, y0: Float, x1: Float, y1: Float,
        w: Int, h: Int, color: Int, alpha: Float
    ) {

        drawLine11((x0 + x1) / w - 1f, 1f - (y0 + y1) / h, x1 * 2f / w - 1f, 1f - 2f * y1 / h, color, alpha)

    }

    private fun defaultUniforms(shader: Shader, color: Vector4fc) {
        shader.v4("tint", color)
        shader.v1("drawMode", GFX.drawMode.id)
    }

    private fun defaultUniforms(shader: Shader, color: Int, alpha: Float) {
        shader.v4("tint", color, alpha)
        shader.v1("drawMode", GFX.drawMode.id)
    }

    private val stack = Matrix4f()
    fun drawLine11(
        x0: Float, y0: Float, x1: Float, y1: Float,
        color: Int, alpha: Float
    ) {
        val shader = shader3D.value
        shader.use()
        GFXTransform.uploadAttractors0(shader)
        val stack = stack
        stack.identity()
        stack.translate(x0, y0, 0f)
        val angle = atan2(y1 - y0, x1 - x0)
        stack.rotate(angle, zAxis)
        stack.scale(distance(x0, y0, x1, y1))
        shader.m4x4("transform", stack)
        defaultUniforms(shader, color, alpha)
        bindWhite(0)
        lineBuffer.draw(shader, GL_LINES)
    }

    fun drawLine(stack: Matrix4fArrayList, color: Vector4fc, p0: Vector3fc, p1: Vector3f) {

        // rotate, scale, and move correctly
        // (-1,0,0) / (+1,0,0) shall become p0 / p1
        stack.next {
            stack.translate(avg(p0, p1))
            stack.scale(p0.distance(p1) * 0.5f)
            val dif = (p1 - p0).normalize()
            // this rotation is correct
            stack.rotateZ(+atan2(dif.y, dif.x))
            stack.rotateY(-atan2(dif.z, sqrt(sq(dif.x, dif.y))))

            val shader = shader3D.value
            shader.use()
            GFXTransform.uploadAttractors0(shader)
            shader.m4x4("transform", stack)
            defaultUniforms(shader, color)
            bindWhite(0)
            lineBuffer.draw(shader, GL_LINES)
        }

    }

    private fun drawLine(stack: Matrix4fArrayList, color: Int, alpha: Float) {

        val shader = shader3D.value
        shader.use()
        GFXTransform.uploadAttractors0(shader)
        shader.m4x4("transform", stack)
        defaultUniforms(shader, color, alpha)
        bindWhite(0)
        lineBuffer.draw(shader, GL_LINES)

    }

    // allow more/full grid customization?
    fun draw(stack: Matrix4fArrayList, cameraTransform: Matrix4f) {

        val distance = cameraTransform.transformProject(Vector4f(0f, 0f, 0f, 1f)).toVec3f().length()
        val log = log10(distance)
        val f = log - floor(log)
        val cameraDistance = 10f * pow(10f, floor(log))

        stack.scale(cameraDistance)

        stack.rotate(toRadians(90f), xAxis)

        val gridAlpha = 0.05f

        drawGrid(stack, gridAlpha * (1f - f))

        stack.scale(10f)

        drawGrid(stack, gridAlpha)

        stack.scale(10f)

        drawGrid(stack, gridAlpha * f)

        drawLine(stack, xAxisColor, 0.15f) // x

        stack.rotate(toRadians(90f), yAxis)
        drawLine(stack, yAxisColor, 0.15f) // y

        stack.rotate(toRadians(90f), zAxis)
        drawLine(stack, zAxisColor, 0.15f) // z

    }

    fun drawBuffer(stack: Matrix4fArrayList, color: Vector4fc, buffer: StaticBuffer) {

        if (color.w() <= 0f) return

        val shader = shader3D.value
        shader.use()
        GFXTransform.uploadAttractors0(shader)
        shader.m4x4("transform", stack)
        defaultUniforms(shader, color)
        bindWhite(0)
        buffer.draw(shader, GL_LINES)

    }

    fun drawGrid(stack: Matrix4fArrayList, alpha: Float) {

        if (alpha <= 0f) return

        val shader = shader3D.value
        shader.use()
        GFXTransform.uploadAttractors0(shader)
        shader.m4x4("transform", stack)
        defaultUniforms(shader, -1, alpha)
        bindWhite(0)
        gridBuffer.draw(shader, GL_LINES)

    }

    fun bindWhite(index: Int) {
        whiteTexture.bind(index, whiteTexture.filtering, whiteTexture.clamping)
    }

}