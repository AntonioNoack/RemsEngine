package me.anno.ui.editor.color.spaces

import me.anno.language.translation.NameDesc
import me.anno.ui.editor.color.ColorSpace
import me.anno.utils.types.Vectors.toSRGB
import me.anno.utils.types.Vectors.toLinear
import org.joml.Vector3f
import kotlin.math.cbrt

/**
 * https://bottosson.github.io/posts/oklab/
 *
 * this color space is meant exclusively for computations, like YUV
 * */
object Oklab : ColorSpace(
    NameDesc("Oklab", "OK Lab color space for image processing", "colorSpace.oklab"),
    lazy {
        "" +
                "float fromLinear(float c){" +
                "   return c <= 0.0031308f ? 12.92 * c : 1.055 * pow(c, 1.0 / 2.4) - 0.055;\n" +
                "}\n" +
                "vec3 spaceToRGB(vec3 lab) {\n" +
                "   float l = pow(dot(lab, vec3(1.0, +0.396337780, +0.21580376)), 3.0);\n" +
                "   float m = pow(dot(lab, vec3(1.0, -0.105561346, -0.06385417)), 3.0);\n" +
                "   float s = pow(dot(lab, vec3(1.0, -0.089484180, -1.29148550)), 3.0);\n" +
                "   vec3 lms = vec3(l,m,s);\n" +
                "   vec3 dst = vec3(\n" +
                "       fromLinear(dot(lms,vec3(+4.0767417, -3.3077116, +0.23096994))),\n" +
                "       fromLinear(dot(lms,vec3(-1.2684380, +2.6097574, -0.34131938))),\n" +
                "       fromLinear(dot(lms,vec3(-0.0041960864, -0.7034186, +1.70761470)))\n" +
                "   );\n" +
                "   return clamp(dst, 0.0, 1.0);\n" +
                "}\n"
    }, Vector3f(0f, 1f, 0.5f)
) {

    override fun fromRGB(rgb: Vector3f, dst: Vector3f): Vector3f {
        rgb.toLinear(dst)
        val l = cbrt(0.41222146f * dst.x + 0.53633255f * dst.y + 0.051445995f * dst.z)
        val m = cbrt(0.21190350f * dst.x + 0.68069950f * dst.y + 0.107396960f * dst.z)
        val s = cbrt(0.08830246f * dst.x + 0.28171885f * dst.y + 0.629978700f * dst.z)
        return dst.set(
            0.210454260f * l + 0.79361780f * m - 0.004072047f * s,
            1.977998500f * l - 2.42859220f * m + 0.450593700f * s,
            0.025904037f * l + 0.78277177f * m - 0.808675770f * s
        )// .mul(1f, 1.5f, 1.5f).add(0f, 0.5f, 0.5f)
    }

    override fun toRGB(input: Vector3f, dst: Vector3f): Vector3f {
        val ix = input.x
        val iy = input.y // (input.y - 0.5f) / 1.5f
        val iz = input.z // (input.z - 0.5f) / 1.5f
        val l1 = ix + 0.396337780f * iy + 0.21580376f * iz
        val m1 = ix - 0.105561346f * iy - 0.06385417f * iz
        val s1 = ix - 0.089484180f * iy - 1.29148550f * iz
        val l3 = l1 * l1 * l1
        val m3 = m1 * m1 * m1
        val s3 = s1 * s1 * s1
        dst.set(
            +4.0767417000f * l3 - 3.3077116f * m3 + 0.23096994f * s3,
            -1.2684380000f * l3 + 2.6097574f * m3 - 0.34131938f * s3,
            -0.0041960864f * l3 - 0.7034186f * m3 + 1.70761470f * s3
        )
        return dst.toSRGB(dst)
    }
}