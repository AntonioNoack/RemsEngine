package me.anno.graph.visual.render

import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.FlowGraphNodeUtils.getFloatInput
import me.anno.graph.visual.scalar.GLSLFuncNode
import org.joml.Matrix3f
import org.joml.Vector3f

class NormalMap : CalculationNode(
    "Normal Map", listOf(
        "Vector3f", "Normal",
        "Vector3f", "Tangent",
        "Vector3f", "Bitangent",
        "Float", "Strength",
        "Vector3f", "Texture RGB"
    ), "Vector3f"
), GLSLFuncNode {

    override fun calculate(): Vector3f {
        val normal = getInput(0) as Vector3f
        val tangent = getInput(1) as Vector3f
        val bitangent = getInput(2) as Vector3f
        val strength = getFloatInput(3)
        val rgb = getInput(4) as Vector3f
        val m = Matrix3f(tangent, bitangent, normal)
        val normalFromTex = Vector3f(rgb).mul(2f).sub(1f, 1f, 1f).mul(m) // transpose??
        return normal.lerp(normalFromTex, strength, normalFromTex)
    }

    override fun getShaderFuncName(outputIndex: Int) = "normalMapNode"
    override fun defineShaderFunc(outputIndex: Int): String {
        return "(vec3 finalNormal, vec3 finalTangent, vec3 finalBitangent, float strength, vec3 normalMapRGB){\n" +
                "   if(strength == 0.0) return finalNormal;\n" +
                "   mat3 tbn = mat3(finalTangent, finalBitangent, finalNormal);\n" +
                "   vec3 normalFromTex = normalMapRGB * 2.0 - 1.0;\n" + // normalize?
                "        normalFromTex = matMul(tbn, normalFromTex);\n" +
                "   return mix(finalNormal, normalFromTex, strength);\n" +
                "}"
    }

}