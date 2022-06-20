package me.anno.graph.types.flow

import me.anno.graph.NodeOutput
import me.anno.graph.types.FlowGraph
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

abstract class ValueNode : FlowGraphNode {

    constructor(name: String) : super(name)
    constructor(name: String, inputs: List<String>, outputs: List<String>) : super(name, inputs, outputs)

    /*constructor(
        name: String,
        inputType: String,
        inputCount: Int,
        outputType: String,
        outputCount: Int
    ) : this(name, Array(inputCount) { inputType }.toList(), Array(outputCount) { outputType }.toList())*/

    override fun createUI(list: PanelListY, style: Style) {}

    abstract fun compute(graph: FlowGraph)

    override fun execute(graph: FlowGraph): NodeOutput? {
        compute(graph)
        return null
    }

}