package me.anno.graph.visual.control

import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.FlowGraphNode
import me.anno.graph.visual.node.NodeOutput
import me.anno.io.saveable.Saveable

abstract class RecursiveFlowGraphNode<State : Saveable>(name: String, inputs: List<String>, outputs: List<String>) :
    FlowGraphNode(name, inputs, outputs) {

    fun requestNextExecution(state: State) {
        (graph as? FlowGraph)?.push(this, state)
    }

    abstract fun continueExecution(state: State): NodeOutput?

    fun continueExecutionUnsafe(state: Saveable): NodeOutput? {
        @Suppress("UNCHECKED_CAST")
        return continueExecution(state as State)
    }
}