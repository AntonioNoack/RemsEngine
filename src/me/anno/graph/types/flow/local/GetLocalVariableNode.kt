package me.anno.graph.types.flow.local

import me.anno.graph.Node
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode

class GetLocalVariableNode() : CalculationNode("GetLocal", inputs, outputs) {

    constructor(key: String) : this() {
        setInput(0, key)
    }

    override fun calculate(graph: FlowGraph): Any? {
        val key = getInput(graph, 0)
        return graph.localVariables[key]
    }

    override fun clone(): Node {
        val clone = GetLocalVariableNode()
        copy(clone)
        return clone
    }

    companion object {
        val inputs = listOf("String", "Name")
        val outputs = listOf("?", "Value")
    }

}