package me.anno.objects

import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.white4
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.Dict
import me.anno.objects.effects.ToneMappers
import me.anno.objects.models.CameraModel.drawCamera
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.currentlyDrawnCamera
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.maths.Maths.pow
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.types.Casting.castToFloat2
import org.joml.Matrix4fArrayList
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4fc

class Camera(parent: Transform? = null) : Transform(parent) {

    // kind of done allow cameras to be merged
    // kind of done allow cameras to film camera (post processing) -> create a stack of cameras/scenes?
    // by implementing SoftLink: scenes can be included in others

    // orthographic-ness by setting the camera back some amount, and narrowing the view

    var lut: FileReference = InvalidRef
    val nearZ = AnimatedProperty.floatPlus(0.001f)
    val farZ = AnimatedProperty.floatPlus(1000f)
    val fovYDegrees = AnimatedProperty(fovType, 90f)
    val chromaticAberration = AnimatedProperty.floatPlus()
    val chromaticOffset = AnimatedProperty.vec2()
    val chromaticAngle = AnimatedProperty.float()
    val distortion = AnimatedProperty.vec3()
    val distortionOffset = AnimatedProperty.vec2()
    val orthographicness = AnimatedProperty.float01()
    val vignetteStrength = AnimatedProperty.floatPlus()
    val vignetteColor = AnimatedProperty.color3(Vector3f(0f, 0f, 0f))

    val orbitRadius = AnimatedProperty.floatPlus(1f)

    val cgOffsetAdd = AnimatedProperty.color3(Vector3f())
    val cgOffsetSub = AnimatedProperty.color3(Vector3f())
    val cgSlope = AnimatedProperty.color(white4)
    val cgPower = AnimatedProperty.color(white4)
    val cgSaturation = AnimatedProperty.float(1f) // only allow +? only 01?

    val bloomSize = AnimatedProperty.floatPlus(0.05f)
    val bloomIntensity = AnimatedProperty.floatPlus(0f)
    val bloomThreshold = AnimatedProperty.floatPlus(0.8f)

    var toneMapping = ToneMappers.RAW8

    var onlyShowTarget = true
    var useDepth = true

    fun getEffectiveOffset(localTime: Double) = orthographicDistance(orthographicness[localTime])
    fun getEffectiveNear(localTime: Double, offset: Float = getEffectiveOffset(localTime)) = nearZ[localTime] + offset
    fun getEffectiveFar(localTime: Double, offset: Float = getEffectiveOffset(localTime)) = farZ[localTime] + offset
    fun getEffectiveFOV(localTime: Double, offset: Float = getEffectiveOffset(localTime)) =
        orthographicFOV(fovYDegrees[localTime], offset)

