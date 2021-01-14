package me.anno.ui.editor.color.spaces

import me.anno.language.translation.NameDesc
import me.anno.ui.editor.color.ColorSpace
import me.anno.utils.ResourceHelper.loadText
import me.anno.utils.Vectors.toVec3
import org.hsluv.HSLuvColorSpace
import org.joml.Vector3f

object HSLuv : ColorSpace(
    NameDesc("HSLuv","HSL/HSV with constant brightness","colorSpace.hsluv"),
    loadText("me/anno/ui/editor/color/spaces/HSLuv.glsl") + "\n" +
            "vec3 spaceToRGB(vec3 hsl){\n" +
            "   return clamp(hsluvToRgb(hsl*vec3(360.0, 100.0, 100.0)), 0.0, 1.0);\n" +
            "}\n", Vector3f(0f, 1f, 0.5f)
) {
    override fun fromRGB(rgb: Vector3f): Vector3f {
        return HSLuvColorSpace.rgbToHsluv(
            doubleArrayOf(
                rgb.x.toDouble(), rgb.y.toDouble(), rgb.z.toDouble()
            )
        ).toVec3().mul(1f / 360f, 0.01f, 0.01f)
    }

    override fun toRGB(input: Vector3f): Vector3f {
        return HSLuvColorSpace.hsluvToRgb(
            doubleArrayOf(input.x * 360.0, input.y * 100.0, input.z * 100.0)
        ).toVec3()
    }
}