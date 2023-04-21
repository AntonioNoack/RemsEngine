package me.anno.graph.types

import me.anno.graph.NodeInput

/**
 * task: given a list of outputs, compute them
 * */
class DataFlowGraph : FlowGraph() {

    // todo given a selection of requested outputs, calculate the result

    // todo a data flow graph is just like a control flow graph;
    // todo except there are no inputs, just outputs

    fun compute(outputs: Set<NodeInput>): Any {
        validId++
        return outputs.map { it.getValue() }
    }

    override val className: String get() = "DataFlowGraph"

}