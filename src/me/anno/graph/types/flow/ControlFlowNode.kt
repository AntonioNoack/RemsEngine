package me.anno.graph.types.flow

import me.anno.graph.NodeInput
import me.anno.graph.NodeOutput
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

abstract class ControlFlowNode(name: String) : FlowGraphNode(name) {

    // may not have details
    override fun createUI(list: PanelListY, style: Style) {}

    constructor(name: String, numInputs: Int, numOutputs: Int) : this(name) {
        inputs = Array(numInputs) { NodeInput("", this) }
        outputs = Array(numOutputs) { NodeOutput("", this) }
    }

    constructor(
        name: String,
        otherInputs: List<String>,
        otherOutputs: List<String>
    ) : this(name) {
        inputs = Array(otherInputs.size) { NodeInput(otherInputs[it], this) }
        outputs = Array(otherOutputs.size) { NodeOutput(otherOutputs[it], this) }
    }

    constructor(
        name: String,
        numFlowInputs: Int,
        otherInputs: List<String>,
        numFlowOutputs: Int,
        otherOutputs: List<String>
    ) : this(name) {
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