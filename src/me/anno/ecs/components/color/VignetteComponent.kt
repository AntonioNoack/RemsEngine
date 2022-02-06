package me.anno.ecs.components.color

import me.anno.ecs.Component
import me.anno.ecs.components.shaders.FragmentShaderComponent
import me.anno.ecs.components.shaders.ShaderEnvironment
import me.anno.ecs.components.shaders.VariableType
import me.anno.gpu.shader.Shader
import me.anno.io.base.BaseWriter
import org.joml.Vector4f

class VignetteComponent : Component(), FragmentShaderComponent {

    val color = Vector4f()
    var strength = 1f

    override fun getShaderComponent(env: ShaderEnvironment): String {
        // "       float rSq = dot(nuv,nuv);\n" + needs to be added to the general shader...
        return "color = mix(" +
                "   vec4(${env[this, "color", VariableType.UNIFORM_V3]}, 1.0)," +
                "   color," +
                "   1.0/(1.0 + ${env[this, "strength", VariableType.UNIFORM_V1]}*rSq)" +
                ");\n"
    }

    override fun getShaderCodeState(): Any? = null

    override fun bindUniforms(shader: Shader, env: ShaderEnvironment) {
        shader.v3X(env[this, "color", VariableType.UNIFORM_V3], color)
        shader.v1f(env[this, "strength", VariableType.UNIFORM_V1], strength)
    }

    /*override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        val group = getGroup("Component", "", "")
        group += vi("Color", "", color, style)
        group += vi("Strength", "", strength, style)
    }*/

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("strength", strength)
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "strength" -> strength = value
            else -> super.readFloat(name, value)
        }
    }

    override val className get() = "VignetteComponent"

    override fun clone(): Component {
        throw RuntimeException("Not yet implemented")
    }

}