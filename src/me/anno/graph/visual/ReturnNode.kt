package me.anno.graph.visual

import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLFlowNode
import me.anno.graph.visual.render.compiler.GraphCompiler

open class ReturnNode(returnValues: List<String> = emptyList(), name: String = "Return") :
    FlowGraphNode(name, inputBase + returnValues, emptyList()), GLSLFlowNode, EarlyExitNode {

    constructor(name: String) : this(emptyList(), name)

    override fun execute(): NodeOutput? = null

    override fun buildCode(g: GraphCompiler, depth: Int): Boolean {
        g.handleReturnNode(this)
        return false
    }

    companion object {
        @JvmStatic
        private val inputBase = listOf("Flow", "Return")
    }
}