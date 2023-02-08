package me.anno.graph.render

import me.anno.gpu.shader.RandomEffect.randomFunc
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode
import me.anno.graph.types.flow.maths.GLSLExprNode
import me.anno.maths.Maths.fract
import org.joml.Vector2f
import kotlin.math.sin

// todo make input and output vec4s :)
class RandomNode : CalculationNode("Random", listOf("Vector2f", "Seed"), listOf("Float", "Result")), GLSLExprNode {

    init {
        setInput(0, Vector2f())
    }

    override fun calculate(): Float {
        val uv = getInput(0) as Vector2f
        return fract(sin(uv.x * 12.9898f + uv.y * 78.233f) * 43758.547f)
    }

    override fun getShaderFuncName(outputIndex: Int): String = "rand2"

    override fun defineShaderFunc(outputIndex: Int): String {
        return "(vec2 uv){\n${randomFunc}return GET_RANDOM(uv);}"
    }
}