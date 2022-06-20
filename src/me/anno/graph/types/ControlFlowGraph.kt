package me.anno.graph.types

import me.anno.graph.Node

/**
 * given an input, execute until termination
 * */
open class ControlFlowGraph : FlowGraph() {

    var inputNode: Node? = null

    override val className = "ControlFlowGraph"

}