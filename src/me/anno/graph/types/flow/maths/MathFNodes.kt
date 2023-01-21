package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode
import me.anno.graph.types.FlowGraph

val dataF1 = MathNode.MathNodeData(
    FloatMathsUnary.values(),
    listOf("Float"), "Float",
    { it.id }, { it.glsl }
)

class MathF1Node : MathNode<FloatMathsUnary>(dataF1), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        setOutput(type.float(getInput(graph, 0) as Float), 0)
    }
}

val dataF2 = MathNode.MathNodeData(
    FloatMathsBinary.values(),
    listOf("Float", "Float"), "Float",
    { it.id }, { it.glsl }
)

class MathF2Node : MathNode<FloatMathsBinary>(dataF2), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        setOutput(type.float(getInput(graph, 0) as Float, getInput(graph, 1) as Float), 0)
    }
}

val dataF3 = MathNode.MathNodeData(
    FloatMathsTernary.values(),
    listOf("Float", "Float", "Float"), "Float",
    { it.id }, { it.glsl }
)

class MathF3Node : MathNode<FloatMathsTernary>(dataF3), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        setOutput(type.float(getInput(graph, 0) as Float, getInput(graph, 1) as Float, getInput(graph, 2) as Float), 0)
    }
}