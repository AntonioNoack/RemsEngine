package me.anno.ui.editor.color

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.TextureLib.colorShowTexture
import me.anno.image.svg.SVGStyle.Companion.parseColorComplex
import me.anno.input.Input
import me.anno.objects.animation.AnimatedProperty
import me.anno.studio.RemsStudio.onSmallChange
import me.anno.studio.Studio.editorTime
import me.anno.ui.base.Panel
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.utils.f3
import me.anno.utils.get
import me.anno.utils.toHex
import org.apache.logging.log4j.LogManager
import org.hsluv.HSLuvColorSpace
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max
import kotlin.math.roundToInt

class ColorChooser(style: Style, withAlpha: Boolean, val owningProperty: AnimatedProperty<*>?) : PanelListY(style) {

    // color section
    // bottom section:
    // hue
    // check box for hsl/hsluv
    // field(?) to enter rgb codes

    var visualisation = lastVisualisation ?: ColorVisualisation.WHEEL
    var colorSpace = lastColorSpace ?: ColorSpace[DefaultConfig["color.defaultColorSpace", "HSLuv"]] ?: ColorSpace.HSLuv

    var isDownInRing = false
    val hslBox = HSVBoxMain(this, Vector3f(), Vector3f(0f, 1f, 0f), Vector3f(0f, 0f, 1f), style)

