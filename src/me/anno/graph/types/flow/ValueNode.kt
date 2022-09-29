package me.anno.graph.types.flow

import me.anno.graph.NodeOutput
import me.anno.graph.types.FlowGraph
import me.anno.ui.base.groups.PanelList
import me.anno.ui.style.Style

abstract class ValueNode : FlowGraphNode {

    constructor(name: String) : super(name)
    constructor(name: String, inputs: List<String>, outputs: List<String>) : super(name, inputs, outputs)

    override fun createUI(list: PanelList, style: Style) {}

    abstract fun compute(graph: FlowGraph)

    override fun execute(graph: FlowGraph): NodeOutput? {
        compute(graph)
        return null
    }

}