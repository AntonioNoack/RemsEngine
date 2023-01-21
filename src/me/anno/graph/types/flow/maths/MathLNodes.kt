package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode
import me.anno.graph.types.FlowGraph

val dataL1 = MathNode.MathNodeData(
    IntMathsUnary.values(),
    listOf("Long"), "Long",
    { it.id }, { it.glsl }
)

class MathL1Node : MathNode<IntMathsUnary>(dataL1), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        setOutput(type.long(getInput(graph, 0) as Long))
    }
}

val dataL2 = MathNode.MathNodeData(
    IntMathsBinary.values(),
    listOf("Long", "Long"), "Long",
    { it.id }, { it.glsl }
)

class MathL2Node : MathNode<IntMathsBinary>(dataL2), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        setOutput(type.long(getInput(graph, 0) as Long, getInput(graph, 1) as Long))
    }
}

val dataL3 = MathNode.MathNodeData(
    IntMathsTernary.values(),
    listOf("Long", "Long", "Long"), "Long",
    { it.id }, { it.glsl }
)

class MathL3Node : MathNode<IntMathsTernary>(dataL3), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        setOutput(type.long(getInput(graph, 0) as Long, getInput(graph, 1) as Long, getInput(graph, 2) as Long))
    }
}