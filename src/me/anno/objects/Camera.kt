package me.anno.objects

import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.effects.ToneMappers
import me.anno.studio.Studio.targetHeight
import me.anno.studio.Studio.targetWidth
import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.frames.FrameSizeInput
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.EnumInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.VectorInput
import me.anno.ui.style.Style
import org.joml.*
import org.lwjgl.opengl.GL11.GL_LINES
import org.lwjgl.opengl.GL20.glUniformMatrix4fv
import java.io.File
import kotlin.math.tan

class Camera(parent: Transform?): Transform(parent){

    // todo allow cameras to be merged
    // todo allow cameras to film camera (post processing) -> todo create a stack of cameras/scenes?

    var lut = File("")
    var nearZ = AnimatedProperty.floatPlus().set(0.001f)
    var farZ = AnimatedProperty.floatPlus().set(1000f)
    var fovYDegrees = AnimatedProperty.float().set(90f)
    var chromaticAberration = AnimatedProperty.float()
    var chromaticOffset = AnimatedProperty.vec2()
    var distortion = AnimatedProperty.vec3()
    var distortionOffset = AnimatedProperty.vec2()

    var toneMapping = ToneMappers.RAW

    var onlyShowTarget = true
    var useDepth = true

    init {
        position.add(0f, Vector3f(0f, 0f, 1f))
    }

    override fun getClassName() = "Camera"

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += VI("Near Z", "Closest Visible Distance", nearZ, lastLocalTime, style)
        list += VI("Far Z", "Farthest Visible Distance", farZ, lastLocalTime, style)
        list += VI("FOV", "Field Of View, in degrees, vertical", fovYDegrees, lastLocalTime, style)
        list += VI("Chromatic Aberration", "Effect occurring in cheap lenses", chromaticAberration, lastLocalTime, style)
        list += VI("Chromatic Offset", "Offset for chromatic aberration", chromaticOffset, lastLocalTime, style)
        list += VI("Distortion", "Params: R², R⁴, Scale", distortion, lastLocalTime, style)
        list += VI("Distortion Offset", "Moves the center of the distortion", distortionOffset, lastLocalTime, style)
        list += EnumInput("Tone Mapping", true, toneMapping.displayName, ToneMappers.values().map { it.displayName }, style)
            .setChangeListener { toneMapping = getToneMapper(it) }
            .setIsSelectedListener { show(null) }
        list += FileInput("LUT", style, lut.toString())
            .setChangeListener { lut = File(it) }
            .setIsSelectedListener { show(null) }
            .setTooltip("Look Up Table for colors")
        list += BooleanInput("Only Show Target", onlyShowTarget, style)
            .setChangeListener { onlyShowTarget = it }
            .setIsSelectedListener { show(null) }
        list += BooleanInput("Use Depth", useDepth, style)
            .setChangeListener { useDepth = it }
            .setIsSelectedListener { show(null) }
            .setTooltip("Causes Z-Fighting, but allows 3D")
        list += ButtonPanel("Reset Transform", style)
            .setOnClickListener { x, y, button, long -> resetTransform() }
            .setTooltip("If accidentally moved")

        // for testing
        list += FrameSizeInput("Frame Size", "5x5", style)


    }

    fun getToneMapper(name: String) =
        ToneMappers.values().firstOrNull { it.displayName == name || it.code == name } ?:
        ToneMappers.RAW

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f) {

        if(GFX.isFinalRendering) return
        if(this === GFX.usedCamera) return
        // idk...
        // if(this !== GFX.selectedTransform) return

        val scaleZ = 1f
        val scaleY = scaleZ * tan(toRadians(fovYDegrees[time])/2f)
        val scaleX = scaleY * targetWidth / targetHeight
        stack.scale(scaleX, scaleY, scaleZ)
        val shader = GFX.lineShader3D

        // todo show the standard level only on user request, or when DOF is enabled
        // todo render the intersections instead
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