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
import me.anno.ui.input.components.Checkbox
import me.anno.ui.style.Style
import me.anno.utils.f3
import me.anno.utils.length
import org.hsluv.HSLuvColorSpace
import org.joml.Vector3f
import kotlin.math.*

class ColorChooser(style: Style, withAlpha: Boolean): PanelListY(style){

    // color section
    // bottom section:
    // hue
    // check box for hsl/hsluv
    // field(?) to enter rgb codes

    var isRing = true

    var colorSpace = ColorSpace[DefaultConfig["color.defaultColorSpace", "HSLuv"]] ?: ColorSpace.HSLuv

    var isDownInRing = false
    val hslBox = object: HSVBox(this, Vector3f(), Vector3f(0f, 1f, 0f), Vector3f(0f, 0f, 1f), 1f, style, 5f, { x, y ->
        if(isRing){
            val s2 = x*2-1
            val l2 = y*2-1
            val dst = s2*s2+l2*l2
            if(dst < 1.0){
                if(isDownInRing){
                    val hue = (atan2(l2, s2) * (0.5/ Math.PI) + 0.5).toFloat()
                    setHSL(hue, this.saturation, this.lightness, opacity, colorSpace)
                } else {
                    // "   vec2 uv2 = clamp((uv-0.5)*1.8+0.5, 0.0, 1.0);\n" +
                    var s3 = (x-0.5f)*1.8f
                    var l3 = (y-0.5f)*1.8f
                    val length = 2*max(abs(s3), abs(l3))
                    if(length > 1f){
                        s3 /= length
                        l3 /= length
                    }
                    s3 += 0.5f
                    l3 += 0.5f
                    if(s3 in 0f .. 1f && l3 in 0f .. 1f){
                        setHSL(hue, s3, l3, opacity, colorSpace)
                    }
                }
            }
        } else {
            setHSL(hue, x, y, opacity, colorSpace)
        }
    }){
        val oldMinH = minH
        override fun calculateSize(w: Int, h: Int) {
            super.calculateSize(w, h)
            if(isRing){
                val size = min(w, h)
                minW = size
                minH = size
            } else {
                minW = 10
                minH = oldMinH
            }
        }
        fun drawCrossHair(x: Int, y: Int){
            // draw a circle around instead?
            GFX.drawRect(x, y-1, 1, 3, black)
            GFX.drawRect(x-1, y, 3, 1, black)
        }
        override fun onMouseDown(x: Float, y: Float, button: Int) {
            val rx = (x - this.x)/this.w
            val ry = (y - this.y)/this.h
            val s2 = rx*2-1
            val l2 = ry*2-1
            val dst = s2*s2+l2*l2
            isDownInRing = dst >= 0.62
        }
        override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.draw(x0, y0, x1, y1)
            if(isRing){
                // "   vec2 uv2 = clamp((uv-0.5)*1.8+0.5, 0.0, 1.0);\n" +
                val cx = x+w/2
                val cy = y+h/2
                val dx = w.toFloat()
                val dy = h.toFloat()
                val x = (cx + (saturation-0.5f)*dx/1.8f).roundToInt()
                val y = (cy - (lightness-0.5f)*dy/1.8f).roundToInt()
                drawCrossHair(x, y)
                // draw hue line
                // hue = (atan2(l2, s2) * (0.5/ Math.PI) + 0.5).toFloat()
                val angle = ((hue - 0.5) * (2 * Math.PI)).toFloat()
                val sin = sin(angle)
                val cos = cos(angle)
                val outerRadius = 0.50f * max(dx, dy)
                val innerRadius = outerRadius * 0.77f
                var i = innerRadius
                while(i < outerRadius){
                    val x2 = (cx + cos * i).roundToInt()
                    val y2 = (cy - sin * i).roundToInt()
                    GFX.drawRect(x2, y2, 1, 1, 0x33000000)
                    i += 0.3f
                }
            } else {
                val x = this.x + (w * saturation).roundToInt()
                val y = this.y + h - (h * lightness).roundToInt()
                drawCrossHair(x, y)
            }
        }
    }

    val hueChooserSpace = SpacePanel(0, 2, style)
    val hueChooser = object: HSVBox(this, Vector3f(), Vector3f(1f, 0f, 0f), Vector3f(0f,0f, 0f), 0f, style, 1f, { hue, _ ->
        setHSL(hue, saturation, lightness, opacity, colorSpace)
    } ) {
        override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
            chooser.drawColorBox(this, colorSpace.hue0, du, dv, dh)
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
        .setTooltip("Ring/Box")

    val isRingCheckbox = Checkbox(isRing, 10, style)
        .setChangeListener { isRing = it }

    init {
        val spaceBox = PanelListX(style)
        this += spaceBox
        spaceBox += colorSpaceInput.setWeight(1f)
        spaceBox += isRingCheckbox
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
        val helperVisibility = if(isRing) Visibility.GONE else Visibility.VISIBLE
        hueChooser.visibility = helperVisibility
        hueChooserSpace.visibility = helperVisibility
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

    fun drawColorBox(element: Panel, d0: Vector3f, du: Vector3f, dv: Vector3f, dh: Float){
        val isRing = isRing && dh > 0f
        val shader = colorSpace.getShader(isRing)
        shader.use()
        GFX.posSize(shader, element.x, element.y + element.h, element.w, - element.h)
        if(isRing){
            shader.v3("v0", d0.x + hue * dh, d0.y, d0.z)
            shader.v3("du", du)
            shader.v3("dv", dv)
            val hue0 = colorSpace.hue0
            shader.v2("v1", hue0.y, hue0.z)
            GFX.flat01.draw(shader)
        } else {
            shader.v3("v0", d0.x + hue * dh, d0.y, d0.z)
            shader.v3("du", du)
            shader.v3("dv", dv)
            GFX.flat01.draw(shader)
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

    override fun onPaste(x: Float, y: Float, pasted: String) {
        // todo better parsing function returning a rgba-vector
        setARGB(parseColor(pasted) ?: return)
        // todo check for HSVuv(h,s,v,a), HSV(h,s,v,a), or #... or RGB(r,g,b,a) or [1,1,0,1]
    }

}