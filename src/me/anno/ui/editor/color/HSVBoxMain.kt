package me.anno.ui.editor.color

import me.anno.config.DefaultStyle
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.ui.base.Constraint
import me.anno.ui.base.Panel
import me.anno.ui.editor.color.ColorChooser.Companion.CircleBarRatio
import me.anno.ui.style.Style
import me.anno.utils.length
import org.joml.Vector3f
import kotlin.math.*

class HSVBoxMain(chooser: ColorChooser, v0: Vector3f, du: Vector3f, dv: Vector3f, style: Style):
    HSVBox(chooser, v0, du, dv, 1f, style, 5f, { x, y ->
        chooser.apply {
            when(spaceStyle){
                ColorVisualisation.WHEEL -> {
                    val s2 = x*2-1
                    val l2 = y*2-1
                    val dst = s2*s2+l2*l2
                    if(dst < 1.0){
                        if(isDownInRing){
                            val hue = (atan2(l2, s2) * (0.5/Math.PI) + 0.5).toFloat()
                            setHSL(hue, this.saturation, this.lightness, opacity, colorSpace)
                        } else {
                            // "   vec2 uv2 = clamp((uv-0.5)*1.8+0.5, 0.0, 1.0);\n" +
                            var s3 = (x-0.5f)*1.8f
                            var l3 = (y-0.5f)*1.8f
                            val length = 2 * max(abs(s3), abs(l3))
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
                }
                ColorVisualisation.CIRCLE -> {
                    val x2 = x * (1f + CircleBarRatio)
                    if(isDownInRing){// alias x2 < 1f
                        // the wheel
                        val s2 = x2*2-1
                        val l2 = y*2-1
                        val hue = (atan2(l2, s2) * (0.5/Math.PI) + 0.5).toFloat()
                        val saturation = min(1f, length(s2, l2))
                        setHSL(hue, saturation, lightness, opacity, colorSpace)
                    } else {
                        // the lightness
                        val lightness = y
                        setHSL(hue, saturation, lightness, opacity, colorSpace)
                    }
                }
                ColorVisualisation.BOX -> {
                    setHSL(hue, x, y, opacity, colorSpace)
                }
            }
        }
    }){

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val size = min(w, h)
        minW = size
        minH = (size * chooser.spaceStyle.ratio).roundToInt()
    }

    fun drawCrossHair(x: Int, y: Int){
        // draw a circle around instead?
        GFX.drawRect(x, y-1, 1, 3, DefaultStyle.black)
        GFX.drawRect(x-1, y, 3, 1, DefaultStyle.black)
    }

    override fun onMouseDown(x: Float, y: Float, button: Int) {
        val rx = (x - this.x)/this.w
        val ry = (y - this.y)/this.h
        when(chooser.spaceStyle){
            ColorVisualisation.BOX -> {}
            ColorVisualisation.WHEEL -> {
                val s2 = rx*2-1
                val l2 = ry*2-1
                val dst = s2*s2+l2*l2
                chooser.isDownInRing = dst >= 0.62
            }
            ColorVisualisation.CIRCLE -> {
                chooser.isDownInRing = (rx * (1f + CircleBarRatio) <= 1f)
            }
        }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        chooser.drawColorBox(this, v0, du, dv, dh, true)
        // show the user, where he is
        when(val style = chooser.spaceStyle){
            ColorVisualisation.WHEEL -> {
                // "   vec2 uv2 = clamp((uv-0.5)*1.8+0.5, 0.0, 1.0);\n" +
                val cx = x+w/2
                val cy = y+h/2
                val dx = w.toFloat()
                val dy = h.toFloat()
                val x = (cx + (chooser.saturation-0.5f)*dx/1.8f).roundToInt()
                val y = (cy - (chooser.lightness-0.5f)*dy/1.8f).roundToInt()
                drawCrossHair(x, y)
                // draw hue line
                // hue = (atan2(l2, s2) * (0.5/ Math.PI) + 0.5).toFloat()
                val angle = ((chooser.hue - 0.5) * (2 * Math.PI)).toFloat()
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
            }
            ColorVisualisation.BOX -> {
                val x = this.x + (w * chooser.saturation).roundToInt()
                val y = this.y + h - (h * chooser.lightness).roundToInt()
                drawCrossHair(x, y)
            }
            ColorVisualisation.CIRCLE -> {
                // show the point and bar
                val w2 = w*style.ratio
                val cx = x+w2/2
                val cy = y+h/2
                val dy = h.toFloat()
                val angle = ((chooser.hue - 0.5) * (2 * Math.PI)).toFloat()
                val sin = sin(angle)
                val cos = cos(angle)
                val radius = w2 * chooser.saturation * 0.5f
                val x = (cx + cos * radius).roundToInt()
                val y = (cy - sin * radius).roundToInt()
                drawCrossHair(x, y)
                val w3 = w - w2
                GFX.drawRect(this.x + w2.toInt() + 3, this.y + (dy * (1f - chooser.lightness)).toInt(), w3.toInt()-3, 1, black)
            }
        }
    }

    init {
        // enforce the aspect ratio
        this += object: Constraint(25){
            override fun apply(panel: Panel) {
                val targetAspectRatio = chooser.spaceStyle.ratio
                if(panel.w * targetAspectRatio > panel.h){
                    // zu breit -> weniger breit
                    panel.w = min(panel.w, (panel.h / targetAspectRatio).roundToInt())
                } else {
                    // zu hoch -> weniger hoch
                    panel.h = min(panel.h, (panel.w * targetAspectRatio).roundToInt())
                }
            }
        }
    }

}