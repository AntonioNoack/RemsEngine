package me.anno.ui.editor.sceneView

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeLayout.Companion.bind
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.shader3DSimple
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.distance
import me.anno.maths.Maths.length
import me.anno.maths.Maths.pow
import me.anno.ui.UIColors
import me.anno.utils.types.Vectors.avg
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.sin

object Grid {

    val xAxisColor = style.getColor("grid.axis.x.color", UIColors.axisXColor)
    val yAxisColor = style.getColor("grid.axis.y.color", UIColors.axisYColor)
    val zAxisColor = style.getColor("grid.axis.z.color", UIColors.axisZColor)

    private val attr = bind(Attribute("positions", 3))
    val gridBuffer = StaticBuffer("grid", attr, 201 * 4)
    val lineBuffer = StaticBuffer("gridLines", attr, 2)
    val sphereBuffer = StaticBuffer("gridSphere", attr, 3 * 64 * 2)

    init {

        lineBuffer.put(+1f, 0f, 0f)
        lineBuffer.put(-1f, 0f, 0f)
        lineBuffer.drawMode = DrawMode.LINES

        for (i in -100..100) {
            val v = 0.01f * i
            gridBuffer.put(v, 0f, +1f)
            gridBuffer.put(v, 0f, -1f)
            gridBuffer.put(+1f, 0f, v)
            gridBuffer.put(-1f, 0f, v)
        }
        gridBuffer.drawMode = DrawMode.LINES

        val x = sphereBuffer.vertexCount / 6
        for (i in 0 until x) {
            val a0 = i * TAUf / x
            sphereBuffer.put(0f, cos(a0), sin(a0))
            val a1 = (i + 1) * TAUf / x
            sphereBuffer.put(0f, cos(a1), sin(a1))
        }
        for (i in 0 until x) {
            val a0 = i * TAUf / x
            sphereBuffer.put(cos(a0), 0f, sin(a0))
            val a1 = (i + 1) * TAUf / x
            sphereBuffer.put(cos(a1), 0f, sin(a1))
        }
        for (i in 0 until x) {
            val a0 = i * TAUf / x
            sphereBuffer.put(cos(a0), sin(a0), 0f)
            val a1 = (i + 1) * TAUf / x
            sphereBuffer.put(cos(a1), sin(a1), 0f)
        }
        sphereBuffer.drawMode = DrawMode.LINES
    }

    fun drawSmoothLine(
        x0: Float, y0: Float, x1: Float, y1: Float,
        color: Int, alpha: Float
    ) {
        val x = GFX.viewportX
        val y = GFX.viewportY
        val w = GFX.viewportWidth
        val h = GFX.viewportHeight
        drawSmoothLine(x0 - x, y0 - y, x1 - x, y1 - y, w, h, color, alpha)
    }

    @Suppress("unused")
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
            val len = 0.25f / hypot(nx, ny)
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

    @Suppress("unused")
    fun drawLineXW(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x: Int, y: Int,
        w: Int, h: Int, color: Int, alpha: Float
    ) = drawLine0W(x0 - x, y0 - y, x1 - x, y1 - y, w, h, color, alpha)

    fun drawLine0W(
        x0: Float, y0: Float, x1: Float, y1: Float,
        w: Int, h: Int, color: Int, alpha: Float
    ) = drawLine11((x0 + x1) / w - 1f, 1f - (y0 + y1) / h, x1 * 2f / w - 1f, 1f - 2f * y1 / h, color, alpha)

    private fun defaultUniforms(shader: Shader, color: Vector4f) {
        shader.v4f("tint", color)
    }

    private fun defaultUniforms(shader: Shader, color: Int, alpha: Float) {
        shader.v4f("tint", color, alpha)
    }

