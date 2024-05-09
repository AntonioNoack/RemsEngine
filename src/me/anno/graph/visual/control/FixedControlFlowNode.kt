package me.anno.graph.visual.control

import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.ControlFlowNode

abstract class FixedControlFlowNode : ControlFlowNode {

    constructor(name: String) : super(name)

    constructor(
        name: String,
        inputs: List<String>,
        outputs: List<String>,
    ) : super(name, inputs, outputs)

    fun getOutputNodes(index: Int): NodeOutput {
        val c = outputs[index] // NodeOutputs
        if (c.type != "Flow") throw RuntimeException()
        return c
    }

}