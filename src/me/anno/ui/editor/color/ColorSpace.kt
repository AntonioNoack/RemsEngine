package me.anno.ui.editor.color

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.uiVertexShader
import me.anno.gpu.shader.ShaderLib.uiVertexShaderList
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.language.translation.NameDesc
import me.anno.ui.editor.color.ColorChooser.Companion.circleBarRatio
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.ui.editor.color.spaces.HSV
import me.anno.ui.editor.color.spaces.HSI
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.collections.set
import kotlin.math.PI

// could be used to replace the two color spaces with more
abstract class ColorSpace(
    val naming: NameDesc,
    val serializationName: String,
    // display
    val glsl: Lazy<String>,
    val hue0: Vector3f
) {

    constructor(name: NameDesc, glsl: Lazy<String>, hue0: Vector3f) : this(name, name.englishName, glsl, hue0)

    private val shaders = HashMap<ColorVisualisation, Shader>()

    fun getShader(type: ColorVisualisation): Shader {
        val oldShader = shaders[type]
        if (oldShader != null) return oldShader
        val fragmentShader = when (type) {
            ColorVisualisation.WHEEL -> {
                glsl.value +
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
                glsl.value +
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
                glsl.value +
                        "void main(){\n" +
                        "   vec3 hsl = v0 + du * uv.x + dv * uv.y;\n" +
                        "   vec3 rgb = spaceToRGB(hsl);\n" +
                        "   gl_FragColor = vec4(rgb, 1.0);\n" +
                        "}"
            }
        }
        val newShader = Shader(
            "${naming.englishName}-${type.naming.englishName}",
            uiVertexShaderList, uiVertexShader, uvList, listOf(
                Variable(GLSLType.V2F, "ringSL"),
                Variable(GLSLType.V3F, "v0"),
                Variable(GLSLType.V3F, "du"),
                Variable(GLSLType.V3F, "dv"),
                Variable(GLSLType.V1F, "sharpness"),
                Variable(GLSLType.V1F, "lightness")
            ),
            fragmentShader
        )
        shaders[type] = newShader
        return newShader
    }

    abstract fun fromRGB(rgb: Vector3f, dst: Vector3f = Vector3f()): Vector3f
    abstract fun toRGB(input: Vector3f, dst: Vector3f = Vector3f()): Vector3f

    fun toRGB(x: Float, y: Float, z: Float, a: Float): Vector4f =
        Vector4f(toRGB(Vector3f(x, y, z)), a)
    fun toRGB(x: Double, y: Double, z: Double, a: Double): Vector4f =
        toRGB(x.toFloat(), y.toFloat(), z.toFloat(), a.toFloat())

    companion object {
        val list = lazy {
            arrayListOf(
                HSLuv,
                HSV,
                HSI
            )
        }
    }
}