package me.anno.ui.editor.color.spaces

import me.anno.language.translation.NameDesc
import me.anno.ui.editor.color.ColorSpace
import me.anno.io.ResourceHelper.loadText
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Vectors.toVector3f
import org.hsluv.HSLuvColorSpace
import org.joml.Vector3f

object HSLuv : ColorSpace(
    NameDesc("HSLuv", "HSL/HSV with constant brightness", "colorSpace.hsluv"),
    lazy {
        loadText("shader/color/HSLuv.glsl") + "\n" +
                "vec3 spaceToRGB(vec3 hsl){\n" +
                "   return clamp(hsluvToRgb(hsl*vec3(360.0, 100.0, 100.0)), 0.0, 1.0);\n" +
                "}\n"
    }, Vector3f(0f, 1f, 0.5f)
) {
    override fun fromRGB(rgb: Vector3f, dst: Vector3f): Vector3f {
        val v3 = JomlPools.vec3d.borrow()
        return HSLuvColorSpace
            .rgbToHsluv(v3.set(rgb))
            .toVector3f(dst)
            .mul(1f / 360f, 0.01f, 0.01f)
    }

    override fun toRGB(input: Vector3f, dst: Vector3f): Vector3f {
        val v3 = JomlPools.vec3d.borrow()
        return HSLuvColorSpace
            .hsluvToRgb(v3.set(input.x * 360.0, input.y * 100.0, input.z * 100.0))
            .toVector3f(dst)
    }
}

/**
 * todo oklab
 * https://bottosson.github.io/posts/oklab/
 *
 * struct Lab {float L; float a; float b;};
struct RGB {float r; float g; float b;};

Lab linear_srgb_to_oklab(RGB c)
{
float l = 0.4122214708f * c.r + 0.5363325363f * c.g + 0.0514459929f * c.b;
float m = 0.2119034982f * c.r + 0.6806995451f * c.g + 0.1073969566f * c.b;
float s = 0.0883024619f * c.r + 0.2817188376f * c.g + 0.6299787005f * c.b;

float l_ = cbrtf(l);
float m_ = cbrtf(m);
float s_ = cbrtf(s);

return {
0.2104542553f*l_ + 0.7936177850f*m_ - 0.0040720468f*s_,
1.9779984951f*l_ - 2.4285922050f*m_ + 0.4505937099f*s_,
0.0259040371f*l_ + 0.7827717662f*m_ - 0.8086757660f*s_,
};
}

RGB oklab_to_linear_srgb(Lab c)
{
float l_ = c.L + 0.3963377774f * c.a + 0.2158037573f * c.b;
float m_ = c.L - 0.1055613458f * c.a - 0.0638541728f * c.b;
float s_ = c.L - 0.0894841775f * c.a - 1.2914855480f * c.b;

float l = l_*l_*l_;
float m = m_*m_*m_;
float s = s_*s_*s_;

return {
+4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
-1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
-0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s,
};
}
 *
 * */