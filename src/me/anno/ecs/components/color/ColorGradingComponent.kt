package me.anno.ecs.components.color

import me.anno.animation.AnimatedProperty
import me.anno.ecs.Component
import me.anno.ecs.components.shaders.FragmentShaderComponent
import me.anno.ecs.components.shaders.ShaderEnvironment
import me.anno.ecs.components.shaders.VariableType
import me.anno.gpu.shader.Shader
import me.anno.io.base.BaseWriter
import org.joml.Vector3f
import org.joml.Vector4f

class ColorGradingComponent : Component(), FragmentShaderComponent {

    val slope = AnimatedProperty.color(Vector4f(1f))
    val offset = AnimatedProperty.color3(Vector3f(0f))
    val power = AnimatedProperty.color(Vector4f(1f))
    val saturation = AnimatedProperty.float(1f)

    override val className get() = "ColorGradingComponent"

    override fun getShaderComponent(env: ShaderEnvironment): String {
        val colorTmp = env[this, "cgcTmp1", VariableType.TEMPORARY]
        val grayTmp = env[this, "cgcTmp2", VariableType.TEMPORARY]
        val slope = env[this, "slope", VariableType.UNIFORM_V3]
        val power = env[this, "power", VariableType.UNIFORM_V3]
        val offset = env[this, "offset", VariableType.UNIFORM_V3]
        val saturation = env[this, "saturation", VariableType.UNIFORM_V1]
        return "vec3 $colorTmp = pow(max(vec3(0.0), color * $slope + $offset), $power);\n" +
                "float $grayTmp = brightness($colorTmp);\n" +
                "color.rgb = mix(vec3($grayTmp), $colorTmp, $saturation);\n"
    }

    override fun bindUniforms(shader: Shader, env: ShaderEnvironment, time: Double) {
        shader.v3X(env[this, "slope", VariableType.UNIFORM_V3], slope[time])
        shader.v3X(env[this, "power", VariableType.UNIFORM_V3], power[time])
        shader.v3f(env[this, "offset", VariableType.UNIFORM_V3], offset[time])
        shader.v1f(env[this, "saturation", VariableType.UNIFORM_V1], saturation[time])
    }

    /*override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        list += vi("Slope", "", slope, style)
        list += vi("Offset", "", offset, style)
        list += vi("Power", "", power, style)
        list += vi("Saturation", "", saturation, style)
    }*/

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "slope", slope)
        writer.writeObject(this, "offset", offset)
        writer.writeObject(this, "power", power)
        writer.writeObject(this, "saturation", saturation)
    }

    override fun getShaderCodeState(): Any? = null

    override fun clone(): Component {
        throw RuntimeException("Not yet implemented")
    }

}