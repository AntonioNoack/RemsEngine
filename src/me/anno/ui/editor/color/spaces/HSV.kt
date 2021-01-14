package me.anno.ui.editor.color.spaces

import me.anno.language.translation.NameDesc
import me.anno.ui.editor.color.ColorSpace
import me.anno.ui.editor.color.HSVColorSpace
import org.joml.Vector3f

object HSV : ColorSpace(
    NameDesc("HSV"),
    HSVColorSpace.GLSL, Vector3f(0f, 0.7f, 1f)) {
    override fun fromRGB(rgb: Vector3f): Vector3f {
        return HSVColorSpace.rgbToHSV(rgb.x, rgb.y, rgb.z)
    }

    override fun toRGB(input: Vector3f): Vector3f {
        return HSVColorSpace.hsvToRGB(input.x, input.y, input.z)
    }
}