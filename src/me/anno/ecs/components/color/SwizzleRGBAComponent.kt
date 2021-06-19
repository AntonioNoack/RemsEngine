package me.anno.ecs.components.color

import me.anno.gpu.shader.Shader
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.animation.AnimatedProperty
import me.anno.ecs.Component
import me.anno.ecs.components.shaders.FragmentShaderComponent
import me.anno.ecs.components.shaders.ShaderEnvironment
import me.anno.ecs.components.shaders.VariableType
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.structures.ValueWithDefault
import me.anno.utils.structures.ValueWithDefault.Companion.writeMaybe

class SwizzleRGBAComponent : Component(), FragmentShaderComponent {

    enum class Swizzle(val code: Int, val isRGBA: Boolean, val simpleCode: Char, val complexCode: String) {

        ZERO(0, false, ' ', "0.0"),
        ONE(1, false, ' ', "1.0"),
        HALF(2, false, ' ', "0.5"),

        R(10, true, 'r', "color.r"),
        G(11, true, 'g', "color.g"),
        B(12, true, 'b', "color.b"),
        A(13, true, 'a', "color.a"),

        INVERSE_R(20, false, ' ', "1.0-color.r"),
        INVERSE_G(21, false, ' ', "1.0-color.g"),
        INVERSE_B(22, false, ' ', "1.0-color.b"),
        INVERSE_A(23, false, ' ', "1.0-color.a"),

        // H, S, V,

        ;
        companion object {
            operator fun get(code: Int): Swizzle? {
                return values().firstOrNull { it.code == code }
            }
        }

    }

    val swizzleR = ValueWithDefault(Swizzle.R)
    val swizzleG = ValueWithDefault(Swizzle.G)
    val swizzleB = ValueWithDefault(Swizzle.B)
    val swizzleA = ValueWithDefault(Swizzle.A)

    val strength = AnimatedProperty.float(1f)

    override fun getClassName(): String = "SwizzleRGBAComponent"

    /*override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        list += vi("R", "", "", null, swizzleR, style)
        list += vi("G", "", "", null, swizzleG, style)
        list += vi("B", "", "", null, swizzleB, style)
        list += vi("A", "", "", null, swizzleA, style)
        list += vi("Strength", "", strength, style)
    }*/

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeMaybe(this, "swizzleR", swizzleR)
        writer.writeMaybe(this, "swizzleG", swizzleG)
        writer.writeMaybe(this, "swizzleB", swizzleB)
        writer.writeMaybe(this, "swizzleA", swizzleA)
        writer.writeObject(this, "strength", strength)
    }

    override fun readInt(name: String, value: Int) {
        when(name){
            "swizzleR" -> swizzleR.value = Swizzle[value] ?: return
            "swizzleG" -> swizzleG.value = Swizzle[value] ?: return
            "swizzleB" -> swizzleB.value = Swizzle[value] ?: return
            "swizzleA" -> swizzleA.value = Swizzle[value] ?: return
            else -> super.readInt(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "strength" -> strength.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun getShaderComponent(env: ShaderEnvironment): String {
        val a = swizzleA.value
        val r = swizzleR.value
        val g = swizzleG.value
        val b = swizzleB.value
        val strength = env[this, "strength", VariableType.UNIFORM_V1]
        return if (a.isRGBA && r.isRGBA && g.isRGBA && b.isRGBA) {
            val swizzle = String(charArrayOf(r.simpleCode, g.simpleCode, b.simpleCode, a.simpleCode))
            "color = mix(color, color.$swizzle, $strength);\n"
        } else {
            val swizzle = "vec4(${r.complexCode}, ${g.complexCode}, ${b.complexCode}, ${a.complexCode})"
            "color = mix(color, $swizzle, $strength);\n"
        }
    }

    override fun getShaderCodeState(): Any = listOf(swizzleR.value, swizzleG.value, swizzleB.value, swizzleA.value)

    override fun bindUniforms(shader: Shader, env: ShaderEnvironment, time: Double) {
        shader.v1(env[this, "strength", VariableType.UNIFORM_V1], strength[time])
    }

}