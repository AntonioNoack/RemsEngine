package me.anno.ui.editor.color

import me.anno.gpu.GFX
import me.anno.gpu.Shader
import me.anno.utils.toVec3
import org.hsluv.HSLuvColorConverter
import org.joml.Vector3f

// could be used to replace the two color spaces with more
abstract class ColorSpace(
    val serializationName: String, val name: String,
    // display
    val shader: Shader,
    val hue0: Vector3f){

    constructor(name: String, shader: Shader, hue0: Vector3f): this(name, name, shader, hue0)

    init {
        values[name] = this
        values[name.toLowerCase()] = this
    }

    abstract fun fromRGB(rgb: Vector3f): Vector3f
    abstract fun toRGB(input: Vector3f): Vector3f

    companion object {
        val values = HashMap<String, ColorSpace>()
        operator fun get(name: String) = values[name] ?: values[name.toLowerCase()]
        val HSLuv = object: ColorSpace("HSLuv", GFX.hsluvShader, Vector3f(0f, 1f, 0.5f)){
            override fun fromRGB(rgb: Vector3f): Vector3f {
                return HSLuvColorConverter.rgbToHsluv(doubleArrayOf(
                    rgb.x.toDouble(), rgb.y.toDouble(), rgb.z.toDouble()
                )).toVec3().mul(1/360f, 1/100f, 1/100f)
            }
            override fun toRGB(input: Vector3f): Vector3f {
                return HSLuvColorConverter.hsluvToRgb(doubleArrayOf(
                    input.x * 360.0, input.y * 100.0, input.z * 100.0
                )).toVec3()
            }
        }
        val HSV = object: ColorSpace("HSV", GFX.hslShader, Vector3f(0f, 0.7f, 1f)){
            override fun fromRGB(rgb: Vector3f): Vector3f {
                return HSVColorConverter.rgbToHSV(rgb.x, rgb.y, rgb.z)
            }
            override fun toRGB(input: Vector3f): Vector3f {
                return HSVColorConverter.hsvToRGB(input.x, input.y, input.z)
            }
        }
    }
}