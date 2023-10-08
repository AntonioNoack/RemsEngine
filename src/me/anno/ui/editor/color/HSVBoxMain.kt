package me.anno.ui.editor.color

import me.anno.gpu.drawing.DrawCurves.drawLine
import me.anno.gpu.drawing.DrawGradients.drawRectGradient
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.TextureLib
import me.anno.input.Key
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.length
import me.anno.ui.Style
import me.anno.ui.base.constraints.AspectRatioConstraint
import me.anno.ui.editor.color.ColorChooser.Companion.circleBarRatio
import me.anno.utils.Color.black
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.Color.withAlpha
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.*

class HSVBoxMain(chooser: ColorChooser, v0: Vector3f, du: Vector3f, dv: Vector3f, style: Style) :
    HSVBox(chooser, v0, du, dv, 1f, style, 5f, { x, y ->
        chooser.apply {
            when (visualisation) {
                ColorVisualisation.WHEEL -> {
                    val s2 = x * 2 - 1
                    val l2 = y * 2 - 1
                    if (isDownInRing) {
                        val hue = (atan2(l2, s2) * (0.5 / PI) + 0.5).toFloat()
                        setHSL(hue, this.saturation, this.lightness, opacity, colorSpace, true)
                    } else {
                        // "   vec2 uv2 = clamp((uv-0.5)*1.8+0.5, 0.0, 1.0);\n" +
                        var s3 = (x - 0.5f) * 1.8f
                        var l3 = (y - 0.5f) * 1.8f
                        val length = 2 * max(abs(s3), abs(l3))
                        if (length > 1f) {
                            s3 /= length
                            l3 /= length
                        }
                        s3 += 0.5f
                        l3 += 0.5f
                        if (s3 in 0f..1f && l3 in 0f..1f) {
                            setHSL(hue, s3, l3, opacity, colorSpace, true)
                        }
                    }
                }
                ColorVisualisation.CIRCLE -> {
                    val x2 = x * (1f + circleBarRatio)
                    if (isDownInRing) {// alias x2 < 1f
                        // the wheel
                        val s2 = x2 * 2 - 1
                        val l2 = y * 2 - 1
                        val hue = (atan2(l2, s2) * (0.5 / PI) + 0.5).toFloat()
                        val saturation = min(1f, length(s2, l2))
                        setHSL(hue, saturation, lightness, opacity, colorSpace, true)
                    } else {
                        // the lightness
                        val lightness = y
                        setHSL(hue, saturation, lightness, opacity, colorSpace, true)
                    }
                }
                ColorVisualisation.BOX -> {
                    setHSL(hue, x, y, opacity, colorSpace, true)
                }
            }
        }
    }) {

    override fun getVisualState() = Pair(super.getVisualState(), chooser.getVisualState())

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val size = min(w, h)
        minW = size
        minH = (size * chooser.visualisation.ratio).roundToInt()
    }

    fun drawCrossHair(x: Int, y: Int) {
        // draw a circle around instead?
        drawRect(x, y - 1, 1, 3, black)
        drawRect(x - 1, y, 3, 1, black)
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) {
            mouseDown(x, y)
        } else super.onKeyDown(x, y, key)
    }

   private fun mouseDown(x: Float, y: Float) {
        val rx = (x - this.x) / this.width
        val ry = (y - this.y) / this.height
        when (chooser.visualisation) {
            ColorVisualisation.BOX -> {
            }
            ColorVisualisation.WHEEL -> {
                val s2 = rx * 2 - 1
                val l2 = ry * 2 - 1
                val distToCircle = 0.787f - sqrt(s2 * s2 + l2 * l2)
                val distToBox = max(abs(s2), abs(l2)) - 0.55f
                chooser.isDownInRing = distToCircle < distToBox
            }
            ColorVisualisation.CIRCLE -> {
                chooser.isDownInRing = (rx * (1f + circleBarRatio) <= 1f)
            }
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
        chooser.drawColorBox(this, v0, du, dv, dh, true)
        // show the user, where he is
        fun drawSmallRect() {
            val size = width / 8
            drawColoredAlpha(x + 1, y + 1, size, size, chooser, 3f, 3f, false)
        }
        when (val style = chooser.visualisation) {
            ColorVisualisation.WHEEL -> {
                val cx = x + width / 2
                val cy = y + height / 2
                val dx = width.toFloat()
                val dy = height.toFloat()
                val x = (cx + (chooser.saturation - 0.5f) * dx / 1.8f).roundToInt()
                val y = (cy - (chooser.lightness - 0.5f) * dy / 1.8f).roundToInt()
                drawCrossHair(x, y)
                // draw hue line
                val angle = (chooser.hue - 0.5f) * TAUf
                val sin = sin(angle)
                val cos = cos(angle)
                val outerRadius = 0.489f * max(dx, dy)
                val innerRadius = 0.392f * max(dx, dy)
                val color = black.withAlpha(0.5f)
                drawLine(
                    cx + cos * innerRadius, cy - sin * innerRadius,
                    cx + cos * outerRadius, cy - sin * outerRadius,
                    0.75f, color, backgroundColor.withAlpha(0), false
                )
                if (!chooser.withAlpha) drawSmallRect()
            }
            ColorVisualisation.BOX -> {
                val x = this.x + (width * chooser.saturation).roundToInt()
                val y = this.y + height - (height * chooser.lightness).roundToInt()
                drawCrossHair(x, y)
            }
            ColorVisualisation.CIRCLE -> {
                // show the point and bar
                val w2 = width * style.ratio
                val cx = x + w2 / 2
                val cy = y + height / 2
                val dy = height.toFloat()
                val angle = ((chooser.hue - 0.5) * (2 * PI)).toFloat()
                val sin = sin(angle)
                val cos = cos(angle)
                val radius = w2 * chooser.saturation * 0.5f
                val x = (cx + cos * radius).roundToInt()
                val y = (cy - sin * radius).roundToInt()
                drawCrossHair(x, y)
                val w3 = width - w2
                // 0.515
                drawRect(
                    this.x + (w2 * 0.510f / 0.5f).toInt(),
                    this.y + (dy * (1f - chooser.lightness)).toInt(),
                    (w3 * 0.5f / 0.515f).toInt(),
                    1,
                    black
                )
                if (!chooser.withAlpha) drawSmallRect()
            }
        }
    }

    init {
        // enforce the aspect ratio
        this += AspectRatioConstraint { chooser.visualisation.ratio }
    }

    override val className: String get() = "HSVBoxMain"

    companion object {
        fun drawColoredAlpha(
            x: Int, y: Int, w: Int, h: Int, chooser: ColorChooser,
            sx: Float, sy: Float, withGradient: Boolean
        ) {
            if (withGradient) {
                drawRectGradient(x, y, w, h, chooser.backgroundColor.toVecRGBA(), chooser.rgba)
            } else {
                chooser.drawColorBox(
                    x, y, w, h,
                    Vector3f(chooser.hue, chooser.saturation, chooser.lightness), Vector3f(), Vector3f(), 0f,
                    ColorVisualisation.BOX
                )
            }
            TextureLib.colorShowTexture.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.REPEAT)
            if (chooser.opacity < 1f) {
                val color =
                    (chooser.backgroundColor and 0xffffff) or ((1f - chooser.opacity) * 255).roundToInt().shl(24)
                drawTexture(x, y, w, h, TextureLib.colorShowTexture, color, Vector4f(sx, sy, sx / 2, sy / 2))
            }
        }
    }
}