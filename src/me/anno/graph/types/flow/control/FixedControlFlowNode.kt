package me.anno.graph.types.flow.control

import me.anno.graph.NodeOutput
import me.anno.graph.types.flow.ControlFlowNode

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