    private val stack = Matrix4f()
    fun drawLine11(
        x0: Float, y0: Float, x1: Float, y1: Float,
        color: Int, alpha: Float
    ) {
        val shader = shader3DSimple.value
        shader.use()
        val stack = stack
        stack.identity()
        stack.translate(x0, y0, 0f)
        val angle = atan2(y1 - y0, x1 - x0)
        stack.rotateZ(angle)
        stack.scale(distance(x0, y0, x1, y1))
        shader.m4x4("transform", stack)
        defaultUniforms(shader, color, alpha)
        lineBuffer.draw(shader)
    }

    fun drawLine(stack: Matrix4fArrayList, color: Vector4f, p0: Vector3f, p1: Vector3f) {

        // rotate, scale, and move correctly
        // (-1,0,0) / (+1,0,0) shall become p0 / p1
        stack.next {
            stack.translate(avg(p0, p1))
            stack.scale(p0.distance(p1) * 0.5f)
            val dif = (p1 - p0).normalize()
            // this rotation is correct
            stack.rotateZ(+atan2(dif.y, dif.x))
            stack.rotateY(-atan2(dif.z, hypot(dif.x, dif.y)))

            val shader = shader3DSimple.value
            shader.use()
            shader.m4x4("transform", stack)
            defaultUniforms(shader, color)
            lineBuffer.draw(shader)
        }
    }

    fun drawLine(stack: Matrix4fArrayList, color: Int, alpha: Float) {
        val shader = shader3DSimple.value
        shader.use()
        shader.m4x4("transform", stack)
        defaultUniforms(shader, color, alpha)
        lineBuffer.draw(shader)
    }

    // allow more/full grid customization?
    fun draw(stack: Matrix4fArrayList, distance: Double) {

        // todo why is line smoothing doing nothing?
        // if (LineBuffer.enableLineSmoothing) glEnable(GL_LINE_SMOOTH)
        // to avoid flickering

        val log = log10(distance)
        val f = (log - floor(log)).toFloat()
        val cameraDistance = (10.0 * pow(10.0, floor(log))).toFloat()

        stack.scale(cameraDistance)

        val gridAlpha = 0.05f

        drawGrid(stack, gridAlpha * (1f - f))

        stack.scale(10f)

        drawGrid(stack, gridAlpha)

        stack.scale(10f)

        drawGrid(stack, gridAlpha * f)

        stack.rotateX(PIf / 2f)
        drawLine(stack, xAxisColor, 0.15f) // x

        stack.rotateY(PIf / 2f)
        drawLine(stack, yAxisColor, 0.15f) // y

        stack.rotateZ(PIf / 2f)
        drawLine(stack, zAxisColor, 0.15f) // z

        // if (LineBuffer.enableLineSmoothing) glDisable(GL_LINE_SMOOTH)
    }

    // allow more/full grid customization?
    fun draw(stack: Matrix4fArrayList, cameraTransform: Matrix4f) {

        val camPos = cameraTransform.transform(Vector4f(0f, 0f, 0f, 1f))
        if (abs(camPos.w) > 1e-16f) camPos.div(camPos.w)
        else return

        val distance = length(camPos.x, camPos.y, camPos.z)
        draw(stack, distance.toDouble())
    }

    /**
     * draws the mesh as lines; used by Rem's Studio
     * */
    @Suppress("unused")
    fun drawLineMesh(pipeline: Pipeline?, stack: Matrix4fArrayList, color: Vector4f, mesh: Mesh) {
        if (color.w <= 0f) return
        val shader = shader3DSimple.value
        shader.use()
        shader.m4x4("transform", stack)
        defaultUniforms(shader, color)
        mesh.draw(pipeline, shader, 0, true)
    }

    fun drawGrid(stack: Matrix4fArrayList, alpha: Float) {
        if (alpha <= 0f) return
        val shader = shader3DSimple.value
        shader.use()
        shader.m4x4("transform", stack)
        defaultUniforms(shader, -1, alpha)
        gridBuffer.draw(shader)
    }
}