package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode
import me.anno.graph.types.FlowGraph

val dataD1 = MathNode.MathNodeData(
    FloatMathsUnary.values(),
    listOf("Double"), "Double",
    { it.id }, { it.glsl }
)

class MathD1Node : MathNode<FloatMathsUnary>(dataD1), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        setOutput(type.double(getInput(graph, 0) as Double))
    }
}

val dataD2 = MathNode.MathNodeData(
    FloatMathsBinary.values(),
    listOf("Double", "Double"), "Double",
    { it.id }, { it.glsl }
)

class MathD2Node : MathNode<FloatMathsBinary>(dataD2), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        setOutput(type.double(getInput(graph, 0) as Double, getInput(graph, 1) as Double))
    }
}

val dataD3 = MathNode.MathNodeData(
    FloatMathsTernary.values(),
    listOf("Double", "Double", "Double"), "Double",
    { it.id }, { it.glsl })

class MathD3Node : MathNode<FloatMathsTernary>(dataD3), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        setOutput(
            type.double(
                getInput(graph, 0) as Double,
                getInput(graph, 1) as Double,
                getInput(graph, 2) as Double
            )
        )
    }
}