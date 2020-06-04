package me.anno.ui.editor.color

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.objects.meshes.svg.SVGStyle.Companion.parseColor
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.components.Checkbox
import me.anno.ui.style.Style
import me.anno.utils.f3
import org.hsluv.HSLuvColorConverter
import org.joml.Vector3f

class ColorChooser(style: Style, withAlpha: Boolean): PanelListY(style){

    // color section
    // bottom section:
    // hue
    // check box for hsl/hsluv
    // field(?) to enter rgb codes

    var isHSLuv = DefaultConfig["color.useHSLuv", true]

    val hslBox = HSVBox(this, Vector3f(), Vector3f(0f, 1f, 0f), Vector3f(0f, 0f, 1f), 1f, style, 5f){ saturation, lightness ->
        setHSL(hue, saturation, lightness, opacity, isHSLuv)
    }

    val hueChooser = object: HSVBox(this, Vector3f(0f, 1f, 0.5f), Vector3f(1f, 0f, 0f), Vector3f(0f,0f, 0f), 0f, style, 1f, { hue, _ ->
        // todo convert the color, so that the rgb value stays the same
        setHSL(hue, saturation, lightness, opacity, isHSLuv)
    } ) {
        val d0_2 = Vector3f(0f, 1f, 1f)
        override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
            if(isHSLuv){
                chooser.drawColorBox(d0, du, dv, dh)
            } else {
                chooser.drawColorBox(d0_2, du, dv, dh)
            }
        }
    }

    val alphaBar = if(withAlpha){
        // todo with transparency indicator like our color show texture
        HSVBox(this, Vector3f(0f,0f, 0f), Vector3f(0f, 0f, 1f), Vector3f(0f, 0f, 0f), 0f, style, 1f) { opacity, _ ->
            setHSL(hue, saturation, lightness, opacity, isHSLuv)
        }
    } else null

    val isHSLuvBox = Checkbox(isHSLuv, 10, style)
        .setChangeListener { isHSLuv ->
            setHSL(hue, saturation, lightness, opacity, isHSLuv)
        }
        .setTooltip("HSLuv instead of HSV")

    init {
        this += hslBox
        this += SpacePanel(0, 2, style)
        val hueBox = PanelListX(style)
        hueBox += hueChooser.setWeight(1f)
        hueBox += SpacePanel(2, 0, style)
        hueBox += isHSLuvBox
        this += hueBox
        if(alphaBar != null){
            this += SpacePanel(0, 2, style)
            this += alphaBar
        }
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
        val hsluv = HSLuvColorConverter.rgbToHsluv(doubleArrayOf(
            r.toDouble(), g.toDouble(), b.toDouble()
        ))
        setHSL(hsluv[0].toFloat() / 360f, hsluv[1].toFloat() / 100f, hsluv[2].toFloat() / 100f, a, isHSLuv)
    }

    fun setHSL(h: Float, s: Float, l: Float, a: Float, isHSLuv: Boolean){
        hue = h
        saturation = s
        lightness = l
        opacity = a
        this.isHSLuv = isHSLuv
        if(isHSLuv){
            val rgb = HSLuvColorConverter.hsluvToRgb(doubleArrayOf(
                hue * 360.0,
                saturation * 100.0,
                lightness * 100.0
            ))
            changeRGBListener(rgb[0].toFloat(), rgb[1].toFloat(), rgb[2].toFloat(), opacity)
        } else {
            val rgb = HSVColorConverter.hsvToRGB(hue, saturation, lightness)
            changeRGBListener(rgb.x, rgb.y, rgb.z, opacity)
        }
    }

    fun drawColorBox(d0: Vector3f, du: Vector3f, dv: Vector3f, dh: Float){
        val shader = if(isHSLuv) GFX.hsluvShader else GFX.hslShader
        shader.use()
        shader.v2("pos", 0f, 0f)
        shader.v2("size", 1f, 1f)
        shader.v3("d0", d0.x + hue * dh, d0.y, d0.z)
        shader.v3("du", du)
        shader.v3("dv", dv)
        GFX.flat01.draw(shader)
    }

    var changeRGBListener: (x: Float, y: Float, z: Float, w: Float) -> Unit = {
            _,_,_,_ ->
    }

    fun setChangeRGBListener(listener: (x: Float, y: Float, z: Float, w: Float) -> Unit): ColorChooser {
        changeRGBListener = listener
        return this
    }

    // todo copy whole timelines?
    override fun onCopyRequested(x: Float, y: Float) = "${if(isHSLuv)"HSLuv" else "HSL"}(${hue.f3()},${saturation.f3()},${lightness.f3()},${opacity.f3()})"

    override fun onPaste(x: Float, y: Float, pasted: String) {
        // todo better parsing function returning a rgba-vector
        setARGB(parseColor(pasted) ?: return)
        // todo check for HSVuv(h,s,v,a), HSV(h,s,v,a), or #... or RGB(r,g,b,a) or [1,1,0,1]
    }

}