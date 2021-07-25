package me.anno.graph.types.flow

import me.anno.graph.NodeInput
import me.anno.graph.NodeOutput
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

abstract class ControlFlowNode() : FlowGraphNode() {

    // may not have details
    override fun createUI(list: PanelListY, style: Style) {}

    constructor(numInputs: Int, numOutputs: Int) : this() {
        inputs = Array(numInputs) { NodeInput("", this) }
        outputs = Array(numOutputs) { NodeOutput("", this) }
    }

    constructor(
        otherInputs: List<String>,
        otherOutputs: List<String>
    ) : this() {
        inputs = Array(otherInputs.size) { NodeInput(otherInputs[it], this) }
        outputs = Array(otherOutputs.size) { NodeOutput(otherOutputs[it], this) }
    }

    constructor(
        numFlowInputs: Int,
        otherInputs: List<String>,
        numFlowOutputs: Int,
        otherOutputs: List<String>
    ) : this() {
        inputs = Array(numFlowInputs + otherInputs.size) {
            NodeInput(if (it < numFlowInputs) "Flow" else otherInputs[it - numFlowInputs], this)
        }
        outputs = Array(numFlowOutputs + otherOutputs.size) {
            NodeOutput(if (it < numFlowOutputs) "Flow" else otherOutputs[it - numFlowOutputs], this)
        }
    }

    // todo one or multiple inputs
    // todo one or multiple outputs

    // todo control flow should be able to have multiple input nodes, but only one output node

}