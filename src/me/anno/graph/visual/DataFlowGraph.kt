package me.anno.graph.visual

import me.anno.graph.visual.node.NodeInput

/**
 * a data flow graph is just like a control flow graph;
 * except there are no inputs, just outputs
 * */
class DataFlowGraph : FlowGraph() {
    fun compute(outputs: Set<NodeInput>): List<Any?> {
        validId++
        return outputs.map { it.getValue() }
    }
}