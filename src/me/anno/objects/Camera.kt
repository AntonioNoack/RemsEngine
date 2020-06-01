package me.anno.objects

import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.FloatInput
import me.anno.ui.style.Style
import org.joml.*
import org.lwjgl.opengl.GL11.GL_LINES
import org.lwjgl.opengl.GL20.glUniformMatrix4fv
import kotlin.math.tan

class Camera(parent: Transform?): Transform(parent){

    // todo allow cameras to be merged
    // todo allow cameras to film camera (post processing) -> todo create a stack of cameras/scenes?

    var nearZ = AnimatedProperty.float().set(0.001f)
    var farZ = AnimatedProperty.float().set(1000f)
    var fovYDegrees = AnimatedProperty.float().set(90f)

    var onlyShowTarget = true
    var useDepth = true

    init {
        position.add(0f, Vector3f(0f, 0f, 1f))
    }

    override fun getClassName() = "Camera"

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += FloatInput("Near Z", nearZ, lastLocalTime, style)
            .setChangeListener { putValue(nearZ, it) }
            .setIsSelectedListener { show(nearZ) }
        list += FloatInput("Far Z", farZ, lastLocalTime, style)
            .setChangeListener { putValue(farZ, it) }
            .setIsSelectedListener { show(farZ) }
        list += FloatInput("FOV", fovYDegrees, lastLocalTime, style)
            .setChangeListener { putValue(fovYDegrees, it) }
            .setIsSelectedListener { show(fovYDegrees) }
        list += BooleanInput("Only Show Target", onlyShowTarget, style)
            .setChangeListener { onlyShowTarget = it }
            .setIsSelectedListener { show(null) }
        list += BooleanInput("Use Depth", useDepth, style)
            .setChangeListener { useDepth = it }
            .setIsSelectedListener { show(null) }
    }

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f) {

        if(this === GFX.selectedCamera) return
        if(this !== GFX.selectedTransform) return

        val scaleZ = 1f
        val scaleY = scaleZ * tan(toRadians(fovYDegrees[time])/2f)
        val scaleX = scaleY * GFX.targetWidth / GFX.targetHeight
        stack.scale(scaleX, scaleY, scaleZ)
        val shader = GFX.lineShader3D

        // todo show the standard level only on user request, or when DOF is enabled
        shader.use()
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        shader.v4("color", color.x, color.y, color.z, color.w)
        cameraModel.draw(shader, GL_LINES)

        stack.scale(nearZ[time])
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        cameraModel.draw(shader, GL_LINES)

        stack.scale(farZ[time]/nearZ[time])
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        cameraModel.draw(shader, GL_LINES)

    }


    companion object {
        val cameraModel = StaticFloatBuffer(listOf(Attribute("attr0", 3)), 2 * 3 * 8)
        init {

            // points
            val zero = Vector3f()
            val p00 = Vector3f(-1f, -1f, -1f)
            val p01 = Vector3f(-1f, +1f, -1f)
            val p10 = Vector3f(+1f, -1f, -1f)
            val p11 = Vector3f(+1f, +1f, -1f)

            // lines to frame
            cameraModel.put(zero)
            cameraModel.put(p00)

            cameraModel.put(zero)
            cameraModel.put(p01)

            cameraModel.put(zero)
            cameraModel.put(p10)

            cameraModel.put(zero)
            cameraModel.put(p11)

            // frame
            cameraModel.put(p00)
            cameraModel.put(p01)

            cameraModel.put(p01)
            cameraModel.put(p11)

            cameraModel.put(p11)
            cameraModel.put(p10)

            cameraModel.put(p10)
            cameraModel.put(p00)

        }
    }

}