package me.anno.ui.editor.color

import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.input.Input
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.ui.editor.color.spaces.HSV
import me.anno.ui.editor.color.spaces.LinearHSI
import me.anno.ui.input.EnumInput
import me.anno.ui.input.components.ColorPalette
import me.anno.ui.style.Style
import me.anno.utils.Color.toHexColor
import me.anno.utils.ColorParsing.parseColorComplex
import me.anno.utils.structures.tuples.Quad
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f
import org.joml.Vector4fc
import kotlin.math.min

open class ColorChooser(
    style: Style,
    val withAlpha: Boolean,
    val palette: ColorPalette
) : PanelListY(style) {

    constructor(style: Style, palette: ColorPalette) : this(style, true, palette)
    constructor(style: Style) : this(style, true, object : ColorPalette(1, 1, style) {
        override fun setColor(x: Int, y: Int, color: Int) {}
        override fun getColor(x: Int, y: Int) = 0
    })

    // color section
    // bottom section:
    // hue
    // check box for hsl/hsluv
    // field(?) to enter rgb codes

    var hue = 0.5f
    var saturation = 0.5f
    var lightness = 0.5f
    var opacity = 1.0f

    @NotSerializedProperty
    var isDownInRing = false

    private val maxWidth = style.getSize("colorChooser.maxWidth", 500)
    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(min(w, maxWidth), h)
    }

    override fun getVisualState() = Quad(hue, saturation, lightness, opacity)

    val rgb = Vector3f()
    val rgba get() = Vector4f(colorSpace.toRGB(Vector3f(hue, saturation, lightness)), opacity)

    var visualisation = lastVisualisation ?: ColorVisualisation.WHEEL
    var colorSpace = getDefaultColorSpace()
        set(value) {
            if (field != value) {
                val rgb = field.toRGB(Vector3f(hue, saturation, lightness))
                val newHSL = value.fromRGB(rgb)
                field = value
                setHSL(newHSL.x, newHSL.y, newHSL.z, opacity, value, true)
                colorSpaceInput.setOption(ColorSpace.list.value.indexOf(value))
                lastColorSpace = value
            }
        }

    private val hslBox = HSVBoxMain(this, Vector3f(), Vector3f(0f, 1f, 0f), Vector3f(0f, 0f, 1f), style)

    private val hueChooserSpace = SpacerPanel(0, 2, style)
    private val hueChooser = HueBar(this, style)
    private val alphaBar = if (withAlpha) AlphaBar(this, style) else null

    private val colorSpaceInput = EnumInput(
        "Color Space",
        "Color Layout: which colors are where?, e.g. color circle", "ui.input.color.colorSpace",
        colorSpace.naming.name,
        ColorSpace.list.value.map { it.naming }, style
    )
        .setChangeListener { _, index, _ ->
            val newColorSpace = ColorSpace.list.value[index]
            colorSpace = newColorSpace
            invalidateLayout()
        }

    private val styleInput = EnumInput(
        "", false,
        visualisation.naming.name,
        ColorVisualisation.values().map { it.naming },
        style
    ).setChangeListener { _, index, _ ->
        visualisation = ColorVisualisation.values()[index]
        lastVisualisation = visualisation
    }.setTooltip("Style, does not change values")

    init {
        val spaceBox = PanelListX(style)
        this += spaceBox
        spaceBox += colorSpaceInput.setWeight(1f)
        spaceBox += styleInput
        this += SpacerPanel(0, 2, style)
        this += hslBox
        this += hueChooserSpace
        this += hueChooser
        if (alphaBar != null) {
            this += SpacerPanel(0, 2, style)
            this += alphaBar
        }
        this += palette
        palette.onColorSelected = { setARGB(it, true) }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val needsHueChooser = Visibility[visualisation.needsHueChooser]
        hueChooser.visibility = needsHueChooser
        hueChooserSpace.visibility = needsHueChooser
        super.onDraw(x0, y0, x1, y1)
    }

    fun setARGB(argb: Int, notify: Boolean) {
        setRGBA(
            (argb.shr(16) and 255) / 255f,
            (argb.shr(8) and 255) / 255f,
            (argb and 255) / 255f,
            (argb.shr(24) and 255) / 255f,
            notify
        )
    }

    fun setRGBA(v: Vector4fc, notify: Boolean) = setRGBA(v.x(), v.y(), v.z(), v.w(), notify)
    fun setRGBA(r: Float, g: Float, b: Float, a: Float, notify: Boolean) {
        val hsl = colorSpace.fromRGB(Vector3f(r, g, b))
        setHSL(hsl.x, hsl.y, hsl.z, clamp(a, 0f, 1f), colorSpace, notify)
    }

    fun setHSL(h: Float, s: Float, l: Float, a: Float, newColorSpace: ColorSpace, notify: Boolean) {
        hue = h
        saturation = s
        lightness = l
        opacity = clamp(a, 0f, 1f)
        this.colorSpace = newColorSpace
        colorSpace.toRGB(Vector3f(hue, saturation, lightness), rgb)
        if (notify) {
            changeListener(rgb.x, rgb.y, rgb.z, opacity)
        }
    }

    fun getColor() = Vector4f(rgb, opacity)

    fun drawColorBox(element: Panel, d0: Vector3fc, du: Vector3fc, dv: Vector3fc, dh: Float, mainBox: Boolean) {
        drawColorBox(
            element.x, element.y, element.w, element.h,
            d0, du, dv, dh,
            if (mainBox) visualisation else ColorVisualisation.BOX
        )
    }

    fun drawColorBox(
        x: Int, y: Int, w: Int, h: Int,
        d0: Vector3fc, du: Vector3fc, dv: Vector3fc, dh: Float,
        spaceStyle: ColorVisualisation
    ) {
        val shader = colorSpace.getShader(spaceStyle)
        shader.use()
        posSize(shader, x, y + h, w, -h)
        val sharpness = min(w, h) * 0.25f + 1f
        when (spaceStyle) {
            ColorVisualisation.WHEEL -> {
                shader.v3f("v0", d0.x() + hue * dh, d0.y(), d0.z())
                shader.v3f("du", du)
                shader.v3f("dv", dv)
                val hue0 = colorSpace.hue0
                shader.v2f("ringSL", hue0.y(), hue0.z())
                shader.v1f("sharpness", sharpness)
                GFX.flat01.draw(shader)
            }
            ColorVisualisation.CIRCLE -> {
                shader.v1f("lightness", lightness)
                shader.v1f("sharpness", sharpness)
                GFX.flat01.draw(shader)
            }
            ColorVisualisation.BOX -> {
                shader.v3f("v0", d0.x() + hue * dh, d0.y(), d0.z())
                shader.v3f("du", du)
                shader.v3f("dv", dv)
                // shader.v1("sharpness", sharpness)
                GFX.flat01.draw(shader)
            }
        }
    }

    var resetListener: () -> Vector4f = { Vector4f(0f, 0f, 0f, 1f) }
        private set

    fun setResetListener(listener: () -> Vector4f): ColorChooser {
        resetListener = listener
        return this
    }

    private var changeListener: (x: Float, y: Float, z: Float, w: Float) -> Unit = { _, _, _, _ -> }
    fun setChangeRGBListener(listener: (x: Float, y: Float, z: Float, w: Float) -> Unit): ColorChooser {
        changeListener = listener
        return this
    }

    override fun onCopyRequested(x: Float, y: Float): String {
        val cssCompatible = DefaultConfig["editor.copyColorsCSS-compatible", false]
        val useAlpha = DefaultConfig["editor.copyColorsCSS-withAlpha", true]
        return if (cssCompatible != Input.isShiftDown) {
            colorSpace.toRGB(hue, saturation, lightness, if (useAlpha) opacity else 1f).toHexColor()
        } else {
            "${colorSpace.serializationName}(${hue.f3()},${saturation.f3()},${lightness.f3()},${opacity.f3()})"
        }
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (val color = parseColorComplex(data)) {
            is Int -> setARGB(color, true)
            is Vector4f -> setRGBA(color, true)
            null -> LOGGER.warn("Didn't understand color $data")
            else -> throw RuntimeException("Color type $data -> $color isn't yet supported for ColorChooser")
        }
    }

    override fun onEmpty(x: Float, y: Float) {
        val default = resetListener()
        setRGBA(default.x, default.y, default.z, default.w, true)
    }

    override fun clone(): ColorChooser {
        val clone = ColorChooser(
            style, withAlpha,
            palette.clone()
        )
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as ColorChooser
        clone.colorSpace = colorSpace
        clone.visualisation = visualisation
        clone.hue = hue
        clone.saturation = saturation
        clone.lightness = lightness
        clone.opacity = opacity
        // only works, if there is no reference involved
        clone.changeListener = changeListener
        clone.rgb.set(rgb)
        clone.rgba.set(rgba)
    }

    override val className: String = "ColorChooser"

    companion object {
        private val LOGGER = LogManager.getLogger(ColorChooser::class)
        var circleBarRatio = 0.2f
        var lastVisualisation: ColorVisualisation? = null
        var lastColorSpace: ColorSpace? = null
        fun getDefaultColorSpace(): ColorSpace {
            return lastColorSpace
                ?: DefaultConfig["default.colorSpace", "HSLuv"].run { ColorSpace.list.value.firstOrNull { it.serializationName == this } }
                ?: HSLuv
        }

        init {
            HSLuv.toString()
            HSV.toString()
            LinearHSI.toString()
        }
    }

}