package me.anno.graph.types

/**
 * given an input, execute until termination
 * */
open class ControlFlowGraph : FlowGraph() {
    override val className: String get() = "ControlFlowGraph"
}