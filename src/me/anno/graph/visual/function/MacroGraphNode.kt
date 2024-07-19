package me.anno.graph.visual.function

import me.anno.graph.visual.FlowGraphNode
import me.anno.graph.visual.node.NodeOutput

class MacroGraphNode(name: String, inputs: List<String>, outputs: List<String>) :
    FlowGraphNode(name, inputs, outputs) {
    override fun execute(): NodeOutput? {
        TODO("Not yet implemented")
    }
}