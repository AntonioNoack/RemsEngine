package me.anno.ui.editor.color.spaces

import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.TAUf
import me.anno.maths.MinMax.min
import me.anno.ui.editor.color.ColorSpace
import org.joml.Vector3f
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt

object HSI : ColorSpace(
    NameDesc("HSI"), "HSI-Linear", lazy {
        "" +
                "vec3 spaceToRGB(vec3 hsv){\n" +
                // not all hsi values are valid rgb values...
                "   float h = hsv.x, s = hsv.y, i = hsv.z;\n" +
                "   float h6 = h * 6.0;\n" +
                "   float ha = mod(h6, 2.0) * ${TAUf / 6f};\n" +
                "   float x = cos(ha) / cos(${TAUf / 6f} - ha);\n" +
                "   float b0 = i - i * s;\n" +
                "   float g0 = i + i * s * (1.0 - x);\n" +
                "   float r0 = i * (1.0 + s * x);\n" +
                "   return h6 <= 2.0 ? vec3(r0,g0,b0) :\n" +
                "          h6 <= 4.0 ? vec3(b0,r0,g0) :\n" +
                "                      vec3(g0,b0,r0);\n" +
                "}"
    }, Vector3f(0f, 0.69f, 0.63f)
) {

    // https://www.had2know.org/technology/hsi-rgb-color-converter-equations.html

    override fun fromRGB(rgb: Vector3f, dst: Vector3f): Vector3f {
        val r = rgb.x
        val g = rgb.y
        val b = rgb.z
        val i = (r + g + b) / 3f
        val top = r - 0.5f * (g + b)
        val bot = r * r + g * g + b * b - r * b - r * g - g * b
        val theta0 = top / sqrt(max(bot, 1e-38f))
        val theta = acos(theta0) / TAUf
        val h = if (g > b) theta else 1f - theta
        val s = 1f - min(min(r, g), b) / i
        return dst.set(h, s, i)
    }

    override fun toRGB(input: Vector3f, dst: Vector3f): Vector3f {
        val h = input.x
        val s = input.y
        val i = input.z
        val h6 = h * 6f
        val ha = (h6 % 2f) / 6f * TAUf
        val deg60 = TAUf / 6f
        val x = cos(ha) / cos(deg60 - ha)
        val b0 = i - i * s
        val g0 = i + i * s * (1 - x)
        val r0 = i * (1 + s * x)
        return when {
            h6 <= 2f -> dst.set(r0, g0, b0)
            h6 <= 4f -> dst.set(b0, r0, g0)
            else -> dst.set(g0, b0, r0)
        }
    }
}