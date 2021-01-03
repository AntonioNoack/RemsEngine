package me.anno.objects

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.effects.ToneMappers
import me.anno.objects.models.CameraModel.drawCamera
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.currentlyDrawnCamera
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.Maths.pow
import org.joml.Matrix4fArrayList
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.File

// todo color palette
// todo save the color palette in the project settings
// todo 8x5 colors?
// todo drag color into there / paste it / auto-line(s)
// todo color picker

class Camera(parent: Transform? = null) : Transform(parent) {

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

    val bloomSize = AnimatedProperty.floatPlus(0.05f)
    val bloomIntensity = AnimatedProperty.floatPlus(0f)
    val bloomThreshold = AnimatedProperty.floatPlus(0.8f)

    var toneMapping = ToneMappers.RAW8

    var onlyShowTarget = true
    var useDepth = true

    init {
        position.defaultValue = Vector3f(0f, 0f, 1f)
    }

    fun getEffectiveOffset(localTime: Double) = orthoDistance(orthographicness[localTime])
    fun getEffectiveNear(localTime: Double, offset: Float = getEffectiveOffset(localTime)) = nearZ[localTime] + offset
    fun getEffectiveFar(localTime: Double, offset: Float = getEffectiveOffset(localTime)) = farZ[localTime] + offset
    fun getEffectiveFOV(localTime: Double, offset: Float = getEffectiveOffset(localTime)) =
        orthoFOV(fovYDegrees[localTime], offset)

    fun orthoDistance(ortho: Float) = pow(200f, ortho) - 1f
    fun orthoFOV(fov: Float, offset: Float) = fov / (1f + offset)

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)

        val cam = getGroup("Projection", "projection")
        cam += vi("FOV", "Field Of View, in degrees, vertical", "camera.fov", fovYDegrees, style)
        cam += vi("Perspective - Orthographic", "Sets back the camera", "camera.orthographicness", orthographicness, style)
        val depth = getGroup("Depth", "depth")
        depth += vi("Near Z", "Closest Visible Distance", "camera.depth.near", nearZ, style)
        depth += vi("Far Z", "Farthest Visible Distance", "camera.depth.far", farZ, style)
        depth += vi("Use Depth", "Causes Z-Fighting, but allows 3D", "camera.depth.enabled", null, useDepth, style) { useDepth = it }
        val chroma = getGroup("Chromatic Aberration", "chroma")
        chroma += vi("Strength", "Effect occurring in cheap lenses", chromaticAberration, style)
        chroma += vi("Offset", "Offset", chromaticOffset, style)
        chroma += vi("Rotation", "Rotation/angle", chromaticAngle, style)
        val dist = getGroup("Distortion", "distortion")
        dist += vi("Distortion", "Params: R², R⁴, Scale", distortion, style)
        dist += vi("Distortion Offset", "Moves the center of the distortion", distortionOffset, style)
        val vignette = getGroup("Vignette", "vignette")
        vignette += vi("Vignette Color", "Color of the border", "vignette.color", vignetteColor, style)
        vignette += vi("Vignette Strength", "Strength of the colored border", "vignette.strength", vignetteStrength, style)
        val bloom = getGroup("Bloom", "bloom")
        bloom += vi("Intensity", "Brightness of effect, 0 = off", "bloom.intensity", bloomIntensity, style)
        bloom += vi("Effect Size", "How much it is blurred", "bloom.size", bloomSize, style)
        bloom += vi("Threshold", "Minimum brightness", "bloom.threshold", bloomThreshold, style)
        val color = getGroup("Color", "color")
        color += vi(
            "Tone Mapping",
            "Maps large ranges of brightnesses (e.g. HDR) to monitor color space",
            null, toneMapping, style
        ) { toneMapping = it }
        color += vi("Look Up Table", "LUT, Look Up Table for colors, formatted like in UE4", null, lut, style) {
            lut = it
        }
        val cg = getGroup("Color Grading (ASC CDL)", "color-grading")
        cg += vi("Power", "sRGB, Linear, a kind of contrast", "cg.power", cgPower, style)
        cg += vi(
            "Saturation",
            "0 = gray scale, 1 = normal, -1 = inverted colors",
            cgSaturation, style
        )
        cg += vi("Slope", "Intensity or Tint", "cg.slope", cgSlope, style)
        cg += vi("Offset", "Can be used to color black objects, or add a tint", "cg.offset", cgOffset, style)
        val editor = getGroup("Editor", "editor")
        editor += vi(
            "Only Show Target",
            "Forces the viewport to have the correct aspect ratio",
            null, onlyShowTarget, style
        ) { onlyShowTarget = it }
        val ops = getGroup("Operations", "operations")
        ops += TextButton("Reset Transform", false, style)
            .setSimpleClickListener { resetTransform(true) }
            .setTooltip("If accidentally moved")
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        if (GFX.isFinalRendering) return
        if (this === currentlyDrawnCamera) return

        val offset = getEffectiveOffset(time)
        val fov = getEffectiveFOV(time, offset)
        val near = getEffectiveNear(time, offset)
        val far = getEffectiveFar(time, offset)

        drawCamera(stack, offset, color, fov, near, far)

    }

    fun resetTransform(updateHistory: Boolean) {
        if (updateHistory) {
            RemsStudio.largeChange("Reset Camera Transform"){
                resetTransform(false)
            }
        } else {
            putValue(position, Vector3f(0f, 0f, 1f), false)
            putValue(scale, Vector3f(1f, 1f, 1f), false)
            putValue(skew, Vector2f(0f, 0f), false)
            putValue(rotationYXZ, Vector3f(), false)
        }
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
        writer.writeObject(this, "bloomIntensity", bloomIntensity)
        writer.writeObject(this, "bloomSize", bloomSize)
        writer.writeObject(this, "bloomThreshold", bloomThreshold)
        writer.writeInt("toneMapping", toneMapping.id, true)
        writer.writeBoolean("onlyShowTarget", onlyShowTarget)
        writer.writeBoolean("useDepth", useDepth)
        writer.writeFile("lut", lut)
        writer.writeObject(this, "cgSaturation", cgSaturation)
        writer.writeObject(this, "cgOffset", cgOffset)
        writer.writeObject(this, "cgSlope", cgSlope)
        writer.writeObject(this, "cgPower", cgPower)
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "onlyShowTarget" -> onlyShowTarget = value
            "useDepth" -> useDepth = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
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
            "bloomIntensity" -> bloomIntensity.copyFrom(value)
            "bloomThreshold" -> bloomThreshold.copyFrom(value)
            "bloomSize" -> bloomSize.copyFrom(value)
            "cgSaturation" -> cgSaturation.copyFrom(value)
            "cgOffset" -> cgOffset.copyFrom(value)
            "cgSlope" -> cgSlope.copyFrom(value)
            "cgPower" -> cgPower.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "lut" -> lut = File(value)
            else -> super.readString(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "toneMapping" -> toneMapping = ToneMappers.values().firstOrNull { it.id == value } ?: toneMapping
            else -> super.readInt(name, value)
        }
    }

    override fun getSymbol() = DefaultConfig["ui.symbol.camera", "\uD83C\uDFA5"]
    override fun getClassName() = Dict["Camera", "obj.camera"]

    companion object {

        const val DEFAULT_VIGNETTE_STRENGTH = 5f

    }

}