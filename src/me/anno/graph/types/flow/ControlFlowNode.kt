package me.anno.graph.types.flow

import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

abstract class ControlFlowNode : FlowGraphNode {

    // may not have details
    override fun createUI(list: PanelListY, style: Style) {}

    constructor(name: String) : super(name)

    constructor(
        name: String,
        inputs: List<String>,
        outputs: List<String>
    ) : super(name, inputs, outputs)

    // todo one or multiple inputs
    // todo one or multiple outputs

    // todo control flow should be able to have multiple input nodes, but only one output node

}