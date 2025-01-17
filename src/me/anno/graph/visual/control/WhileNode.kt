package me.anno.graph.visual.control

import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLFlowNode
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.io.saveable.Saveable

class WhileNode : FixedControlFlowNode("While Loop", inputs, outputs), GLSLFlowNode {

    override fun execute(): NodeOutput? {
        requestNextExecution(null)
        return null
    }

    override fun continueExecution(state: Saveable?): NodeOutput {
        if (getBoolInput(1)) {
            requestNextExecution(null)
            return getNodeOutput(0)
        } else {
            return getNodeOutput(1)
        }
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