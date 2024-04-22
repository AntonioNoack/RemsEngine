package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode

val dataL1 = MathNode.MathNodeData(
    IntMathsUnary.entries,
    listOf("Long"), "Long",
    { it.id }, { it.glsl }
)

class MathL1Node : MathNode<IntMathsUnary>(dataL1), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, type.long(getInput(0) as Long))
    }
}

val dataL2 = MathNode.MathNodeData(
    IntMathsBinary.entries,
    listOf("Long", "Long"), "Long",
    { it.id }, { it.glsl }
)

class MathL2Node : MathNode<IntMathsBinary>(dataL2), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, type.long(getInput(0) as Long, getInput(1) as Long))
    }
}

val dataL3 = MathNode.MathNodeData(
    IntMathsTernary.entries,
    listOf("Long", "Long", "Long"), "Long",
    { it.id }, { it.glsl }
)

class MathL3Node : MathNode<IntMathsTernary>(dataL3), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, type.long(getInput(0) as Long, getInput(1) as Long, getInput(2) as Long))
    }
}