package me.anno.graph.types.flow.local

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.actions.ActionNode

class SetLocalVariableNode() : ActionNode(listOf("String", "?"), listOf("String", "?")) {

    constructor(key: String, value: Any?) : this() {
        setInputs(listOf(null, key, value))
    }

    override fun executeAction(graph: FlowGraph) {
        val key = getInput(graph, 1) as String
        val value = getInput(graph, 2)
        graph.localVariables[key] = value
        setOutput(key, 1)
        setOutput(value, 2)
    }

}