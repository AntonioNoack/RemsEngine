package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode

val dataF1 = MathNode.MathNodeData(
    FloatMathsUnary.entries,
    listOf("Float"), "Float",
    { it.id }, { it.glsl }
)

class MathF1Node : MathNode<FloatMathsUnary>(dataF1), EnumNode, GLSLExprNode {
    override fun compute() {
        setOutput(0, type.float(getInput(0) as Float))
    }
}

val dataF2 = MathNode.MathNodeData(
    FloatMathsBinary.entries,
    listOf("Float", "Float"), "Float",
    { it.id }, { it.glsl }
)

class MathF2Node : MathNode<FloatMathsBinary>(dataF2), EnumNode, GLSLExprNode {
    override fun compute() {
        setOutput(0, type.float(getInput(0) as Float, getInput(1) as Float))
    }
}

val dataF3 = MathNode.MathNodeData(
    FloatMathsTernary.entries,
    listOf("Float", "Float", "Float"), "Float",
    { it.id }, { it.glsl }
)

class MathF3Node : MathNode<FloatMathsTernary>(dataF3), EnumNode, GLSLExprNode {
    override fun compute() {
        setOutput(0, type.float(getInput(0) as Float, getInput(1) as Float, getInput(2) as Float))
    }
}