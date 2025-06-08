package me.anno.ui.editor.color

import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.DrawCurves.drawLine
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.GFXx2D.noTiling
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.GOLDEN_RATIOf
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.length
import me.anno.ui.editor.color.ColorChooser.Companion.circleBarRatio
import me.anno.ui.editor.color.HSVBoxMain.Companion.drawColoredAlpha
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class ColorVisualisation(val id: Int, val nameDesc: NameDesc, val ratio: Float, val needsHueChooser: Boolean) {
    WHEEL(0, NameDesc("Wheel", "", "ui.input.color.vis.wheel"), 1f, false),
    CIRCLE(1, NameDesc("Circle", "", "ui.input.color.vis.circle"), 1f / (1f + circleBarRatio), false),
    BOX(2, NameDesc("Box", "", "ui.input.color.vis.box"), 1f / GOLDEN_RATIOf, true);

    fun setColorByClick(rx: Float, ry: Float, self: ColorChooser) {
        when (this) {
            WHEEL -> {
                if (self.isDownInRing) {
                    val s2 = rx * 2f - 1f
                    val l2 = ry * 2f - 1f
                    val hue = (atan2(-l2, s2) * (0.5 / PI) + 0.5).toFloat()
                    self.setHSL(hue, self.saturation, self.lightness, self.opacity, self.colorSpace, 1, true)
                } else {
                    // "   vec2 uv2 = clamp((uv-0.5)*1.8+0.5, 0.0, 1.0);\n" +
                    var s3 = (rx - 0.5f) * 1.8f
                    var l3 = (ry - 0.5f) * 1.8f
                    val length = 2f * max(abs(s3), abs(l3))
                    if (length > 1f) { // normalize / clamp to edge of ring
                        s3 /= length
                        l3 /= length
                    }
                    s3 += 0.5f
                    l3 += 0.5f
                    if (s3 in 0f..1f && l3 in 0f..1f) {
                        self.setHSL(self.hue, s3, 1f - l3, self.opacity, self.colorSpace, 6, true)
                    }
                }
            }
            CIRCLE -> {
                val x2 = rx * (1f + circleBarRatio)
                if (self.isDownInRing) {// alias x2 < 1f
                    // the wheel
                    val s2 = x2 * 2f - 1f
                    val l2 = ry * 2f - 1f
                    val hue = (atan2(-l2, s2) * (0.5 / PI) + 0.5).toFloat()
                    val saturation = min(1f, length(s2, l2))
                    self.setHSL(hue, saturation, self.lightness, self.opacity, self.colorSpace, 3, true)
                } else {
                    // the lightness
                    self.setHSL(self.hue, self.saturation, 1f - ry, self.opacity, self.colorSpace, 4, true)
                }
            }
            BOX -> {
                self.setHSL(self.hue, rx, 1f - ry, self.opacity, self.colorSpace, 6, true)
            }
        }
    }

    fun isDownInRing(rx: Float, ry: Float): Boolean {
        return when (this) {
            BOX -> false
            WHEEL -> {
                val s2 = rx * 2f - 1f
                val l2 = ry * 2f - 1f
                val distToCircle = 0.787f - hypot(s2, l2)
                val distToBox = max(abs(s2), abs(l2)) - 0.55f
                distToCircle < distToBox
            }
            CIRCLE -> {
                (rx * (1f + circleBarRatio) <= 1f)
            }
        }
    }

    fun drawColorBox(
        x: Int, y: Int, width: Int, height: Int,
        d0: Vector3f, du: Vector3f, dv: Vector3f, dh: Float,
        self: ColorChooser
    ) {
        val shader = self.colorSpace.getShader(this)
        shader.use()
        posSize(shader, x, y, width, height, true)
        noTiling(shader)
        val sharpness = min(width, height) * 0.25f + 1f
        when (this) {
            WHEEL -> {
                shader.v3f("v0", d0.x + self.hue * dh, d0.y, d0.z)
                shader.v3f("du", du)
                shader.v3f("dv", dv)
                val hue0 = self.colorSpace.hue0
                shader.v2f("ringSL", hue0.y, hue0.z)
                shader.v1f("sharpness", sharpness)
                flat01.draw(shader)
            }
            CIRCLE -> {
                shader.v1f("lightness", self.lightness)
                shader.v1f("sharpness", sharpness)
                flat01.draw(shader)
            }
            BOX -> {
                shader.v3f("v0", d0.x + self.hue * dh, d0.y, d0.z)
                shader.v3f("du", du)
                shader.v3f("dv", dv)
                // shader.v1("sharpness", sharpness)
                flat01.draw(shader)
            }
        }
    }

    fun drawColorBoxDecoration(
        x: Int, y: Int, width: Int, height: Int,
        backgroundColor: Int, chooser: ColorChooser,
    ) {
        // show the user, where he is
        fun drawSmallRect() {
            val size = width / 8
            drawColoredAlpha(x + 1, y + 1, size, size, chooser, 3f, 3f, false)
        }

        fun drawCrossHair(x: Int, y: Int) {
            // draw a circle around instead?
            drawRect(x, y - 1, 1, 3, black)
            drawRect(x - 1, y, 3, 1, black)
        }
        when (val style = chooser.visualisation) {
            WHEEL -> {
                val cx = x + width / 2
                val cy = y + height / 2
                val dx = width.toFloat()
                val dy = height.toFloat()
                val x = (cx + (chooser.saturation - 0.5f) * dx / 1.8f).roundToIntOr()
                val y = (cy - (chooser.lightness - 0.5f) * dy / 1.8f).roundToIntOr()
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
            BOX -> {
                val x = x + (width * chooser.saturation).roundToIntOr()
                val y = y + height - (height * chooser.lightness).roundToIntOr()
                drawCrossHair(x, y)
            }
            CIRCLE -> {
                // show the point and bar
                val w2 = width * style.ratio
                val cx = x + w2 / 2
                val cy = y + height / 2
                val dy = height.toFloat()
                val angle = ((chooser.hue - 0.5) * (2 * PI)).toFloat()
                val sin = sin(angle)
                val cos = cos(angle)
                val radius = w2 * chooser.saturation * 0.5f
                val x2 = (cx + cos * radius).roundToIntOr()
                val y2 = (cy - sin * radius).roundToIntOr()
                drawCrossHair(x2, y2)
                val w3 = width - w2
                // 0.515
                drawRect(
                    x + (w2 * 0.510f / 0.5f).toInt(),
                    y + (dy * (1f - chooser.lightness)).toInt(),
                    (w3 * 0.5f / 0.515f).toInt(),
                    1, black
                )
                if (!chooser.withAlpha) drawSmallRect()
            }
        }
    }

    fun getFragmentShader(): String {
        return when (this) {
            WHEEL -> {
                "void main(){\n" +
                        "   vec2 nuv = uv*2.0-1.0;\n" + // normalized uv
                        "   float dst = dot(nuv,nuv);\n" +
                        "   float radius = sqrt(dst);\n" +
                        "   float hue = atan(nuv.y, nuv.x) * ${(0.5 / PI)} + 0.5;\n" +
                        "   vec3 hsl = vec3(hue, ringSL);\n" +
                        "   float alpha = radius > 0.975 ? 1.0 + (0.975-radius)*sharpness : 1.0;\n" +
                        "   float isSquare = clamp((0.787-radius)*sharpness, 0.0, 1.0);\n" +
                        "   vec2 uv2 = clamp((uv-0.5)*1.8+0.5, 0.0, 1.0);\n" +
                        "   float dst2 = max(abs(uv2.x-0.5), abs(uv2.y-0.5));\n" +
                        "   alpha *= mix(1.0, clamp((0.5-dst2)*sharpness, 0.0, 1.0), isSquare);\n" +
                        "   if(alpha <= 0.0) discard;\n" +
                        "   vec3 ringColor = spaceToRGB(hsl);\n" +
                        "   vec3 squareColor = spaceToRGB(v0 + du * uv2.x + dv * uv2.y);\n" +
                        "   vec3 rgb = mix(ringColor, squareColor, isSquare);\n" +
                        "   gl_FragColor = vec4(rgb, alpha);\n" +
                        "}"
            }
            CIRCLE -> {
                "void main(){\n" +
                        "   vec3 rgb;\n" +
                        "   float alpha = 1.0;\n" +
                        "   vec2 nuv = vec2(uv.x * ${1f + circleBarRatio}, uv.y) - 0.5;\n" + // normalized + bar
                        "   if(nuv.x > 0.5){\n" +
                        "       // a simple brightness bar \n" +
                        "       rgb = vec3(uv.y);\n" +
                        "       alpha = clamp(min(" +
                        "           min(" +
                        "               nuv.x-0.515," +
                        "               ${0.5f + circleBarRatio}-nuv.x" +
                        "           ), min(" +
                        "               nuv.y+0.5," +
                        "               0.5-nuv.y" +
                        "           )" +
                        "       ) * sharpness, 0.0, 1.0);\n" +
                        "   } else {\n" +
                        "       // a circle \n" +
                        "       float radius = 2.0 * length(nuv);\n" +
                        "       float dst = radius*radius;\n" +
                        "       float hue = atan(nuv.y, nuv.x) * ${0.5 / PI} + 0.5;\n" +
                        "       alpha = radius > 0.975 ? 1.0 + (0.975-radius)*sharpness : 1.0;\n" +
                        "       vec3 hsl = vec3(hue, radius, lightness);\n" +
                        "       rgb = spaceToRGB(hsl);\n" +
                        "   }\n" +
                        "   gl_FragColor = vec4(rgb, alpha);\n" +
                        "}"
            }
            BOX -> {
                "void main(){\n" +
                        "   vec3 hsl = v0 + du * uv.x + dv * uv.y;\n" +
                        "   vec3 rgb = spaceToRGB(hsl);\n" +
                        "   gl_FragColor = vec4(rgb, 1.0);\n" +
                        "}"
            }
        }
    }
}