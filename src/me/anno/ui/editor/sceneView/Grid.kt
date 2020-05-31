package me.anno.ui.editor.sceneView

import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.objects.Transform.Companion.xAxis
import me.anno.objects.Transform.Companion.yAxis
import me.anno.objects.Transform.Companion.zAxis
import me.anno.objects.blending.BlendMode
import me.anno.utils.pow
import me.anno.utils.sq
import me.anno.utils.toVec3f
import org.joml.Matrix4f
import org.joml.Matrix4fStack
import org.joml.Vector4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20.*
import kotlin.math.floor
import kotlin.math.log10

object Grid {

    private val gridBuffer = StaticFloatBuffer(listOf(Attribute("attr0", 2)), 201 * 8)
    private val lineBuffer = StaticFloatBuffer(listOf(Attribute("attr0", 2)), 4)

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

    fun drawLine(stack: Matrix4fStack, color: Int, alpha: Float){

        val shader = GFX.shader3D
        shader.use()
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        shader.v2("pos", -1f, -1f)
        shader.v2("billboardSize", 1f, 1f)
        shader.v2("size", 2f, 2f)
        shader.v1("isBillboard", 0f)
        shader.v4("tint",
            color.shr(16).and(255) / 255f,
            color.shr(8).and(255) / 255f,
            color.and(255) / 255f, alpha)
        GFX.whiteTexture.bind(0, true)
        lineBuffer.draw(shader, GL_LINES)

    }

    fun blendFactor(x: Float, x0: Float) = 1f - sq(x-x0)

    fun draw(stack: Matrix4fStack, cameraTransform: Matrix4f){

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

        drawLine(stack, 0xff7777, 0.15f) // x

        stack.rotate(toRadians(90f), yAxis)

        drawLine(stack, 0x77ff77, 0.15f) // y

        stack.rotate(toRadians(90f), zAxis)

        drawLine(stack, 0x7777ff, 0.15f) // z

        BlendMode.DEFAULT.apply()
        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)

    }

    fun drawGrid(stack: Matrix4fStack, alpha: Float){

        if(alpha <= 0f) return

        val shader = GFX.shader3D
        shader.use()
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        shader.v2("pos", -1f, -1f)
        shader.v2("billboardSize", 1f, 1f)
        shader.v2("size", 2f, 2f)
        shader.v1("isBillboard", 0f)
        shader.v4("tint", 1f, 1f, 1f, alpha)
        GFX.whiteTexture.bind(0, true)
        gridBuffer.draw(shader, GL_LINES)

    }

}