package me.anno.graph.visual.scalar

import me.anno.graph.visual.EnumNode
import me.anno.graph.visual.render.compiler.GLSLFuncNode

val dataD1 = MathNodeData(
    FloatMathsUnary.entries,
    listOf("Double"), "Double",
    FloatMathsUnary::id, FloatMathsUnary::glsl
)

class MathD1Node : MathNode<FloatMathsUnary>(dataD1), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, type.double(getInput(0) as Double))
    }
}

val dataD2 = MathNodeData(
    FloatMathsBinary.entries,
    listOf("Double", "Double"), "Double",
    FloatMathsBinary::id, FloatMathsBinary::glsl
)

class MathD2Node : MathNode<FloatMathsBinary>(dataD2), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, type.double(getInput(0) as Double, getInput(1) as Double))
    }
}

val dataD3 = MathNodeData(
    FloatMathsTernary.entries,
    listOf("Double", "Double", "Double"), "Double",
    FloatMathsTernary::id, FloatMathsTernary::glsl
)

class MathD3Node : MathNode<FloatMathsTernary>(dataD3), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(
            0, type.double(
                getInput(0) as Double,
                getInput(1) as Double,
                getInput(2) as Double
            )
        )
    }
}