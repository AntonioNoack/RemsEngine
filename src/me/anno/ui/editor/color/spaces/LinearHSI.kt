package me.anno.ui.editor.color.spaces

import me.anno.language.translation.NameDesc
import me.anno.ui.editor.color.ColorSpace
import me.anno.maths.Maths
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos

object LinearHSI : ColorSpace(
    NameDesc("HSI"), "HSI-Linear", lazy {
        "" +
                "vec3 hueToRGB(float h){" +
                "   vec3 hsv = vec3(h, 1.0, 1.0);\n" +
                "   float x = (1.0 - abs(mod(h*6.0, 2.0) - 1.0));\n" +
                "   vec3 rgb = h < 0.5 ? (\n" +
                "       h < ${1.0 / 6.0} ? vec3(1.0,x,0.0) : h < ${2.0 / 6.0} ? vec3(x,1.0,0.0) : vec3(0.0,1.0,x) \n" +
                "   ) : (\n" +
                "       h < ${4.0 / 6.0} ? vec3(0.0,x,1.0) : h < ${5.0 / 6.0} ? vec3(x,0.0,1.0) : vec3(1.0,0.0,x)\n" +
                "   );\n" +
                "   return rgb;\n" +
                "}" +
                "vec3 spaceToRGB(vec3 hsv){" +
                "   float h = hsv.x, s = hsv.y, v = hsv.z;\n" +
                // just interpolate between black, white, and the most vibrant color at that hue
                "   vec3 color = hueToRGB(h);\n" +
                "   vec3 center = mix(vec3(0.5), color, s);\n" +
                "   if(v > 0.5){\n" +
                "       return mix(center, vec3(1.0), 2*v-1);\n" +
                "   } else {\n" +
                "       return center*(2*v);\n" +
                "   }\n" +
                "}"
    }, Vector3f(0f, 0.69f, 0.63f)
) {

    // https://www.vocal.com/video/rgb-and-hsvhsihsl-color-space-conversion/

    override fun fromRGB(rgb: Vector3f, dst: Vector3f): Vector3f {
        val r = rgb.x
        val g = rgb.y
        val b = rgb.z
        val max = Maths.max(r, g, b)
        val min = Maths.min(r, g, b)
        val delta = max - min
        var h = when (max) {
            r -> (g - b) / delta
            g -> (b - r) / delta + 2f
            else -> (r - g) / delta + 4f
        } / 6f
        if (h < 0f) h += 1f
        val i = (r + g + b) / 3
        val s = if (i == 0f) 0f else 1 - min / i
        return dst.set(h, s, max)
    }

    override fun toRGB(input: Vector3f, dst: Vector3f): Vector3f {
        val h = input.x
        val s = input.y
        val i = input.z
        val h6 = h * 6
        val r: Float
        val g: Float
        val b: Float
        val hAngle = h * PI.toFloat() * 2f
        val deg60 = PI.toFloat() / 3f
        when {
            h6 <= 2f -> {
                b = i * (1 - s)
                r = i * (1 + s * cos(hAngle) / (cos(deg60 - h)))
                g = 3 * i - b - r
                // LOGGER.info("1 $b $r $g")
            }
            h6 <= 4f -> {
                r = i * (1 - s)
                g = i * (1 + s * cos(2f * deg60 - hAngle) / cos(3f * deg60 - hAngle))
                b = 3 * i - r - g
                // LOGGER.info("2 $r $g $b")
            }
            else -> {
                g = i * (1 - s)
                b = i * (1 + s * cos(4f * deg60 - hAngle) / cos(5f * deg60 - hAngle))
                r = 3 * i - b - g
                // LOGGER.info("3 $g $b $r")
            }
        }
        return dst.set(r, g, b)
    }

}