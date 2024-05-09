package me.anno.graph.visual.control

import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLFlowNode
import me.anno.graph.visual.render.compiler.GraphCompiler

class ForNode : FixedControlFlowNode("For Loop", inputs, outputs), GLSLFlowNode {

    init {
        setInput(1, 0)
        setInput(2, 0)
        setInput(3, 1)
        setInput(4, false)
    }

    override fun execute(): NodeOutput {
        val graph = graph as FlowGraph
        val startIndex = inputs[1].getValue() as Long
        val endIndex = inputs[2].getValue() as Long
        val increment = inputs[3].getValue() as Long
        val reversed = inputs[4].getValue() as Boolean
        if (startIndex != endIndex) {
            val running = getOutputNodes(0).others.mapNotNull { it.node }
            if (running.isNotEmpty()) {
                if (reversed) {
                    for (index in (startIndex - 1) downTo endIndex step increment) {
                        if (increment <= 0L) throw IllegalStateException("Step size must be > 0")
                        setOutput(1, index)
                        graph.requestId()
                        // new id, because it's a new run, and we need to invalidate all previously calculated values
                        // theoretically it would be enough to just invalidate the ones in that subgraph
                        // we'd have to calculate that list
                        graph.executeNodes(running)
                    }
                } else {
                    for (index in startIndex until endIndex step increment) {
                        if (increment <= 0L) throw IllegalStateException("Step size must be > 0")
                        setOutput(1, index)
                        graph.requestId()
                        // new id, because it's a new run, and we need to invalidate all previously calculated values
                        // theoretically it would be enough to just invalidate the ones in that subgraph
                        // we'd have to calculate that list
                        graph.executeNodes(running)
                    }
                }
            }// else done
        }
        return getOutputNodes(2)
    }

    override fun buildCode(g: GraphCompiler, depth: Int): Boolean {
        val ki = g.loopIndexCtr++
        val body = getOutputNode(0)
        if (body != null) {
            val descending = g.constEval(inputs[4])
            if (descending is Boolean) { // is that value constant?
                g.builder.append("for(int i").append(ki).append('=')
                g.expr(inputs[1])
                if (descending) g.builder.append("-1")
                g.builder.append(";i").append(ki).append(if (descending) ">=" else "<")
                g.expr(inputs[2])
            } else { // idk about this, shouldn't be used...
                g.builder.append("bool d$ki=").append(ki).append('=')
                g.expr(inputs[4])
                g.builder.append(";\nfor(int i").append(ki).append('=')
                g.expr(inputs[1])
                g.builder.append("-(d$ki?1:0);d$ki?i$ki>=")
                g.expr(inputs[2])
                g.builder.append(":i$ki<")
                g.expr(inputs[2])
            }
            g.builder.append(";i$ki+=")
            g.expr(inputs[3])
            g.builder.append("){\n")
            g.buildCode(body, depth + 1)
            g.builder.append("}\n")
        }
        return g.buildCode(getOutputNode(2), depth)
    }

    companion object {
        val inputs = listOf(
            "Flow", beforeName,
            "Long", "Start Index",
            "Long", "End Index",
            "Long", "Step",
            "Boolean", "Descending"
        )
        val outputs = listOf(
            "Flow", "Loop Body",
            "Long", "Loop Index",
            "Flow", afterName
        )
    }
}