package me.anno.ui.editor.color

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.objects.meshes.svg.SVGStyle.Companion.parseColor
import me.anno.ui.base.Panel
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import me.anno.utils.f3
import org.hsluv.HSLuvColorSpace
import org.joml.Vector3f
import kotlin.math.*

class ColorChooser(style: Style, withAlpha: Boolean): PanelListY(style){

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
    val hueChooser = object: HSVBox(this, Vector3f(), Vector3f(1f, 0f, 0f), Vector3f(0f,0f, 0f), 0f, style, 1f, { hue, _ ->
        setHSL(hue, saturation, lightness, opacity, colorSpace)
    } ) {
        override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.draw(x0, y0, x1, y1)
            val x = x0 + ((x1-x0) * hue).roundToInt()
            GFX.drawRect(x, y0, 1, y1-y0, black)
        }
    }

    val alphaBar = if(withAlpha){
        // todo with transparency indicator like our color show texture
        object: HSVBox(this, Vector3f(0f,0f, 0f), Vector3f(0f, 0f, 1f), Vector3f(0f, 0f, 0f), 0f, style, 1f, { opacity, _ ->
            setHSL(hue, saturation, lightness, opacity, colorSpace)
        }){
            override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
                super.draw(x0, y0, x1, y1)
                val x = x0 + ((x1-x0) * opacity).roundToInt()
                GFX.drawRect(x, y0, 1, y1-y0, black)
            }
        }
    } else null

    val colorSpaceInput = EnumInput("Color Space", false, colorSpace.name,
        ColorSpace.values.values.toSet().map { it.name }, style)
        .setChangeListener {
            val newColorSpace = ColorSpace[it]
            lastColorSpace = newColorSpace
            if(newColorSpace != null && newColorSpace != colorSpace){
                val rgb = colorSpace.toRGB(Vector3f(hue, saturation, lightness))
                val newHSL = newColorSpace.fromRGB(rgb)
                setHSL(newHSL.x, newHSL.y, newHSL.z, opacity, newColorSpace)
            }
        }
        .setTooltip("Ring/Box")

    val styleInput = EnumInput("Style", false, visualisation.displayName, ColorVisualisation.values().map { it.displayName }, style)
        .setChangeListener {
            visualisation = ColorVisualisation.values().firstOrNull { v -> v.displayName == it } ?: visualisation
            lastVisualisation = visualisation
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
        if(alphaBar != null){
            this += SpacePanel(0, 2, style)
            this += alphaBar
        }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val needsHueChooser = Visibility[visualisation.needsHueChooser]
        hueChooser.visibility = needsHueChooser
        hueChooserSpace.visibility = needsHueChooser
        super.draw(x0, y0, x1, y1)
    }

    var hue = 0.5f
    var saturation = 0.5f
    var lightness = 0.5f
    var opacity = 1.0f

    fun setARGB(argb: Int){
        setRGBA((argb.shr(16) and 255)/255f,
            (argb.shr(8) and 255)/255f,
            (argb and 255)/255f,
            (argb.shr(24) and 255)/255f)
    }

    fun setRGBA(r: Float, g: Float, b: Float, a: Float){
        val hsluv = HSLuvColorSpace.rgbToHsluv(doubleArrayOf(
            r.toDouble(), g.toDouble(), b.toDouble()
        ))
        setHSL(hsluv[0].toFloat() / 360f, hsluv[1].toFloat() / 100f, hsluv[2].toFloat() / 100f, a, colorSpace)
    }

    fun setHSL(h: Float, s: Float, l: Float, a: Float, newColorSpace: ColorSpace){
        hue = h
        saturation = s
        lightness = l
        opacity = a
        this.colorSpace = newColorSpace
        val rgb = colorSpace.toRGB(Vector3f(hue, saturation, lightness))
        changeRGBListener(rgb.x, rgb.y, rgb.z, opacity)
    }

    fun drawColorBox(element: Panel, d0: Vector3f, du: Vector3f, dv: Vector3f, dh: Float, mainBox: Boolean){
        val spaceStyle = if(mainBox) visualisation else ColorVisualisation.BOX
        val shader = colorSpace.getShader(spaceStyle)
        shader.use()
        GFX.posSize(shader, element.x, element.y + element.h, element.w, - element.h)
        val sharpness = max(w, h) * 0.25f + 1f
        when(spaceStyle){
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

    var changeRGBListener: (x: Float, y: Float, z: Float, w: Float) -> Unit = {
            _,_,_,_ ->
    }

    fun setChangeRGBListener(listener: (x: Float, y: Float, z: Float, w: Float) -> Unit): ColorChooser {
        changeRGBListener = listener
        return this
    }

    // todo copy whole timelines?
    override fun onCopyRequested(x: Float, y: Float) = "${colorSpace.serializationName}(${hue.f3()},${saturation.f3()},${lightness.f3()},${opacity.f3()})"

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        // todo better parsing function returning a rgba-vector
        setARGB(parseColor(data) ?: return)
        // todo check for HSVuv(h,s,v,a), HSV(h,s,v,a), or #... or RGB(r,g,b,a) or [1,1,0,1]
    }

    companion object {
        val CircleBarRatio = 0.2f
        var lastVisualisation: ColorVisualisation? = null
        var lastColorSpace: ColorSpace? = null
    }

}