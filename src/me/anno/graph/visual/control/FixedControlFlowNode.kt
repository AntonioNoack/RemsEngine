package me.anno.graph.visual.control

import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.FlowGraphNode

abstract class FixedControlFlowNode(name: String, inputs: List<String>, outputs: List<String>) :
    FlowGraphNode(name, inputs, outputs) {

    fun getNodeOutput(index: Int): NodeOutput {
        val c = outputs[index] // NodeOutputs
        if (c.type != "Flow") throw RuntimeException()
        return c
    }
}