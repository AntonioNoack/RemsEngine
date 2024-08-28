package me.anno.graph.visual.control

import me.anno.graph.visual.FlowGraphNode
import me.anno.graph.visual.node.NodeOutput

abstract class FixedControlFlowNode(name: String, inputs: List<String>, outputs: List<String>) :
    FlowGraphNode(name, inputs, outputs) {

    fun getNodeOutput(index: Int): NodeOutput = outputs[index]
}