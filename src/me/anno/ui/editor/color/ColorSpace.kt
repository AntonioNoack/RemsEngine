package me.anno.ui.editor.color

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.OpenGLShader.Companion.attribute
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.language.translation.NameDesc
import me.anno.ui.editor.color.ColorChooser.Companion.circleBarRatio
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.ui.editor.color.spaces.HSV
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.collections.set
import kotlin.math.PI

// could be used to replace the two color spaces with more
abstract class ColorSpace(
    val naming: NameDesc,
    val serializationName: String,
    // display
    val glsl: String,
    val hue0: Vector3f
) {

    constructor(name: NameDesc, glsl: String, hue0: Vector3f) : this(name, name.englishName, glsl, hue0)

    private val shaders = HashMap<ColorVisualisation, Shader>()

    fun getShader(type: ColorVisualisation): Shader {
        val oldShader = shaders[type]
        if (oldShader != null) return oldShader
        val vertexShader = "" +
                "$attribute vec2 coords;\n" +
                "uniform vec2 pos, size;\n" +
                "uniform mat4 transform;\n" +
                "void main(){\n" +
                "   gl_Position = transform * vec4((pos + coords * size)*2.-1., 0.0, 1.0);\n" +
                "   uv = coords;\n" +
                "}"
        val varyingShader = listOf(Variable(GLSLType.V2F, "uv"))
        val fragmentShader = when (type) {
            ColorVisualisation.WHEEL -> {
                "" +
                        "uniform vec2 ringSL;\n" +
                        "uniform vec3 v0, du, dv;\n" +
                        "uniform float sharpness;\n" +
                        glsl +
                        "void main(){\n" +
                        "   vec2 nuv = uv*2.0-1.0;\n" + // normalized uv
                        "   float dst = dot(nuv,nuv);\n" +
                        "   float radius = sqrt(dst);\n" +
                        "   float hue = atan(nuv.y, nuv.x) * ${(0.5 / PI)} + 0.5;\n" +
                        "   vec3 hsl = vec3(hue, ringSL);\n" +
                        "   float alpha = radius > 0.975 ? 1.0 + (0.975-radius)*sharpness : 1.0;\n" +
                        "   float isSquare = clamp((0.787-radius)*sharpness, 0.0, 1.0);\n" +
                        "   vec2 uv2 = clamp((uv-0.5)*1.8+0.5, 0.0, 1.0);\n" +
                        "   float dst2 = max(abs(uv2.x-0.5), abs(uv2.y-0.5));\n" +
                        "   alpha *= mix(1.0, clamp((0.5-dst2)*sharpness, 0.0, 1.0), isSquare);\n" +
                        "   if(alpha <= 0.0) discard;\n" +
                        "   vec3 ringColor = spaceToRGB(hsl);\n" +
                        "   vec3 squareColor = spaceToRGB(v0 + du * uv2.x + dv * uv2.y);\n" +
                        "   vec3 rgb = mix(ringColor, squareColor, isSquare);\n" +
                        "   gl_FragColor = vec4(rgb, alpha);\n" +
                        "}"
            }
            ColorVisualisation.CIRCLE -> {
                "" +
                        "uniform float lightness, sharpness;\n" +
                        glsl +
                        "void main(){\n" +
                        "   vec3 rgb;\n" +
                        "   float alpha = 1.0;\n" +
                        "   vec2 nuv = vec2(uv.x * ${1f + circleBarRatio}, uv.y) - 0.5;\n" + // normalized + bar
                        "   if(nuv.x > 0.5){\n" +
                        "       // a simple brightness bar \n" +
                        "       rgb = vec3(uv.y);\n" +
                        "       alpha = clamp(min(" +
                        "           min(" +
                        "               nuv.x-0.515," +
                        "               ${0.5f + circleBarRatio}-nuv.x" +
                        "           ), min(" +
                        "               nuv.y+0.5," +
                        "               0.5-nuv.y" +
                        "           )" +
                        "       ) * sharpness, 0.0, 1.0);\n" +
                        "   } else {\n" +
                        "       // a circle \n" +
                        "       float radius = 2.0 * length(nuv);\n" +
                        "       float dst = radius*radius;\n" +
                        "       float hue = atan(nuv.y, nuv.x) * ${0.5 / PI} + 0.5;\n" +
                        "       alpha = radius > 0.975 ? 1.0 + (0.975-radius)*sharpness : 1.0;\n" +
                        "       vec3 hsl = vec3(hue, radius, lightness);\n" +
                        "       rgb = spaceToRGB(hsl);\n" +
                        "   }\n" +
                        "   gl_FragColor = vec4(rgb, alpha);\n" +
                        "}"
            }
            ColorVisualisation.BOX -> {
                "" +
                        "uniform vec3 v0, du, dv;\n" +
                        glsl +
                        "void main(){\n" +
                        "   vec3 hsl = v0 + du * uv.x + dv * uv.y;\n" +
                        "   vec3 rgb = spaceToRGB(hsl);\n" +
                        "   gl_FragColor = vec4(rgb, 1.0);\n" +
                        "}"
            }
        }
        val newShader = Shader(
            "$naming-${type.naming}",
            vertexShader,
            varyingShader,
            fragmentShader
        )
        shaders[type] = newShader
        return newShader
    }

    abstract fun fromRGB(rgb: Vector3f, dst: Vector3f = Vector3f()): Vector3f
    abstract fun toRGB(input: Vector3f, dst: Vector3f = Vector3f()): Vector3f

    fun toRGB(x: Float, y: Float, z: Float, a: Float) = Vector4f(toRGB(Vector3f(x, y, z)), a)
    fun toRGB(x: Double, y: Double, z: Double, a: Double) = toRGB(x.toFloat(), y.toFloat(), z.toFloat(), a.toFloat())

    companion object {
        val list = lazy {
            ArrayList<ColorSpace>().apply {
                add(HSLuv)
                add(HSV)
            }
        }
        /*val RGB = object: ColorSpace("R-GB", "vec3 spaceToRGB(vec3 rgb){ return rgb; }\n",
            Vector3f(0f, 0f, 0f)){
            override fun fromRGB(rgb: Vector3f): Vector3f {
                return rgb
            }
            override fun toRGB(input: Vector3f): Vector3f {
                return input
            }
        }*/

        // the hsi model is awkward... so complex, and so bad results...
        // and the cpu hsi->rgb impl. has an error somewhere (red @ white corner is pink)
        /*val HSI = object : ColorSpace("HSI", "" +
                "vec3 spaceToRGB(vec3 hsi){" +
                "   float r,g,b,h=hsi.x;\n" +
                "   if(h < 0.0) h+=1.0; else if(h>1.0) h-=1.0;\n" +
                "   float H = h * ${2.0*PI}, S = hsi.y, I = hsi.z;\n" +
                "   if(H < ${2*PI/3}){\n" +
                "       b = (1.0-S)/3.0;\n" +
                "       r = (1.0+S*cos(H)/cos(${PI/3}-H))/3.0;\n" +
                "       g = 1.0-(r+b);\n" +
                "   } else if(H < ${4*PI/3}){\n" +
                "       H-=${2*PI/3};\n" +
                "       r = (1.0-S)/3.0;\n" +
                "       g = (1.0+S*cos(H)/cos(${PI/3}-H))/3.0;\n" +
                "       b = 1.0-(r+g);\n" +
                "   } else {\n" +
                "       H-=${4*PI/3};\n" +
                "       g = (1.0-S)/3.0;\n" +
                "       b = (1.0+S*cos(H)/cos(${PI/3}-H))/3.0;\n" +
                "       r = 1.0-(g+b);\n" +
                "   }" +
                "   vec3 rgb = vec3(r,g,b)*(3.0*I);\n" +
                "   return clamp(rgb, 0.0, 1.0);\n" +
                "}", Vector3f(0f, 0.7f, 1f)) {

            // http://fourier.eng.hmc.edu/e161/lectures/ColorProcessing/node3.html
            override fun toRGB(input: Vector3f): Vector3f {
                var h = input.x
                if(h < 0f) h += 1f else if(h > 1f) h -= 1f
                var hue = h * 2.0 * PI
                val d60 = PI / 3.0
                val s = input.y
                val r: Float
                val g: Float
                val b: Float
                when {
                    h < 1f/3f -> {
                        b = (1f-s)/3f
                        r = ((1f+s*cos(hue))/cos(d60 - hue)).toFloat()/3f
                        g = 1f-(b+r)
                    }
                    h < 2f/3f -> {
                        hue -= 2*d60
                        r = (1f-s)/3f
                        g = ((1f+s*cos(hue))/cos(d60 - hue)).toFloat()/3f
                        b = 1f-(r+g)
                    }
                    else -> {
                        hue -= 4*d60
                        g = (1f-s)/3f
                        b = ((1f+s*cos(hue))/cos(d60 - hue)).toFloat()/3f
                        r = 1f-(g+b)
                    }
                }
                val i = input.z * 3
                return Vector3f(clamp01(r*i), clamp01(g*i), clamp01(b*i))
            }

            // http://fourier.eng.hmc.edu/e161/lectures/ColorProcessing/node2.html
            override fun fromRGB(rgb: Vector3f): Vector3f {
                val i = (rgb.x + rgb.y + rgb.z) / 3f
                val r = rgb.x / i
                val g = rgb.y / i
                val b = rgb.z / i
                return if(r == g && g == b){
                    // s = 0, h = 0
                    Vector3f(0f, 0f, i)
                } else {
                    val s = 1 - min(rgb.x, min(rgb.y, rgb.z)) / i
                    val hSqrtTerm = sq(r-g) + (r-b)*(g-b)
                    var h = acos(clamp01(((r-g)+(r-b))/(2*sqrt(hSqrtTerm)))) / (2*PI).toFloat()
                    if(b > g) h = 1f-h
                    Vector3f(h, s, i)
                }
            }
        }*/

    }
}