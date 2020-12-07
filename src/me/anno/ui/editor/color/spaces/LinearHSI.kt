package me.anno.ui.editor.color.spaces

import me.anno.ui.editor.color.ColorSpace
import me.anno.ui.editor.color.HSVColorSpace
import me.anno.utils.GradientDescent.gradientDescent
import me.anno.utils.Maths.mix
import me.anno.utils.Vectors.times
import org.joml.Vector3f

object LinearHSI : ColorSpace(
    "HSI", "HSI-Linear", "" +
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
            "}", Vector3f(0f, 0.69f, 0.63f)
) {

    // not completely correct yet;
    // use this as a starting point for gradient descent,
    // if we can't figure it out?
    override fun fromRGB(rgb: Vector3f): Vector3f {
        val hsv = HSVColorSpace.rgbToHSV(rgb.x, rgb.y, rgb.z)
        // we were too lazy to find the correct solution 😅, so we just use gradient descent to find it
        val solution = gradientDescent(floatArrayOf(hsv.x, hsv.y, hsv.z), 0.1f, 1e-6f, 250) {
            val currentRGB = toRGB(Vector3f(it[0], it[1], it[2]))
            val dr = currentRGB.x - rgb.x
            val dg = currentRGB.y - rgb.y
            val db = currentRGB.z - rgb.z
            dr * dr + dg * dg + db * db
        }
        return Vector3f(solution[0], solution[1], solution[2])
    }

    override fun toRGB(input: Vector3f): Vector3f {
        val h = input.x
        val s = input.y
        val v = input.z
        val color = HSVColorSpace.hsvToRGB(h, 1f, 1f)
        val center = mix(Vector3f(0.5f), color, s)
        return if (v > 0.5) {
            mix(center, Vector3f(1f), 2 * v - 1)
        } else {
            center * (2 * v)
        }
    }

}