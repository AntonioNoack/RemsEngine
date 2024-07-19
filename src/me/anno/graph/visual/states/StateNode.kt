package me.anno.graph.visual.states

import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.node.NodeConnector

open class StateNode(
    name: String = "State",
    inputs: List<String> = emptyList(),
    outputs: List<String> = emptyList()
) : ActionNode(name, inputs, outputs) {

    open fun update(): StateNode {

        val outputs = outputs
        val graph = graph as? FlowGraph
        if (graph != null) {
            val depth = graph.nodeStack.size
            try {
                for (i in outputs.indices) {
                    graph.executeNodes(outputs[i].others)
                }
            } catch (e: NewState) {
                while (graph.nodeStack.size > depth) { // could be more elegant...
                    graph.nodeStack.removeLast(true)
                }
                return e.state
            }
        }
        return this
    }

    open fun onEnterState(oldState: StateNode?) {}

    open fun onExitState(newState: StateNode?) {}

    final override fun executeAction() {
        throw NewState(this)
    }

    override fun canAddOutput(type: String, index: Int): Boolean = index > 0 && type == "Flow"
    override fun canRemoveOutput(type: String, index: Int): Boolean = index > 0 && type == "Flow"

    override fun supportsMultipleInputs(con: NodeConnector) = true
    override fun supportsMultipleOutputs(con: NodeConnector) = true
}