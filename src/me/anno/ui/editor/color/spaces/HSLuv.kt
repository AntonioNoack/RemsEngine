package me.anno.ui.editor.color.spaces

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.language.translation.NameDesc
import me.anno.ui.editor.color.ColorSpace
import me.anno.utils.pooling.JomlPools
import org.hsluv.HSLuvColorSpace
import org.joml.Vector3f

object HSLuv : ColorSpace(
    NameDesc("HSLuv", "HSL/HSV with constant brightness", "colorSpace.hsluv"),
    lazy {
        getReference("res://shaders/color/HSLuv.glsl").readTextSync() + "\n" +
                "vec3 spaceToRGB(vec3 hsl){\n" +
                "   return clamp(hsluvToRgb(hsl*vec3(360.0, 100.0, 100.0)), 0.0, 1.0);\n" +
                "}\n"
    }, Vector3f(0f, 1f, 0.5f)
) {
    override fun fromRGB(rgb: Vector3f, dst: Vector3f): Vector3f {
        val v3 = JomlPools.vec3d.borrow()
        return dst.set(HSLuvColorSpace.rgbToHsluv(v3.set(rgb)))
            .mul(1f / 360f, 0.01f, 0.01f)
    }

    override fun toRGB(input: Vector3f, dst: Vector3f): Vector3f {
        val v3 = JomlPools.vec3d.borrow()
        return dst.set(HSLuvColorSpace.hsluvToRgb(v3.set(input.x * 360.0, input.y * 100.0, input.z * 100.0)))
    }
}
