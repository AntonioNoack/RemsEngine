@file:Suppress("unused")

package me.anno.graph.visual.scalar

import me.anno.graph.visual.ComputeNode
import me.anno.graph.visual.EnumNode
import me.anno.graph.visual.FlowGraphNodeUtils.getBoolInput
import me.anno.graph.visual.render.compiler.GLSLFuncNode

class NotNode : ComputeNode("Binary Not", listOf("Boolean", "Value"), "Boolean") {
    override fun compute() {
        setOutput(0, !getBoolInput(0))
    }
}

private val dataB2 = MathNodeData(
    BooleanMathsBinary.entries,
    listOf("Boolean", "Boolean"), "Boolean",
    BooleanMathsBinary::id, BooleanMathsBinary::glsl
)

class MathB2Node : MathNode<BooleanMathsBinary>(dataB2), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, enumType.compute(getBoolInput(0), getBoolInput(1)))
    }
}

private val dataB3 = MathNodeData(
    BooleanMathsTernary.entries,
    listOf("Boolean", "Boolean", "Boolean"), "Boolean",
    BooleanMathsTernary::id, BooleanMathsTernary::glsl
)

class MathB3Node : MathNode<BooleanMathsTernary>(dataB3), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, enumType.compute(getBoolInput(0), getBoolInput(1), getBoolInput(2)))
    }
}
