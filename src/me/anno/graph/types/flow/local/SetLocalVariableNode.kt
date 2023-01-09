package me.anno.graph.types.flow.local

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.actions.ActionNode

class SetLocalVariableNode() : ActionNode("SetLocal", inputs, outputs) {

    constructor(key: String, value: Any?) : this() {
        setInputs(listOf(null, key, value))
    }

    fun getKey(graph: FlowGraph) = getInput(graph, 1) as String
    fun getValue(graph: FlowGraph) = getInput(graph, 2)

    override fun executeAction(graph: FlowGraph) {
        val key = getInput(graph, 1) as String
        val value = getInput(graph, 2)
        val previous = graph.localVariables[key]
        graph.localVariables[key] = value
        setOutput(value, 1)
        setOutput(previous, 2)
    }

    companion object {
        val inputs = listOf("String", "Name", "?", "New Value")
        val outputs = listOf("?", "Current Value", "?", "Previous Value")
    }


}