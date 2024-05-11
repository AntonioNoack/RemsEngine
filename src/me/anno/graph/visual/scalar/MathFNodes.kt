@file:Suppress("unused")

package me.anno.graph.visual.scalar

import me.anno.graph.visual.EnumNode
import me.anno.graph.visual.FlowGraphNodeUtils.getFloatInput
import me.anno.graph.visual.render.compiler.GLSLFuncNode

val dataF1 = MathNodeData(
    FloatMathsUnary.entries,
    listOf("Float"), "Float",
    FloatMathsUnary::id, FloatMathsUnary::glsl
)

class MathF1Node : MathNode<FloatMathsUnary>(dataF1), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, type.float(getFloatInput(0)))
    }
}

val dataF2 = MathNodeData(
    FloatMathsBinary.entries,
    listOf("Float", "Float"), "Float",
    FloatMathsBinary::id, FloatMathsBinary::glsl
)

class MathF2Node : MathNode<FloatMathsBinary>(dataF2), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, type.float(getFloatInput(0), getFloatInput(1)))
    }
}

val dataF3 = MathNodeData(
    FloatMathsTernary.entries,
    listOf("Float", "Float", "Float"), "Float",
    FloatMathsTernary::id, FloatMathsTernary::glsl
)

class MathF3Node : MathNode<FloatMathsTernary>(dataF3), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, type.float(getFloatInput(0), getFloatInput(1), getFloatInput(2)))
    }
}