package me.anno.graph.types.flow.control

import me.anno.graph.NodeOutput
import me.anno.graph.types.FlowGraph

class IfElseNode() : FixedControlFlowNode(1, listOf("Boolean"), 2, emptyList()) {

    override fun execute(graph: FlowGraph): NodeOutput {
        val condition = graph.getValue(inputs!![1]) != false
        return getOutputNodes(if (condition) 0 else 1)
    }

}