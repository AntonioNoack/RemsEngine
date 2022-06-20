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
        val validId = validId++
        return outputs.map {
            it.castGetValue(this, validId)
        }
    }

    override val className: String = "DataFlowGraph"

}