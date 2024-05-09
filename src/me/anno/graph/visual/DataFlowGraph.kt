package me.anno.graph.visual

import me.anno.graph.visual.node.NodeInput

/**
 * task: given a list of outputs, compute them
 * */
class DataFlowGraph : FlowGraph() {

    // todo given a selection of requested outputs, calculate the result

    // todo a data flow graph is just like a control flow graph;
    //  except there are no inputs, just outputs

    fun compute(outputs: Set<NodeInput>): Any {
        validId++
        return outputs.map { it.getValue() }
    }
}