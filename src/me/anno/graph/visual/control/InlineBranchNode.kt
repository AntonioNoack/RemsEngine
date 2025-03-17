package me.anno.graph.visual.control

import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLExprNode
import me.anno.graph.visual.render.compiler.GraphCompiler

@Suppress("unused")
class InlineBranchNode(type: String = "?") : CalculationNode(
    "Inline $type Branch",
    listOf("Bool", "Condition", type, "IfTrue", type, "IfFalse"), type
), GLSLExprNode {

    // todo inline-switch-case-node (int/string input, many type inputs -> many outputs)

    init {
        setInput(0, false)
    }

    override fun calculate(): Any? {
        return getInput(if (getBoolInput(0)) 1 else 2)
    }

    override fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node) {
        g.builder.append('(')
        g.expr(inputs[0])
        g.builder.append('?')
        g.expr(inputs[1])
        g.builder.append(':')
        g.expr(inputs[2])
        g.builder.append(')')
    }
}