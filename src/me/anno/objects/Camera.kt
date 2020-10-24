package me.anno.objects

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.gpu.ShaderLib.lineShader3D
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.effects.ToneMappers
import me.anno.studio.RemsStudio.nullCamera
import me.anno.studio.RemsStudio.onSmallChange
import me.anno.studio.RemsStudio.targetHeight
import me.anno.studio.RemsStudio.targetWidth
import me.anno.studio.RemsStudio.usedCamera
import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.pow
import org.joml.*
import org.lwjgl.opengl.GL20.glUniformMatrix4fv
import java.io.File
import kotlin.math.tan

// todo color palette
// todo save the color palette in the project settings
// todo 8x5 colors?
// todo drag color into there / paste it / auto-line(s)
// todo color picker

class Camera(parent: Transform? = null): Transform(parent){

    // todo allow cameras to be merged
    // todo allow cameras to film camera (post processing) -> todo create a stack of cameras/scenes?

    // orthographic-ness by setting the camera back some amount, and narrowing the view

    var lut = File("")
    val nearZ = AnimatedProperty.floatPlus(0.001f)
    val farZ = AnimatedProperty.floatPlus(1000f)
    val fovYDegrees = AnimatedProperty.float(90f)
    val chromaticAberration = AnimatedProperty.floatPlus()
    val chromaticOffset = AnimatedProperty.vec2()
    val chromaticAngle = AnimatedProperty.float()
    val distortion = AnimatedProperty.vec3()
    val distortionOffset = AnimatedProperty.vec2()
    val orthographicness = AnimatedProperty.float01()
    val vignetteStrength = AnimatedProperty.floatPlus()
    val vignetteColor = AnimatedProperty.color3(Vector3f(0f, 0f, 0f))

    val cgOffset = AnimatedProperty.vec3()
    val cgSlope = AnimatedProperty.color(Vector4f(1f, 1f, 1f, 1f))
    val cgPower = AnimatedProperty.color(Vector4f(1f, 1f, 1f, 1f))
    val cgSaturation = AnimatedProperty.float(1f) // only allow +? only 01?

    var toneMapping = ToneMappers.RAW8

    var onlyShowTarget = true
    var useDepth = true

    init {
        position.addKeyframe(0.0, Vector3f(0f, 0f, 1f))
    }

    fun getEffectiveOffset(localTime: Double) = orthoDistance(orthographicness[localTime])
    fun getEffectiveNear(localTime: Double, offset: Float = getEffectiveOffset(localTime)) = nearZ[localTime]
    fun getEffectiveFar(localTime: Double, offset: Float = getEffectiveOffset(localTime)) = farZ[localTime] + offset
    fun getEffectiveFOV(localTime: Double, offset: Float = getEffectiveOffset(localTime)) = orthoFOV(fovYDegrees[localTime], offset)

    fun orthoDistance(ortho: Float) = pow(200f, ortho) - 1f
    fun orthoFOV(fov: Float, offset: Float) = fov / (1f + offset)

    override fun getSymbol() = DefaultConfig["ui.symbol.camera", "\uD83C\uDFA5"]
    override fun getClassName() = "Camera"

