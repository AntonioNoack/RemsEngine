package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode
import me.anno.graph.types.FlowGraph

val dataI1 = MathNode.MathNodeData(
    IntMathsUnary.values(),
    listOf("Int"), "Int",
    { it.id }, { it.glsl }
)

class MathI1Node : MathNode<IntMathsUnary>(dataI1), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        setOutput(type.int(getInput(graph, 0) as Int), 0)
    }
}

val dataI2 = MathNode.MathNodeData(
    IntMathsBinary.values(),
    listOf("Int", "Int"), "Int",
    { it.id }, { it.glsl }
)

class MathI2Node : MathNode<IntMathsBinary>(dataI2), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        setOutput(type.int(getInput(graph, 0) as Int, getInput(graph, 1) as Int), 0)
    }
}

val dataI3 = MathNode.MathNodeData(
    IntMathsTernary.values(),
    listOf("Int", "Int", "Int"), "Int",
    { it.id }, { it.glsl }
)

class MathI3Node : MathNode<IntMathsTernary>(dataI3), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        setOutput(type.int(getInput(graph, 0) as Int, getInput(graph, 1) as Int, getInput(graph, 2) as Int), 0)
    }
}