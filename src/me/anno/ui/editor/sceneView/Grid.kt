package me.anno.ui.editor.sceneView

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.objects.Transform.Companion.xAxis
import me.anno.objects.Transform.Companion.yAxis
import me.anno.objects.Transform.Companion.zAxis
import me.anno.objects.blending.BlendMode
import me.anno.objects.meshes.svg.SVGStyle.Companion.parseColor
import me.anno.utils.distance
import me.anno.utils.length
import me.anno.utils.pow
import me.anno.utils.toVec3f
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import org.lwjgl.opengl.GL20.*
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.log10

// todo gizmo element with arrows in the axis directions :)

object Grid {

    private val xAxisColor = parseColor(DefaultConfig["grid.axis.x.color", "#ff7777"]) ?: 0xff7777
    private val yAxisColor = parseColor(DefaultConfig["grid.axis.y.color", "#77ff77"]) ?: 0x77ff77
    private val zAxisColor = parseColor(DefaultConfig["grid.axis.z.color", "#7777ff"]) ?: 0x7777ff

    private val gridBuffer = StaticFloatBuffer(listOf(Attribute("attr0", 2)), 201 * 4)
    private val lineBuffer = StaticFloatBuffer(listOf(Attribute("attr0", 2)), 2)

    init {

        lineBuffer.put(1f, 0.5f)
        lineBuffer.put(0f, 0.5f)

        for(i in -100 .. 100){
            val v = 0.5f + 0.005f * i
            gridBuffer.put(v, 1f)
            gridBuffer.put(v, 0f)
            gridBuffer.put(1f, v)
            gridBuffer.put(0f, v)
        }

    }

    fun drawLine(x0: Float, y0: Float, x1: Float, y1: Float,
                 color: Int, alpha: Float){

        drawLine2((x0+x1)/2, (y0+y1)/2, x1, y1, color, alpha)

    }

    fun drawLine2(x0: Float, y0: Float, x1: Float, y1: Float,
                  color: Int, alpha: Float){
        val shader = GFX.shader3D.shader
        shader.use()
        val stack = Matrix4f()
        stack.translate(x0, y0, 0f)
        val angle = atan2(y1-y0, x1-x0)
        stack.rotate(angle, zAxis)
        stack.scale(distance(x0, y0, x1, y1))
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        shader.v2("billboardSize", 1f, 1f)
        shader.v1("isBillboard", 0f)
        shader.v4("tint",
            color.shr(16).and(255) / 255f,
            color.shr(8).and(255) / 255f,
            color.and(255) / 255f, alpha)
        GFX.whiteTexture.bind(0, true)
        lineBuffer.draw(shader, GL_LINES)
    }

    fun drawLine(stack: Matrix4fArrayList, color: Int, alpha: Float){

        val shader = GFX.shader3D.shader
        shader.use()
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        shader.v2("billboardSize", 1f, 1f)
        shader.v1("isBillboard", 0f)
        shader.v4("tint",
            color.shr(16).and(255) / 255f,
            color.shr(8).and(255) / 255f,
            color.and(255) / 255f, alpha)
        GFX.whiteTexture.bind(0, true)
        lineBuffer.draw(shader, GL_LINES)

    }

    fun draw(stack: Matrix4fArrayList, cameraTransform: Matrix4f){

        if(GFX.isFinalRendering) return

        BlendMode.ADD.apply()
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)

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

    fun drawGrid(stack: Matrix4fArrayList, alpha: Float){

        if(alpha <= 0f) return

        val shader = GFX.shader3D.shader
        shader.use()
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        shader.v2("billboardSize", 1f, 1f)
        shader.v1("isBillboard", 0f)
        shader.v4("tint", 1f, 1f, 1f, alpha)
        GFX.whiteTexture.bind(0, true)
        gridBuffer.draw(shader, GL_LINES)

    }

}