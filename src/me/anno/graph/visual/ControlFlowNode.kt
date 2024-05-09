package me.anno.graph.visual

abstract class ControlFlowNode : FlowGraphNode {

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