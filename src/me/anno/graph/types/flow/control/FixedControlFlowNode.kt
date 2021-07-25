package me.anno.graph.types.flow.control

import me.anno.graph.NodeOutput
import me.anno.graph.types.flow.ControlFlowNode

abstract class FixedControlFlowNode : ControlFlowNode {

    constructor() : super()

    constructor(inputs: List<String>, outputs: List<String>) : super(inputs, outputs)

    constructor(
        numFlowInputs: Int,
        otherInputs: List<String>,
        numFlowOutputs: Int,
        otherOutputs: List<String>
    ) : super(numFlowInputs, otherInputs, numFlowOutputs, otherOutputs)

    fun getOutputNodes(index: Int): NodeOutput {
        val c = outputs!![index] // NodeOutputs
        if (c.type != "Flow") throw RuntimeException()
        return c
    }

    override fun canAddInput(): Boolean = false
    override fun canAddOutput(): Boolean = false
    override fun canRemoveInput(): Boolean = false
    override fun canRemoveOutput(): Boolean = false

}