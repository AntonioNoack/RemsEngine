package me.anno.graph.types.flow.local

import me.anno.graph.Node
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.actions.ActionNode

class SetLocalVariableNode() : ActionNode("SetLocal", inputs, outputs) {

    constructor(key: String, value: Any?) : this() {
        setInputs(listOf(null, key, value))
    }

    override fun executeAction(graph: FlowGraph) {
        val key = getInput(graph, 1) as String
        val value = getInput(graph, 2)
        val previous = graph.localVariables[key]
        graph.localVariables[key] = value
        setOutput(previous, 1)
    }

    override fun clone(): Node {
        val clone = SetLocalVariableNode()
        copy(clone)
        return clone
    }

    companion object {
        val inputs = listOf("String", "Name", "?", "Value")
        val outputs = listOf("?", "Previous")
    }


}