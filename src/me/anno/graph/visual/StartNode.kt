package me.anno.graph.visual

import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.graph.visual.render.compiler.GLSLFlowNode

open class StartNode(funcArguments: List<String> = emptyList()) :
    FlowGraphNode("Start", emptyList(), flow + funcArguments), GLSLFlowNode {

    override fun execute(): NodeOutput? {
        return outputs.getOrNull(0)
    }

    override fun buildCode(g: GraphCompiler, depth: Int): Boolean {
        return g.buildCode(getOutputNode(0), depth)
    }

    companion object {
        private val flow = listOf("Flow", "Start")
    }
}