    override fun createInspector(list: PanelListY, style: Style, getGroup: (title: String, id: String) -> SettingCategory) {
        super.createInspector(list, style, getGroup)

        val cam = getGroup("Projection", "projection")
        cam += VI("FOV", "Field Of View, in degrees, vertical", fovYDegrees, style)
        cam += VI("Perspective - Orthographic", "Sets back the camera", orthographicness, style)
        val depth = getGroup("Depth", "depth")
        depth += VI("Near Z", "Closest Visible Distance", nearZ, style)
        depth += VI("Far Z", "Farthest Visible Distance", farZ, style)
        depth += VI("Use Depth", "Causes Z-Fighting, but allows 3D", null, useDepth, style){ useDepth = it }
        val chroma = getGroup("Chromatic Aberration", "chroma")
        chroma += VI("Strength", "Effect occurring in cheap lenses", chromaticAberration, style)
        chroma += VI("Offset", "Offset", chromaticOffset, style)
        chroma += VI("Rotation", "Rotation/angle", chromaticAngle, style)
        val dist = getGroup("Distortion", "distortion")
        dist += VI("Distortion", "Params: R², R⁴, Scale", distortion, style)
        dist += VI("Distortion Offset", "Moves the center of the distortion", distortionOffset, style)
        val vignette = getGroup("Vignette", "vignette")
        vignette += VI("Vignette Color", "Color of the border", vignetteColor, style)
        vignette += VI("Vignette Strength", "Strength of the colored border", vignetteStrength, style)
        val color = getGroup("Color", "color")
        color += VI("Tone Mapping", "Maps large ranges of brightnesses (e.g. HDR) to monitor color space", null, toneMapping, style){ toneMapping = it }
        color += VI("Look Up Table", "LUT, Look Up Table for colors, formatted like in UE4", null, lut, style){ lut = it }
        val editor = getGroup("Editor", "editor")
        editor += VI("Only Show Target", "Forces the viewport to have the correct aspect ratio", null, onlyShowTarget, style){ onlyShowTarget = it }
        val ops = getGroup("Operations", "operations")
        ops += ButtonPanel("Reset Transform", style)
            .setSimpleClickListener { resetTransform() }
            .setTooltip("If accidentally moved")
        color += VI("Power", "Color Grading, ASC CDL", cgPower, style)
        color += VI("Saturation", "Color Grading, 0 = gray scale, 1 = normal, -1 = inverted colors", cgSaturation, style)
        color += VI("Slope", "Color Grading, Intensity", cgSlope, style)
        color += VI("Offset", "Color Grading, ASC CDL", cgOffset, style)
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
        val shader = lineShader3D

        // todo show the standard level only on user request, or when DOF is enabled
        // todo render the intersections instead
        shader.use()
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        GFX.shaderColor(shader, "color", color)
        cameraModel.draw(shader)

        stack.scale(near)
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        cameraModel.draw(shader)

        stack.scale(far/near)
        stack.get(GFX.matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, GFX.matrixBuffer)
        cameraModel.draw(shader)

    }

    fun resetTransform(){
        putValue(position, Vector3f(0f, 0f, 1f))
        putValue(scale, Vector3f(1f, 1f, 1f))
        putValue(skew, Vector2f(0f, 0f))
        putValue(rotationYXZ, Vector3f())
        if(this != nullCamera) onSmallChange("camera-reset")
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "nearZ", nearZ)
        writer.writeObject(this, "farZ", farZ)
        writer.writeObject(this, "fovY", fovYDegrees)
        writer.writeObject(this, "chromaticAberration", chromaticAberration)
        writer.writeObject(this, "chromaticOffset", chromaticOffset)
        writer.writeObject(this, "distortion", distortion)
        writer.writeObject(this, "distortionOffset", distortionOffset)
        writer.writeObject(this, "orthographicness", orthographicness)
        writer.writeObject(this, "vignetteStrength", vignetteStrength)
        writer.writeObject(this, "vignetteColor", vignetteColor)
        writer.writeInt("toneMapping", toneMapping.id, true)
        writer.writeBool("onlyShowTarget", onlyShowTarget)
        writer.writeBool("useDepth", useDepth)
        writer.writeFile("lut", lut)
        writer.writeObject(this, "cgSaturation", cgSaturation)
        writer.writeObject(this, "cgOffset", cgOffset)
        writer.writeObject(this, "cgSlope", cgSlope)
        writer.writeObject(this, "cgPower", cgPower)
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "onlyShowTarget" -> onlyShowTarget = value
            "useDepth" -> useDepth = value
            else -> super.readBool(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "nearZ" -> nearZ.copyFrom(value)
            "farZ" -> farZ.copyFrom(value)
            "fovY" -> fovYDegrees.copyFrom(value)
            "chromaticAberration" -> chromaticAberration.copyFrom(value)
            "chromaticOffset" -> chromaticOffset.copyFrom(value)
            "distortion" -> distortion.copyFrom(value)
            "distortionOffset" -> distortionOffset.copyFrom(value)
            "orthographicness" -> orthographicness.copyFrom(value)
            "vignetteStrength" -> vignetteStrength.copyFrom(value)
            "vignetteColor" -> vignetteColor.copyFrom(value)
            "cgSaturation" -> cgSaturation.copyFrom(value)
            "cgOffset" -> cgOffset.copyFrom(value)
            "cgSlope" -> cgSlope.copyFrom(value)
            "cgPower" -> cgPower.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when(name){
            "lut" -> lut = File(value)
            else -> super.readString(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when(name){
            "toneMapping" -> toneMapping = ToneMappers.values().firstOrNull { it.id == value } ?: toneMapping
            else -> super.readInt(name, value)
        }
    }


    companion object {
        val cameraModel = StaticBuffer(listOf(Attribute("attr0", 3)), 2 * 8)
        val DEFAULT_VIGNETTE_STRENGTH = 5f
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

            cameraModel.lines()

        }
    }

}