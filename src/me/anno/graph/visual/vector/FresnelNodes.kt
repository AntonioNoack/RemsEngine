package me.anno.graph.visual.vector

import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.FlowGraphNodeUtils.getFloatInput
import me.anno.graph.visual.render.compiler.GLSLFuncNode
import me.anno.maths.Maths.max
import me.anno.maths.Maths.pow
import org.joml.Vector3f

@Suppress("unused")
class FresnelNode3 : CalculationNode(
    "Vector3f Fresnel",
    listOf("Vector3f", "CamSpace Position", "Vector3f", "Normal", "Float", "Power"),
    "Float"
), GLSLFuncNode {

    init {
        setInput(0, Vector3f())
        setInput(1, Vector3f())
        setInput(2, 2f)
    }

    override fun calculate(): Float {
        val a = getInput(0) as Vector3f
        val b = getInput(1) as Vector3f
        val p = getFloatInput(2)
        return pow(max(-a.dot(b) / a.length(), 0f), p)
    }

    override fun getShaderFuncName(outputIndex: Int) = "fresnel3"
    override fun defineShaderFunc(outputIndex: Int): String {
        return "(vec3 c, vec3 n, float p){return pow(max(-dot(c,n)*inversesqrt(dot(c,c)),0.0),p);}"
    }
}
