package me.anno.graph.visual.states

import me.anno.graph.visual.node.NodeConnector
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.actions.ActionNode

open class StateNode(
    name: String = "State",
    inputs: List<String> = emptyList(),
    outputs: List<String> = emptyList()
) : ActionNode(name, inputs, outputs) {

    open fun update(): StateNode {
        return try {
            val outputs = outputs
            val graph = graph as? FlowGraph
            if (graph != null) {
                for (output in outputs) {
                    for (input in output.others) {
                        val node = input.node
                        if (node != null) {
                            graph.execute(node)
                        }
                    }
                }
            }
            this
        } catch (e: NewState) {
            e.state
        }
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