package me.anno.objects.models

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.studio.rems.RemsStudio
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4fc
import kotlin.math.tan

object CameraModel {

    fun drawCamera(
        stack: Matrix4fArrayList,
        offset: Float,
        color: Vector4fc,
        fov: Float, near: Float, far: Float
    ) {

        stack.translate(0f, 0f, offset)

        val scaleZ = 1f
        val scaleY = scaleZ * tan(GFX.toRadians(fov) / 2f)
        val scaleX = scaleY * RemsStudio.targetWidth / RemsStudio.targetHeight
        stack.scale(scaleX, scaleY, scaleZ)
        val shader = ShaderLib.lineShader3D

        // todo show the standard level only on user request, or when DOF is enabled
        // todo render the intersections instead
        shader.use()
        shader.m4x4("transform", stack)
        GFX.shaderColor(shader, "color", color)
        cameraModel.draw(shader)

        stack.scale(near)
        shader.m4x4("transform", stack)
        cameraModel.draw(shader)

        stack.scale(far / near)
        shader.m4x4("transform", stack)
        cameraModel.draw(shader)

    }

    fun destroy(){
        cameraModel.destroy()
    }

    private val cameraModel: StaticBuffer = StaticBuffer(
        listOf(
            Attribute("attr0", 3)
        ), 2 * 8
    ).apply {

        // points
        val zero = Vector3f()
        val p00 = Vector3f(-1f, -1f, -1f)
        val p01 = Vector3f(-1f, +1f, -1f)
        val p10 = Vector3f(+1f, -1f, -1f)
        val p11 = Vector3f(+1f, +1f, -1f)

        // lines to frame
        put(zero)
        put(p00)

        put(zero)
        put(p01)

        put(zero)
        put(p10)

        put(zero)
        put(p11)

        // frame
        put(p00)
        put(p01)

        put(p01)
        put(p11)

        put(p11)
        put(p10)

        put(p10)
        put(p00)

        lines()
    }


}