package me.anno.graph.visual.render

import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.render.compiler.GLSLFuncNode
import me.anno.maths.Maths.fract
import org.joml.Vector2f
import kotlin.math.sin

// todo make input and output vec4s :)
class RandomNode : CalculationNode("Random", listOf("Vector2f", "Seed"), listOf("Float", "Result")), GLSLFuncNode {

    init {
        setInput(0, Vector2f())
    }

    override fun calculate(): Float {
        val uv = getInput(0) as Vector2f
        return fract(sin(uv.x * 12.9898f + uv.y * 78.233f) * 43758.547f)
    }

    override fun getShaderFuncName(outputIndex: Int): String = "rand2"

    override fun defineShaderFunc(outputIndex: Int): String {
        return "(vec2 uv){ return fract(sin(dot(uv, vec2(12.9898,78.233))) * 43758.5453); }"
    }
}