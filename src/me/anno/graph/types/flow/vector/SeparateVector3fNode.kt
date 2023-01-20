package me.anno.graph.types.flow.vector

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ValueNode
import org.joml.Vector3f

class SeparateVector3fNode : ValueNode("Separate Vector3f", inputs, outputs) {

    init {
        setInput(0, Vector3f())
    }

    override fun compute(graph: FlowGraph) {
        val v = getInput(graph, 0) as Vector3f
        setOutput(v.x, 0)
        setOutput(v.y, 1)
        setOutput(v.z, 2)
    }

    companion object {
        val inputs = listOf("Vector3f", "Vector")
        val outputs = listOf("Float", "X", "Float", "Y", "Float", "Z")
    }

}