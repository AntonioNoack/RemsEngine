package me.anno.ui.editor.color

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.uiVertexShader
import me.anno.gpu.shader.ShaderLib.uiVertexShaderList
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.language.translation.NameDesc
import me.anno.ui.editor.color.spaces.HSI
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.ui.editor.color.spaces.HSV
import org.joml.Vector3f
import org.joml.Vector4f

// could be used to replace the two color spaces with more
abstract class ColorSpace(
    val nameDesc: NameDesc,
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
        val newShader = Shader(
            "${nameDesc.englishName}-${type.nameDesc.englishName}",
            uiVertexShaderList, uiVertexShader, uvList, listOf(
                Variable(GLSLType.V2F, "ringSL"),
                Variable(GLSLType.V3F, "v0"),
                Variable(GLSLType.V3F, "du"),
                Variable(GLSLType.V3F, "dv"),
                Variable(GLSLType.V1F, "sharpness"),
                Variable(GLSLType.V1F, "lightness")
            ), glsl.value + type.getFragmentShader()
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