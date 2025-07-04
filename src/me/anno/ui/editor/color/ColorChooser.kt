package me.anno.ui.editor.color

import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.input.Input
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.color.spaces.HSI
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.ui.editor.color.spaces.HSV
import me.anno.ui.input.ColorInput.Companion.pickColor
import me.anno.ui.input.EnumInput
import me.anno.ui.input.components.ColorPalette
import me.anno.ui.input.components.EmptyColorPalette
import me.anno.utils.Color.black4
import me.anno.utils.Color.toHexColor
import me.anno.utils.ColorParsing.parseColorComplex
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.min

open class ColorChooser(
    style: Style,
    val withAlpha: Boolean,
    val palette: ColorPalette
) : PanelListY(style) {

    constructor(style: Style, palette: ColorPalette) : this(style, true, palette)
    constructor(style: Style) : this(style, true, EmptyColorPalette(style))

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

    val rgb = Vector3f()
    val rgba get() = Vector4f(colorSpace.toRGB(Vector3f(hue, saturation, lightness)), opacity)

    var visualisation = lastVisualisation ?: ColorVisualisation.WHEEL
    var colorSpace = getDefaultColorSpace()
        set(value) {
            if (field != value) {
                val rgb = field.toRGB(Vector3f(hue, saturation, lightness))
                val newHSL = value.fromRGB(rgb)
                field = value
                setHSL(newHSL.x, newHSL.y, newHSL.z, opacity, value, -1, true)
                colorSpaceInput.setOption(ColorSpace.list.value.indexOf(value))
                lastColorSpace = value
            }
        }

    private val hslBox = HSVBoxMain(this, Vector3f(), Vector3f(0f, 1f, 0f), Vector3f(0f, 0f, 1f), style)

    private val hueChooserSpace = SpacerPanel(0, 2, style)
    private val hueChooser = HueBar(this, style)
    private val alphaBar = if (withAlpha) AlphaBar(this, style) else null

    private val colorSpaceInput = EnumInput(
        NameDesc(
            "Color Space",
            "Color Layout: which colors are where?, e.g. color circle",
            "ui.input.color.colorSpace"
        ),
        colorSpace.nameDesc,
        ColorSpace.list.value.map { it.nameDesc }, style
    ).setChangeListener { _, index, _ ->
        colorSpace = ColorSpace.list.value[index]
    }

    private val styleInput = EnumInput(
        NameDesc("Style"), true,
        visualisation.nameDesc,
        ColorVisualisation.entries.map { it.nameDesc },
        style
    ).setChangeListener { _, index, _ ->
        visualisation = ColorVisualisation.entries[index]
        lastVisualisation = visualisation
    }.setTooltip("Style, does not change values")

    private val pickButton = TextButton(
        NameDesc(
            "Pick", "Takes a screenshot, and lets you choose a pixel of your liking. Zoom in via mouse wheel.",
            ""
        ),
        false, style
    ).addLeftClickListener {
        pickColor(windowStack, style) { color ->
            setARGB(color, -1, true)
        }
    }

    init {
        this += colorSpaceInput
        this += styleInput
        this += pickButton
        this += SpacerPanel(0, 2, style)
        this += hslBox
        this += hueChooserSpace
        this += hueChooser
        if (alphaBar != null) {
            this += SpacerPanel(0, 2, style)
            this += alphaBar
        }
        this += palette
        palette.onColorSelected = { setARGB(it, -1, true) }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val needsHueChooser = visualisation.needsHueChooser
        hueChooser.isVisible = needsHueChooser
        hueChooserSpace.isVisible = needsHueChooser
        super.draw(x0, y0, x1, y1)
    }

    fun setARGB(argb: Int, mask: Int, notify: Boolean) {
        setRGBA(
            (argb.shr(16) and 255) / 255f,
            (argb.shr(8) and 255) / 255f,
            (argb and 255) / 255f,
            (argb.shr(24) and 255) / 255f,
            mask,
            notify
        )
    }

    fun setRGBA(v: Vector4f, mask: Int, notify: Boolean) = setRGBA(v.x, v.y, v.z, v.w, mask, notify)
    fun setRGBA(r: Float, g: Float, b: Float, a: Float, mask: Int, notify: Boolean) {
        val hsl = colorSpace.fromRGB(Vector3f(r, g, b))
        setHSL(hsl.x, hsl.y, hsl.z, clamp(a, 0f, 1f), colorSpace, mask, notify)
    }

    fun setHSL(h: Float, s: Float, l: Float, a: Float, newColorSpace: ColorSpace, mask: Int, notify: Boolean) {
        hue = h
        saturation = s
        lightness = l
        opacity = clamp(a, 0f, 1f)
        this.colorSpace = newColorSpace
        colorSpace.toRGB(Vector3f(hue, saturation, lightness), rgb)
        if (notify) {
            // if any HSL component changed, change all RGB components
            val mask1 = (mask.and(7) != 0).toInt(7) or mask.and(8)
            changeListener(rgb.x, rgb.y, rgb.z, opacity, mask1)
        }
    }

    fun getColor() = Vector4f(rgb, opacity)

    fun drawColorBox(
        x: Int, y: Int, w: Int, h: Int,
        d0: Vector3f, du: Vector3f, dv: Vector3f, dh: Float,
        mainBox: Boolean
    ) {
        val visualisation = if (mainBox) visualisation else ColorVisualisation.BOX
        visualisation.drawColorBox(
            x, y, w, h,
            d0, du, dv, dh, this
        )
    }

    fun drawColorBox(element: Panel, d0: Vector3f, du: Vector3f, dv: Vector3f, dh: Float, mainBox: Boolean) {
        drawColorBox(element.x, element.y, element.width, element.height, d0, du, dv, dh, mainBox)
    }

    // todo change mouse cursor if on active area of color chooser

    var resetListener: (() -> Vector4f)? = null
        private set

    fun setResetListener(listener: (() -> Vector4f)?): ColorChooser {
        resetListener = listener
        return this
    }

    private var changeListener: (x: Float, y: Float, z: Float, w: Float, mask: Int) -> Unit = { _, _, _, _, _ -> }
    fun setChangeRGBListener(listener: (x: Float, y: Float, z: Float, w: Float, mask: Int) -> Unit): ColorChooser {
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
            is Int -> setARGB(color, -1, true)
            is Vector4f -> setRGBA(color, -1, true)
            null -> LOGGER.warn("Didn't understand color $data")
            else -> LOGGER.warn("Color type $data -> $color isn't yet supported for ColorChooser")
        }
    }

    override fun onEmpty(x: Float, y: Float) {
        val default = resetListener?.invoke() ?: black4
        setRGBA(default.x, default.y, default.z, default.w, -1, true)
    }

    override fun clone(): ColorChooser {
        val clone = ColorChooser(
            style, withAlpha,
            palette.clone()
        )
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyIntoExceptChildren(dst)
        if (dst !is ColorChooser) return
        dst.colorSpace = colorSpace
        dst.visualisation = visualisation
        dst.hue = hue
        dst.saturation = saturation
        dst.lightness = lightness
        dst.opacity = opacity
        // only works, if there is no reference involved
        dst.changeListener = changeListener
        dst.rgb.set(rgb)
        dst.rgba.set(rgba)
    }

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
            HSI.toString()
        }
    }
}