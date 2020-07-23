package me.anno.objects

import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.effects.ToneMappers
import me.anno.studio.Studio.targetHeight
import me.anno.studio.Studio.targetWidth
import me.anno.studio.Studio.usedCamera
import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import me.anno.utils.pow
import org.joml.*
import org.lwjgl.opengl.GL11.GL_LINES
import org.lwjgl.opengl.GL20.glUniformMatrix4fv
import java.io.File
import kotlin.math.tan

class Camera(parent: Transform? = null): Transform(parent){

    // todo allow cameras to be merged
    // todo allow cameras to film camera (post processing) -> todo create a stack of cameras/scenes?

    // todo orthographic-ness by setting the camera back some amount, and narrowing the view

    var lut = File("")
    var nearZ = AnimatedProperty.floatPlus().set(0.001f)
    var farZ = AnimatedProperty.floatPlus().set(1000f)
    var fovYDegrees = AnimatedProperty.float().set(90f)
    var chromaticAberration = AnimatedProperty.float()
    var chromaticOffset = AnimatedProperty.vec2()
    var distortion = AnimatedProperty.vec3()
    var distortionOffset = AnimatedProperty.vec2()
    var orthographicness = AnimatedProperty.float01()

    var toneMapping = ToneMappers.RAW

    var onlyShowTarget = true
    var useDepth = true

    init {
        position.add(0.0, Vector3f(0f, 0f, 1f))
    }


    fun getEffectiveOffset(localTime: Double) = orthoDistance(orthographicness[localTime])
    fun getEffectiveNear(localTime: Double, offset: Float = getEffectiveOffset(localTime)) = nearZ[localTime]
    fun getEffectiveFar(localTime: Double, offset: Float = getEffectiveOffset(localTime)) = farZ[localTime] + offset
    fun getEffectiveFOV(localTime: Double, offset: Float = getEffectiveOffset(localTime)) = orthoFOV(fovYDegrees[localTime], offset)

    fun orthoDistance(ortho: Float) = pow(200f, ortho) - 1f
    fun orthoFOV(fov: Float, offset: Float) = fov / (1f + offset)


    override fun getClassName() = "Camera"

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += VI("Near Z", "Closest Visible Distance", nearZ, style)
        list += VI("Far Z", "Farthest Visible Distance", farZ, style)
        list += VI("FOV", "Field Of View, in degrees, vertical", fovYDegrees, style)
        list += VI("Perspective - Orthographic", "Sets back the camera", orthographicness, style)
        list += VI("Chromatic Aberration", "Effect occurring in cheap lenses", chromaticAberration, style)
        list += VI("Chromatic Offset", "Offset for chromatic aberration", chromaticOffset, style)
        list += VI("Distortion", "Params: R², R⁴, Scale", distortion, style)
        list += VI("Distortion Offset", "Moves the center of the distortion", distortionOffset, style)
        list += VI("Tone Mapping", "Maps large ranges of brightnesses (e.g. HDR) to monitor color space", null, toneMapping, style){ toneMapping = it }
        list += VI("Look Up Table", "LUT, Look Up Table for colors, formatted like in UE4", null, lut, style){ lut = it }
        list += VI("Only Show Target", "Forces the viewport to have the correct aspect ratio", null, onlyShowTarget, style){ onlyShowTarget = it }
        list += VI("Use Depth", "Causes Z-Fighting, but allows 3D", null, useDepth, style){ useDepth = it }
        list += ButtonPanel("Reset Transform", style)
            .setOnClickListener { x, y, button, long -> resetTransform() }
            .setTooltip("If accidentally moved")
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        if(GFX.isFinalRendering) return
        if(this === usedCamera) return
        // idk...
        // if(this !== GFX.selectedTransform) return

        val offset = getEffectiveOffset(time)
        val fov = getEffectiveFOV(time, offset)
        val near = getEffectiveNear(time, offset)
        val far = getEffectiveFar(time, offset)

        stack.translate(0f, 0f, offset)

        val scaleZ = 1f
        val scaleY = scaleZ * tan(toRadians(fov)/2f)
        val scaleX = scaleY * targetWidth / targetHeight
        stack.scale(scaleX, scaleY, scaleZ)
        val shader = GFX.lineShader3D

        // todo show the standard level only on user request, or when DOF is enabled
        // todo render the intersections instead
        shader.use()
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        GFX.shaderColor(shader, "color", color)
        cameraModel.draw(shader, GL_LINES)

        stack.scale(near)
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        cameraModel.draw(shader, GL_LINES)

        stack.scale(far/near)
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        cameraModel.draw(shader, GL_LINES)

    }

    fun resetTransform(){
        putValue(position, Vector3f(0f, 0f, 1f))
        putValue(scale, Vector3f(1f, 1f, 1f))
        putValue(skew, Vector2f(0f, 0f))
        putValue(rotationYXZ, Vector3f())
    }

    companion object {
        val cameraModel = StaticFloatBuffer(listOf(Attribute("attr0", 3)), 2 * 8)
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