package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode
import me.anno.graph.types.flow.ComputeNode

class NotNode : ComputeNode("Binary Not", listOf("Boolean", "Value"), "Boolean") {
    override fun compute() {
        setOutput(0, getInput(0) != true)
    }
}

val dataB2 = MathNode.MathNodeData(
    BooleanMathsBinary.entries,
    listOf("Boolean", "Boolean"), "Boolean",
    { it.id }, { it.glsl }
)

class MathB2Node : MathNode<BooleanMathsBinary>(dataB2), EnumNode, GLSLExprNode {
    override fun compute() {
        setOutput(0, type.compute(getInput(0) == true, getInput(1) == true))
    }
}

val dataB3 = MathNode.MathNodeData(
    BooleanMathsTernary.entries,
    listOf("Boolean", "Boolean", "Boolean"), "Boolean",
    { it.id }, { it.glsl }
)

class MathB3Node : MathNode<BooleanMathsTernary>(dataB3), EnumNode, GLSLExprNode {
    override fun compute() {
        setOutput(
            0, type.compute(
                getInput(0) == true,
                getInput(1) == true,
                getInput(2) == true
            )
        )
    }
}
