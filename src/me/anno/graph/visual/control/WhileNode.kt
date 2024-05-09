package me.anno.graph.visual.control

import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLFlowNode
import me.anno.graph.visual.render.compiler.GraphCompiler

class WhileNode : FixedControlFlowNode("While Loop", inputs, outputs),
    GLSLFlowNode {

    override fun execute(): NodeOutput {
        val graph = graph as FlowGraph
        val running = getOutputNodes(0).others.mapNotNull { it.node }
        val condition0 = inputs[1]
        while (true) {
            val condition = condition0.getValue() != false
            if (!condition) break
            if (running.isNotEmpty()) {
                graph.requestId()
                // new id, because it's a new run, and we need to invalidate all previously calculated values
                // theoretically it would be enough to just invalidate the ones in that subgraph
                // we'd have to calculate that list
                for (node in running) {
                    graph.execute(node)
                }
            } else Thread.sleep(1) // wait until condition does false
        }
        return getOutputNodes(1)
    }

    override fun buildCode(g: GraphCompiler, depth: Int): Boolean {
        val body = getOutputNode(0)
        val cc = g.constEval(inputs[1])
        if (body != null && cc != false) {
            g.builder.append("while((")
            g.expr(inputs[1]) // condition
            g.builder.append(") && (budget--)>0){\n")
            g.buildCode(body, depth + 1)
            g.builder.append("}\n")
        }
        return g.buildCode(getOutputNode(1), depth)
    }

    companion object {
        val inputs = listOf("Flow", beforeName, "Boolean", "Condition")
        val outputs = listOf("Flow", "Loop Body", "Flow", afterName)
    }
}