    fun orthographicDistance(orthographicness: Float) = pow(200f, orthographicness) - 1f
    fun orthographicFOV(fov: Float, offset: Float) = fov / (1f + offset)

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)

        val transform = getGroup("Transform", "", "transform")
        transform += vi("Orbit Radius", "Orbiting Distance", "camera.orbitDis", orbitRadius, style)

        val cam = getGroup("Projection", "How rays of light are mapped to the screen", "projection")
        cam += vi("FOV", "Field Of View, in degrees, vertical", "camera.fov", fovYDegrees, style)
        cam += vi(
            "Perspective - Orthographic",
            "Sets back the camera", "camera.orthographicness",
            orthographicness, style
        )
        val depth = getGroup("Depth", "Z-axis related settings; from camera perspective", "depth")
        depth += vi("Near Z", "Closest Visible Distance", "camera.depth.near", nearZ, style)
        depth += vi("Far Z", "Farthest Visible Distance", "camera.depth.far", farZ, style)
        depth += vi(
            "Use Depth",
            "Causes Z-Fighting, but allows 3D", "camera.depth.enabled",
            null, useDepth, style
        ) { useDepth = it }
        val chroma = getGroup("Chromatic Aberration", "Effect occurring in cheap lenses", "chroma")
        chroma += vi("Strength", "How large the effect is", "camera.chromaStrength", chromaticAberration, style)
        chroma += vi("Offset", "Offset", "camera.chromaOffset", chromaticOffset, style)
        chroma += vi("Rotation", "Rotation/angle in Degrees", "camera.chromaRotation", chromaticAngle, style)
        val dist = getGroup("Distortion", "Transforms the image", "distortion")
        dist += vi("Distortion", "Params: R², R⁴, Scale", distortion, style)
        dist += vi("Distortion Offset", "Moves the center of the distortion", distortionOffset, style)
        val vignette = getGroup("Vignette", "Darkens/colors the border", "vignette")
        vignette += vi("Vignette Color", "Color of the border", "vignette.color", vignetteColor, style)
        vignette += vi(
            "Vignette Strength",
            "Strength of the colored border", "vignette.strength",
            vignetteStrength,
            style
        )
        val bloom = getGroup("Bloom", "Adds a light halo around bright objects", "bloom")
        bloom += vi("Intensity", "Brightness of effect, 0 = off", "bloom.intensity", bloomIntensity, style)
        bloom += vi("Effect Size", "How much it is blurred", "bloom.size", bloomSize, style)
        bloom += vi("Threshold", "Minimum brightness", "bloom.threshold", bloomThreshold, style)
        val color = getGroup("Color", "Tint and Tonemapping", "color")
        color += vi(
            "Tone Mapping",
            "Maps large ranges of brightnesses (e.g. HDR) to monitor color space", "camera.toneMapping",
            null, toneMapping, style
        ) { toneMapping = it }
        color += vi(
            "Look Up Table",
            "LUT, Look Up Table for colors, formatted like in UE4", "camera.lut", null, lut, style
        ) {
            lut = it
        }

        ColorGrading.createInspector(
            this,
            cgPower,
            cgSaturation,
            cgSlope,
            cgOffsetAdd,
            cgOffsetSub,
            { it },
            getGroup,
            style
        )

        val editor = getGroup("Editor", "Settings, which only effect editing", "editor")
        editor += vi(
            "Only Show Target",
            "Forces the viewport to have the correct aspect ratio",
            null, onlyShowTarget, style
        ) { onlyShowTarget = it }
        val ops = getGroup("Operations", "Actions", "operations")
        ops += TextButton("Reset Transform", "If accidentally moved", "obj.camera.resetTransform", false, style)
            .setSimpleClickListener { resetTransform(true) }
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {

        if (GFX.isFinalRendering) return
        if (this === currentlyDrawnCamera) return

        val offset = getEffectiveOffset(time)
        val fov = getEffectiveFOV(time, offset)
        val near = getEffectiveNear(time, offset)
        val far = getEffectiveFar(time, offset)

        super.onDraw(stack, time, color)

        stack.translate(0f, 0f, orbitRadius[time])

        drawCamera(stack, offset, color, fov, near, far)

    }

    fun resetTransform(updateHistory: Boolean) {
        if (updateHistory) {
            RemsStudio.largeChange("Reset Camera Transform") {
                resetTransform(false)
            }
        } else {
            putValue(position, Vector3f(), false)
            putValue(scale, Vector3f(1f, 1f, 1f), false)
            putValue(skew, Vector2f(0f, 0f), false)
            putValue(rotationYXZ, Vector3f(), false)
            putValue(orbitRadius, 1f, false)
            putValue(nearZ, 0.001f, false)
            putValue(farZ, 1000f, false)
        }
    }

    fun cloneTransform(src: Transform, srcTime: Double) {
        putValue(position, src.position[srcTime], false)
        putValue(rotationYXZ, src.rotationYXZ[srcTime], false)
        putValue(scale, src.scale[srcTime], false)
        putValue(skew, src.skew[srcTime], false)
        if (src is Camera) {
            putValue(fovYDegrees, src.fovYDegrees[srcTime], false)
            putValue(orbitRadius, src.orbitRadius[srcTime], false)
            putValue(nearZ, src.nearZ[srcTime], false)
            putValue(farZ, src.farZ[srcTime], false)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "orbitRadius", orbitRadius)
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
        writer.writeObject(this, "cgOffsetAdd", cgOffsetAdd)
        writer.writeObject(this, "cgOffsetSub", cgOffsetSub)
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
            "orbitRadius" -> orbitRadius.copyFrom(value)
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
            "cgOffset", "cgOffsetAdd" -> cgOffsetAdd.copyFrom(value)
            "cgOffsetSub" -> cgOffsetSub.copyFrom(value)
            "cgSlope" -> cgSlope.copyFrom(value)
            "cgPower" -> cgPower.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "lut" -> lut = value?.toGlobalFile() ?: InvalidRef
            else -> super.readString(name, value)
        }
    }

    override fun readFile(name: String, value: FileReference) {
        when (name) {
            "lut" -> lut = value
            else -> super.readFile(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "toneMapping" -> toneMapping = ToneMappers.values().firstOrNull { it.id == value } ?: toneMapping
            else -> super.readInt(name, value)
        }
    }

    override val className get() = "Camera"
    override val defaultDisplayName = Dict["Camera", "obj.camera"]
    override val symbol = DefaultConfig["ui.symbol.camera", "\uD83C\uDFA5"]

    companion object {

        // linear and exponential aren't really the correct types...
        // around 0f and 180f should have exponential speed decay
        val fovType = Type(90f, 1, 1f, true, true, { clamp(castToFloat2(it), 0.001f, 179.999f) }, { it })

        const val DEFAULT_VIGNETTE_STRENGTH = 5f

    }

}