    val hueChooserSpace = SpacePanel(0, 2, style)
    val hueChooser = object : HSVBox(this,
        Vector3f(0f, 1f, 0.75f),
        Vector3f(1f, 0f, 0f),
        Vector3f(), 0f, style, 1f, { hue, _ ->
            setHSL(hue, saturation, lightness, opacity, colorSpace, true)
            onSmallChange("color-hue")
        }) {
        override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.onDraw(x0, y0, x1, y1)
            val x = x0 + ((x1 - x0) * hue).roundToInt()
            GFX.drawRect(x, y0, 1, y1 - y0, black)
        }
    }

    val alphaBar = if (withAlpha) {
        object : HSVBox(this,
            Vector3f(0f, 0f, 0f),
            Vector3f(0f, 0f, 1f),
            Vector3f(0f, 0f, 0f), 0f, style, 1f,
            { opacity, _ ->
                setHSL(hue, saturation, lightness, clamp(opacity, 0f, 1f), colorSpace, true)
                onSmallChange("color-alpha")
            }) {
            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                // super.onDraw(x0, y0, x1, y1)
                val x = x0 + ((x1 - x0) * opacity).roundToInt()
                // lerp transparency for the alpha bar
                for (dx in 0 until w) {
                    GFX.drawRect(this.x + dx, y, 1, h, 0xffffff or (dx * 255 / w).shl(24))
                }
                GFX.drawTexture(this.x, y, w, h, colorShowTexture, -1, Vector4f(w.toFloat() / h, 1f, 0f, 0f))
                GFX.drawRect(x, y0, 1, y1 - y0, black)
            }
        }
    } else null

    val colorSpaceInput = EnumInput(
        "Color Space", false, colorSpace.name,
        ColorSpace.list.map { it.name }, style
    )
        .setChangeListener { it, _, _ ->
            val newColorSpace = ColorSpace[it]
            lastColorSpace = newColorSpace
            if (newColorSpace != null && newColorSpace != colorSpace) {
                val rgb = colorSpace.toRGB(Vector3f(hue, saturation, lightness))
                val newHSL = newColorSpace.fromRGB(rgb)
                setHSL(newHSL.x, newHSL.y, newHSL.z, opacity, newColorSpace, true)
                // onSmallChange("color-space")
            }
        }

    val styleInput =
        EnumInput("Style", false, visualisation.displayName, ColorVisualisation.values().map { it.displayName }, style)
            .setChangeListener { it, _, _ ->
                visualisation = ColorVisualisation.values().firstOrNull { v -> v.displayName == it } ?: visualisation
                lastVisualisation = visualisation
                // onSmallChange("color-style")
            }

    init {
        val spaceBox = PanelListX(style)
        this += spaceBox
        spaceBox += colorSpaceInput.setWeight(1f)
        spaceBox += styleInput
        this += SpacePanel(0, 2, style)
        this += hslBox
        this += hueChooserSpace
        this += hueChooser
        if (alphaBar != null) {
            this += SpacePanel(0, 2, style)
            this += alphaBar
        }
    }

    var lastTime = editorTime
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        if (lastTime != editorTime && owningProperty != null) {
            lastTime = editorTime
            setRGBA(owningProperty[editorTime] as Vector4f, false)
        }
        val needsHueChooser = Visibility[visualisation.needsHueChooser]
        hueChooser.visibility = needsHueChooser
        hueChooserSpace.visibility = needsHueChooser
        super.onDraw(x0, y0, x1, y1)
    }

    var hue = 0.5f
    var saturation = 0.5f
    var lightness = 0.5f
    var opacity = 1.0f

    fun setARGB(argb: Int, notify: Boolean) {
        setRGBA(
            (argb.shr(16) and 255) / 255f,
            (argb.shr(8) and 255) / 255f,
            (argb and 255) / 255f,
            (argb.shr(24) and 255) / 255f,
            notify
        )
    }

    fun setRGBA(v: Vector4f, notify: Boolean) = setRGBA(v.x, v.y, v.z, v.w, notify)
    fun setRGBA(r: Float, g: Float, b: Float, a: Float, notify: Boolean) {
        val hsluv = HSLuvColorSpace.rgbToHsluv(
            doubleArrayOf(
                r.toDouble(), g.toDouble(), b.toDouble()
            )
        )
        setHSL(
            hsluv[0].toFloat() / 360f,
            hsluv[1].toFloat() / 100f,
            hsluv[2].toFloat() / 100f,
            clamp(a, 0f, 1f),
            colorSpace, notify
        )
    }

    fun setHSL(h: Float, s: Float, l: Float, a: Float, newColorSpace: ColorSpace, notify: Boolean) {
        hue = h
        saturation = s
        lightness = l
        opacity = clamp(a, 0f, 1f)
        this.colorSpace = newColorSpace
        val rgb = colorSpace.toRGB(Vector3f(hue, saturation, lightness))
        if (notify) changeRGBListener(rgb.x, rgb.y, rgb.z, opacity)
    }

    fun drawColorBox(element: Panel, d0: Vector3f, du: Vector3f, dv: Vector3f, dh: Float, mainBox: Boolean) {
        val spaceStyle = if (mainBox) visualisation else ColorVisualisation.BOX
        val shader = colorSpace.getShader(spaceStyle)
        shader.use()
        GFX.posSize(shader, element.x, element.y + element.h, element.w, -element.h)
        val sharpness = max(w, h) * 0.25f + 1f
        when (spaceStyle) {
            ColorVisualisation.WHEEL -> {
                shader.v3("v0", d0.x + hue * dh, d0.y, d0.z)
                shader.v3("du", du)
                shader.v3("dv", dv)
                val hue0 = colorSpace.hue0
                shader.v2("ringSL", hue0.y, hue0.z)
                shader.v1("sharpness", sharpness)
                GFX.flat01.draw(shader)
            }
            ColorVisualisation.CIRCLE -> {
                shader.v1("lightness", lightness)
                shader.v1("sharpness", sharpness)
                GFX.flat01.draw(shader)
            }
            ColorVisualisation.BOX -> {
                shader.v3("v0", d0.x + hue * dh, d0.y, d0.z)
                shader.v3("du", du)
                shader.v3("dv", dv)
                // shader.v1("sharpness", sharpness)
                GFX.flat01.draw(shader)
            }
        }
    }

    var changeRGBListener: (x: Float, y: Float, z: Float, w: Float) -> Unit = { _, _, _, _ -> }
    fun setChangeRGBListener(listener: (x: Float, y: Float, z: Float, w: Float) -> Unit): ColorChooser {
        changeRGBListener = listener
        return this
    }

    override fun onCopyRequested(x: Float, y: Float): String {
        return "${colorSpace.serializationName}(${hue.f3()},${saturation.f3()},${lightness.f3()},${opacity.f3()})"
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (val color = parseColorComplex(data)) {
            is Int -> {
                setARGB(color, true)
                onSmallChange("color-paste-argb")
            }
            is Vector4f -> {
                setRGBA(color.x, color.y, color.z, color.w, true)
                onSmallChange("color-paste-vec4")
            }
            null -> LOGGER.warn("Didn't understand color $data")
            else -> throw RuntimeException("Color type $data -> $color isn't yet supported for ColorChooser")
        }
    }

    override fun onEmpty(x: Float, y: Float) {
        val default = owningProperty?.defaultValue ?: Vector4f(0f)
        setRGBA(default[0], default[1], default[2], default[3], true)
        onSmallChange("color-empty")
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ColorChooser::class)
        val CircleBarRatio = 0.2f
        var lastVisualisation: ColorVisualisation? = null
        var lastColorSpace: ColorSpace? = null
    }

}