package me.anno.ui.editor.sceneView

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.gpu.ShaderLib.shader3D
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.gpu.texture.ClampMode
import me.anno.objects.Transform.Companion.xAxis
import me.anno.objects.Transform.Companion.yAxis
import me.anno.objects.Transform.Companion.zAxis
import me.anno.objects.blending.BlendMode
import me.anno.image.svg.SVGStyle.Companion.parseColor
import me.anno.utils.distance
import me.anno.utils.pow
import me.anno.utils.toVec3f
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import org.lwjgl.opengl.GL20.*
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.log10

object Grid {

    private val xAxisColor = parseColor(DefaultConfig["grid.axis.x.color", "#ff7777"]) ?: 0xff7777
    private val yAxisColor = parseColor(DefaultConfig["grid.axis.y.color", "#77ff77"]) ?: 0x77ff77
    private val zAxisColor = parseColor(DefaultConfig["grid.axis.z.color", "#7777ff"]) ?: 0x7777ff

    private val gridBuffer = StaticFloatBuffer(listOf(Attribute("attr0", 3), Attribute("attr1", 2)), 201 * 4)
    private val lineBuffer = StaticFloatBuffer(listOf(Attribute("attr0", 3), Attribute("attr1", 2)), 2)

    init {

        lineBuffer.put(1f, 0f, 0f, 0f, 0f)
        lineBuffer.put(-1f, 0f, 0f, 0f, 0f)

        for(i in -100 .. 100){
            val v = 0.01f * i
            gridBuffer.put(v, 1f, 0f, 0f, 0f)
            gridBuffer.put(v, -1f, 0f, 0f, 0f)
            gridBuffer.put(1f, v, 0f, 0f, 0f)
            gridBuffer.put(-1f, v, 0f, 0f, 0f)
        }

    }

    fun drawLine01(x0: Float, y0: Float, x1: Float, y1: Float,
                   w: Int, h: Int, color: Int, alpha: Float){

        // drawLine2((x0+x1)/w-1, 1-(y0+y1)/h, x1*2/w-1, 1-2*y1/h, color, alpha)
        drawLine2((x0+x1)/w-1, 1-(y0+y1)/h, x1*2/w-1, 1-2*y1/h, color, alpha)

    }

    fun drawLine2(x0: Float, y0: Float, x1: Float, y1: Float,
                  color: Int, alpha: Float){
        val shader = shader3D.shader
        shader.use()
        val stack = Matrix4f()
        stack.translate(x0, y0, 0f)
        val angle = atan2(y1-y0, x1-x0)
        stack.rotate(angle, zAxis)
        stack.scale(distance(x0, y0, x1, y1))
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        shader.v4("tint",
            color.shr(16).and(255) / 255f,
            color.shr(8).and(255) / 255f,
            color.and(255) / 255f, alpha)
        whiteTexture.bind(0, true, ClampMode.CLAMP)
        lineBuffer.draw(shader, GL_LINES)
    }

    fun drawLine(stack: Matrix4fArrayList, color: Int, alpha: Float){

        val shader = shader3D.shader
        shader.use()
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        shader.v4("tint",
            color.shr(16).and(255) / 255f,
            color.shr(8).and(255) / 255f,
            color.and(255) / 255f, alpha)
        whiteTexture.bind(0, true, ClampMode.CLAMP)
        lineBuffer.draw(shader, GL_LINES)

    }

    // allow more/full grid customization?
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

        val shader = shader3D.shader
        shader.use()
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        shader.v4("tint", 1f, 1f, 1f, alpha)
        whiteTexture.bind(0, true, ClampMode.CLAMP)
        gridBuffer.draw(shader, GL_LINES)

    }

}