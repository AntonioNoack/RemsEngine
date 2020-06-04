package me.anno.ui.editor.color

import me.anno.gpu.GFX
import me.anno.gpu.Shader
import me.anno.utils.toVec3
import org.hsluv.HSLuvColorSpace
import org.joml.Vector3f

// could be used to replace the two color spaces with more
abstract class ColorSpace(
    val name: String,
    val serializationName: String,
    // display
    val glsl: String,
    // val shader: Shader,
    val hue0: Vector3f){

    constructor(name: String, glsl: String, hue0: Vector3f): this(name, name, glsl, hue0)

    init {
        values[name] = this
        values[name.toLowerCase()] = this
    }

    var ringShader: Shader? = null
    var boxShader: Shader? = null

    fun getShader(ring: Boolean): Shader {
        val oldShader = if(ring) ringShader else boxShader
        if(oldShader != null) return oldShader
        val vertexShader = "" +
                "in vec2 attr0;\n" +
                "uniform vec2 pos, size;\n" +
                "void main(){\n" +
                "   gl_Position = vec4((pos + attr0 * size)*2.-1., 0.0, 1.0);\n" +
                "   uv = attr0;\n" +
                "}"
        val varyingShader = "varying vec2 uv;\n"
        val fragmentShader = if(ring){
            "" +
                    "uniform vec2 v1;\n" +
                    "uniform vec3 v0, du, dv;\n" +
                    glsl +
                    "void main(){\n" +
                    "   vec2 nuv = uv*2.0-1.0;\n" +
                    "   float dst = dot(nuv,nuv);\n" +
                    "   float hue = atan(nuv.y, nuv.x) * ${(0.5/Math.PI)} + 0.5;\n" +
                    "   vec3 hsl = vec3(hue, v1);\n" +
                    "   float alpha = dst > 0.95 ? 1.0 + (0.95-dst)*15.0 : 1.0;\n" +
                    "   float isSquare = clamp((0.62-dst)*20.0, 0.0, 1.0);\n" +
                    "   vec2 uv2 = clamp((uv-0.5)*1.8+0.5, 0.0, 1.0);\n" +
                    "   float dst2 = max(abs(uv2.x-0.5), abs(uv2.y-0.5));\n" +
                    "   alpha *= mix(1.0, clamp((0.5-dst2)*50.0, 0.0, 1.0), isSquare);\n" +
                    "   if(alpha <= 0.0) discard;\n" +
                    "   vec3 squareColor = spaceToRGB(v0 + du * uv2.x + dv * uv2.y);\n" +
                    "   vec3 rgb = mix(spaceToRGB(hsl), squareColor, isSquare);\n" +
                    "   gl_FragColor = vec4(rgb, alpha);\n" +
                    "}"
        } else {
            "" +
                    "uniform vec3 v0, du, dv;\n" +
                    glsl +
                    "void main(){\n" +
                    "   vec3 hsl = v0 + du * uv.x + dv * uv.y;\n" +
                    "   vec3 rgb = spaceToRGB(hsl);\n" +
                    "   gl_FragColor = vec4(rgb, 1.0);\n" +
                    "}"
        }
        val newShader = Shader(vertexShader, varyingShader, fragmentShader, disableShorts = true)
        if(ring){
            ringShader = newShader
        } else {
            boxShader = newShader
        }
        return newShader
    }

    abstract fun fromRGB(rgb: Vector3f): Vector3f
    abstract fun toRGB(input: Vector3f): Vector3f

    companion object {
        val values = HashMap<String, ColorSpace>()
        operator fun get(name: String) = values[name] ?: values[name.toLowerCase()]
        val HSLuv = object: ColorSpace("HSLuv", HSLuvGLSL.GLSL + "\n" +
                "vec3 spaceToRGB(vec3 hsl){\n" +
                "   return clamp(hsluvToRgb(hsl*vec3(360.0, 100.0, 100.0)), 0.0, 1.0);\n" +
                "}\n", Vector3f(0f, 1f, 0.5f)){
            override fun fromRGB(rgb: Vector3f): Vector3f {
                return HSLuvColorSpace.rgbToHsluv(doubleArrayOf(
                    rgb.x.toDouble(), rgb.y.toDouble(), rgb.z.toDouble()
                )).toVec3().mul(1/360f, 1/100f, 1/100f)
            }
            override fun toRGB(input: Vector3f): Vector3f {
                return HSLuvColorSpace.hsluvToRgb(doubleArrayOf(
                    input.x * 360.0, input.y * 100.0, input.z * 100.0
                )).toVec3()
            }
        }
        val HSV = object: ColorSpace("HSV", HSVColorSpace.GLSL, Vector3f(0f, 0.7f, 1f)){
            override fun fromRGB(rgb: Vector3f): Vector3f {
                return HSVColorSpace.rgbToHSV(rgb.x, rgb.y, rgb.z)
            }
            override fun toRGB(input: Vector3f): Vector3f {
                return HSVColorSpace.hsvToRGB(input.x, input.y, input.z)
            }
        }
    }
}