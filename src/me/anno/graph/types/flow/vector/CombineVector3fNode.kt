package me.anno.graph.types.flow.vector

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode
import org.joml.Vector3f

class CombineVector3fNode : CalculationNode("Combine Vector3f", inputs, outputs) {

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