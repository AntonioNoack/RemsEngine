@file:Suppress("unused")

package me.anno.graph.visual.scalar

import me.anno.graph.visual.EnumNode
import me.anno.graph.visual.render.compiler.GLSLFuncNode

val dataL1 = MathNodeData(
    IntMathsUnary.entries,
    listOf("Long"), "Long",
    IntMathsUnary::id, IntMathsUnary::glsl
)

class MathL1Node : MathNode<IntMathsUnary>(dataL1), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, type.long(getInput(0) as Long))
    }
}

val dataL2 = MathNodeData(
    IntMathsBinary.entries,
    listOf("Long", "Long"), "Long",
    IntMathsBinary::id, IntMathsBinary::glsl
)

class MathL2Node : MathNode<IntMathsBinary>(dataL2), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, type.long(getInput(0) as Long, getInput(1) as Long))
    }
}

val dataL3 = MathNodeData(
    IntMathsTernary.entries,
    listOf("Long", "Long", "Long"), "Long",
    IntMathsTernary::id, IntMathsTernary::glsl
)

class MathL3Node : MathNode<IntMathsTernary>(dataL3), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, type.long(getInput(0) as Long, getInput(1) as Long, getInput(2) as Long))
    }
}