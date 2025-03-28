package me.anno.graph.visual.control

import me.anno.graph.visual.FlowGraphNode
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLFlowNode
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.utils.types.Booleans.toInt

class IfElseNode : FlowGraphNode("If-Else Branch", inputs, outputs), GLSLFlowNode {

    init {
        setInput(0, false)
    }

    override fun execute(): NodeOutput {
        val secondBranch = inputs[1].getValue() != true
        return getNodeOutput(secondBranch.toInt())
    }

    override fun buildCode(g: GraphCompiler, depth: Int): Boolean {
        val ifTrue = getOutputNode(0)
        val ifFalse = getOutputNode(1)
        if (ifTrue == ifFalse) { // easy, nothing to do
            return g.buildCode(ifTrue, depth)
        }
        // constant eval if possible
        return when (g.constEval(inputs[1])) {
            true -> g.buildCode(ifTrue, depth)
            false -> g.buildCode(ifFalse, depth)
            else -> {
                if (ifTrue != null && ifFalse != null) {
                    g.builder.append("if(")
                    g.expr(inputs[1]) // condition
                    g.builder.append("){\n")
                    val needsReturnT = g.buildCode(ifTrue, depth)
                    g.builder.append("} else {\n")
                    val needsReturnF = g.buildCode(ifFalse, depth)
                    g.builder.append("}\n")
                    needsReturnT || needsReturnF
                } else {
                    g.builder.append(if (ifTrue != null) "if((" else "if(!(")
                    g.expr(inputs[1]) // condition
                    g.builder.append(")){\n")
                    val needsExtraReturn = g.buildCode(ifTrue ?: ifFalse, depth)
                    g.builder.append("}\n")
                    needsExtraReturn
                }
            }
        }
    }

    companion object {
        val inputs = listOf("Flow", beforeName, "Boolean", "Condition")
        val outputs = listOf("Flow", "If True", "Flow", "If False")
    }
}