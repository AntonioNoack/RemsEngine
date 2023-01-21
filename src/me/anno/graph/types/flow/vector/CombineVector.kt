package me.anno.graph.types.flow.vector

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class CombineVector2f : CalculationNode("Combine Vector2f", inputs, outputs) {

    init {
        setInput(0, 0f)
        setInput(1, 0f)
    }

    override fun calculate(graph: FlowGraph): Vector2f {
        val x = getInput(graph, 0) as Float
        val y = getInput(graph, 1) as Float
        return Vector2f(x, y)
    }

    companion object {
        val inputs = listOf("Float", "X", "Float", "Y")
        val outputs = listOf("Vector2f", "Vector")
    }

}

class CombineVector3f : CalculationNode("Combine Vector3f", inputs, outputs) {

    init {
        setInput(0, 0f)
        setInput(1, 0f)
        setInput(2, 0f)
    }

    override fun calculate(graph: FlowGraph): Vector3f {
        val x = getInput(graph, 0) as Float
        val y = getInput(graph, 1) as Float
        val z = getInput(graph, 2) as Float
        return Vector3f(x, y, z)
    }

    companion object {
        val inputs = listOf("Float", "X", "Float", "Y", "Float", "Z")
        val outputs = listOf("Vector3f", "Vector")
    }

}

class CombineVector4f : CalculationNode("Combine Vector4f", inputs, outputs) {

    init {
        setInput(0, 0f)
        setInput(1, 0f)
        setInput(2, 0f)
        setInput(3, 0f)
    }

    override fun calculate(graph: FlowGraph): Vector4f {
        val x = getInput(graph, 0) as Float
        val y = getInput(graph, 1) as Float
        val z = getInput(graph, 2) as Float
        val w = getInput(graph, 3) as Float
        return Vector4f(x, y, z, w)
    }

    companion object {
        val inputs = listOf("Float", "X", "Float", "Y", "Float", "Z", "Float", "W")
        val outputs = listOf("Vector4f", "Vector")
    }

}