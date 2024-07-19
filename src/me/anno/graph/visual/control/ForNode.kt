package me.anno.graph.visual.control

import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLFlowNode
import me.anno.graph.visual.render.compiler.GraphCompiler
import kotlin.math.sign

class ForNode : FixedControlFlowNode("For Loop", inputs, outputs), GLSLFlowNode {

    init {
        setInput(1, 0)
        setInput(2, 0)
        setInput(3, 1)
        setInput(4, false)
    }

    override fun execute(): NodeOutput? {
        val startIndex = getLongInput(1)
        var endIndex = getLongInput(2)
        val increment = getLongInput(3)
        val endInclusive = getBoolInput(4)
        if (endInclusive) endIndex += increment.sign // not safe regarding overflow, but that would be u64, so it should be rare
        if (increment != 0L) {
            requestNextExection(ForLoopState(startIndex, endIndex, increment))
        }
        return null
    }

    override fun continueExecution(state: Any?): NodeOutput {
        state as ForLoopState
        val index = state.currentIndex
        val comparison = index.compareTo(state.endIndex)
        val continueExecution = state.increment.sign * comparison.sign < 0
        if (continueExecution) {
            setOutput(1, index)
            state.currentIndex = index + state.increment
            requestNextExection(state)
            return getNodeOutput(0)
        } else {
            return getNodeOutput(2)
        }
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
            "Boolean", "End Inclusive"
        )
        val outputs = listOf(
            "Flow", "Loop Body",
            "Long", "Loop Index",
            "Flow", afterName
        )
    }
}