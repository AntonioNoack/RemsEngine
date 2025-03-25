package me.anno.graph.visual.control

import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLFlowNode
import me.anno.graph.visual.render.compiler.GraphCompiler

class DoWhileNode : RecursiveFlowGraphNode<EmptyState>("Do While Loop", WhileNode.inputs, WhileNode.outputs),
    GLSLFlowNode {

    override fun execute(): NodeOutput {
        requestNextExecution(EmptyState.INSTANCE)
        return getNodeOutput(0) // first time is always executed
    }

    override fun continueExecution(state: EmptyState): NodeOutput {
        if (getBoolInput(1)) {
            // continue execution
            requestNextExecution(state)
            return getNodeOutput(0)
        } else {
            return getNodeOutput(1)
        }
    }

    override fun buildCode(g: GraphCompiler, depth: Int): Boolean {
        val body = getOutputNode(0)
        val cc = g.constEval(inputs[1])
        if (body != null && cc != false) {
            g.builder.append("do {")
            g.buildCode(body, depth + 1)
            g.builder.append("} while((\n")
            g.expr(inputs[1])
            g.builder.append(") && (budget--)>0);\n")
        }
        return g.buildCode(getOutputNode(1), depth)
    }
}