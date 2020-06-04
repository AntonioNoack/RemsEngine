package me.anno.ui.editor.color

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.objects.Camera
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.meshes.svg.SVGStyle.Companion.parseColor
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.EnumInput
import me.anno.ui.input.components.Checkbox
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.utils.f3
import me.anno.utils.pow
import org.hsluv.HSLuvColorConverter
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max
import kotlin.math.roundToInt

class ColorChooser(style: Style, withAlpha: Boolean): PanelListY(style){

    // color section
    // bottom section:
    // hue
    // check box for hsl/hsluv
    // field(?) to enter rgb codes

    var colorSpace = ColorSpace[DefaultConfig["color.defaultColorSpace", "HSLuv"]] ?: ColorSpace.HSLuv

    val hslBox = object: HSVBox(this, Vector3f(), Vector3f(0f, 1f, 0f), Vector3f(0f, 0f, 1f), 1f, style, 5f, { saturation, lightness ->
        setHSL(hue, saturation, lightness, opacity, colorSpace)
    }){
        override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.draw(x0, y0, x1, y1)
            val x = x0 + ((x1-x0) * saturation).roundToInt()
            val y = y1 + ((y0-y1) * lightness).roundToInt()
            // draw a circle around instead?
            GFX.drawRect(x, y-1, 1, 3, black)
            GFX.drawRect(x-1, y, 3, 1, black)
        }
    }

    val hueChooser = object: HSVBox(this, Vector3f(), Vector3f(1f, 0f, 0f), Vector3f(0f,0f, 0f), 0f, style, 1f, { hue, _ ->
        setHSL(hue, saturation, lightness, opacity, colorSpace)
    } ) {
        override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
            chooser.drawColorBox(colorSpace.hue0, du, dv, dh)
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
            if(newColorSpace != null && newColorSpace != colorSpace){
                val rgb = colorSpace.toRGB(Vector3f(hue, saturation, lightness))
                val newHSL = newColorSpace.fromRGB(rgb)
                setHSL(newHSL.x, newHSL.y, newHSL.z, opacity, newColorSpace)
            }
        }

    /*val isHSLuvBox = Checkbox(colorSpace == ColorSpace.HSLuv, 10, style)
        .setChangeListener { isHSLuv ->
            if(isHSLuv != this.colorSpace){
                // convert the color, so that the rgb value stays the same
                val hsl = Vector3f(hue, saturation, lightness)
                val rgb = if(this.colorSpace) ColorSpace.HSLuv.toRGB(hsl) else ColorSpace.HSV.toRGB(hsl)
                val newHSL = if(isHSLuv) ColorSpace.HSLuv.fromRGB(rgb) else ColorSpace.HSV.fromRGB(rgb)
                setHSL(newHSL.x, newHSL.y, newHSL.z, opacity, isHSLuv)
            }
        }
        .setTooltip("HSLuv instead of HSV")*/

    init {
        this += colorSpaceInput
        this += SpacePanel(0, 2, style)
        this += hslBox
        this += SpacePanel(0, 2, style)
        this += hueChooser
        // val hueBox = PanelListX(style)
        // hueBox += hueChooser.setWeight(1f)
        // hueBox += SpacePanel(2, 0, style)
        // hueBox += isHSLuvBox
        // this += hueBox
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

    fun drawColorBox(d0: Vector3f, du: Vector3f, dv: Vector3f, dh: Float){
        val shader = colorSpace.shader
        shader.use()
        shader.v2("pos", 0f, 0f)
        shader.v2("size", 1f, 1f)
        shader.v3("v0", d0.x + hue * dh, d0.y, d0.z)
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
    override fun onCopyRequested(x: Float, y: Float) = "${colorSpace.serializationName}(${hue.f3()},${saturation.f3()},${lightness.f3()},${opacity.f3()})"

    override fun onPaste(x: Float, y: Float, pasted: String) {
        // todo better parsing function returning a rgba-vector
        setARGB(parseColor(pasted) ?: return)
        // todo check for HSVuv(h,s,v,a), HSV(h,s,v,a), or #... or RGB(r,g,b,a) or [1,1,0,1]
    }


    override fun onMouseDown(x: Float, y: Float, button: Int) {
        super.onMouseDown(x, y, button)
        mouseIsDown = true
    }

    var mouseIsDown = false
    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        super.onMouseMoved(x, y, dx, dy)
        if(mouseIsDown){
            val size = (if(Input.isShiftDown) 4f else 20f) * (if(GFX.selectedTransform is Camera) -1f else 1f) / max(GFX.width,GFX.height)
            val dx0 = dx*size
            val dy0 = dy*size
            val delta = dx0-dy0
            val scaleFactor = 1.10f
            val scale = pow(scaleFactor, delta)
            if(Input.isControlDown){
                setHSL(hue, saturation, lightness * scale, opacity, colorSpace)
            } else {
                setHSL(hue, saturation, lightness, clamp(opacity + delta, 0f, 1f), colorSpace)
            }
        }
    }

    override fun onMouseUp(x: Float, y: Float, button: Int) {
        super.onMouseUp(x, y, button)
        mouseIsDown = false
    }

}