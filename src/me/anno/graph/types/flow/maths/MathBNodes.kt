package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode
import me.anno.graph.types.flow.ComputeNode
import me.anno.graph.types.flow.FlowGraphNodeUtils.getBoolInput

class NotNode : ComputeNode("Binary Not", listOf("Boolean", "Value"), "Boolean") {
    override fun compute() {
        setOutput(0, !getBoolInput(0))
    }
}

val dataB2 = MathNode.MathNodeData(
    BooleanMathsBinary.entries,
    listOf("Boolean", "Boolean"), "Boolean",
    { it.id }, { it.glsl }
)

class MathB2Node : MathNode<BooleanMathsBinary>(dataB2), EnumNode, GLSLExprNode {
    override fun compute() {
        setOutput(0, type.compute(getBoolInput(0), getBoolInput(1)))
    }
}

val dataB3 = MathNode.MathNodeData(
    BooleanMathsTernary.entries,
    listOf("Boolean", "Boolean", "Boolean"), "Boolean",
    { it.id }, { it.glsl }
)

class MathB3Node : MathNode<BooleanMathsTernary>(dataB3), EnumNode, GLSLExprNode {
    override fun compute() {
        setOutput(0, type.compute(getBoolInput(0), getBoolInput(1), getBoolInput(2)))
    }
}
