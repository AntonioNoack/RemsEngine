@file:Suppress("unused")

package me.anno.graph.visual.scalar

import me.anno.graph.visual.FlowGraphNodeUtils.getDoubleInput
import me.anno.utils.structures.maps.LazyMap

private val types = "Float,Double".split(',')

private val dataD1 = LazyMap { type: String ->
    MathNodeData(
        FloatMathsUnary.entries,
        listOf(type), type,
        FloatMathsUnary::id, FloatMathsUnary::glsl
    )
}

class MathF1Node : TypedMathNode<FloatMathsUnary>(dataD1, types) {
    override fun compute() {
        setOutput(0, enumType.double(getDoubleInput(0)))
    }
}

private val dataD2 = LazyMap { type: String ->
    MathNodeData(
        FloatMathsBinary.entries,
        listOf(type, type), type,
        FloatMathsBinary::id, FloatMathsBinary::glsl
    )
}

class MathF2Node : TypedMathNode<FloatMathsBinary>(dataD2, types) {
    override fun compute() {
        setOutput(0, enumType.double(getDoubleInput(0), getDoubleInput(1)))
    }
}

private val dataD3 = LazyMap { type: String ->
    MathNodeData(
        FloatMathsTernary.entries,
        listOf(type, type, type), type,
        FloatMathsTernary::id, FloatMathsTernary::glsl
    )
}

class MathF3Node : TypedMathNode<FloatMathsTernary>(dataD3, types) {
    override fun compute() {
        setOutput(
            0, enumType.calculate(
                getDoubleInput(0),
                getDoubleInput(1),
                getDoubleInput(2)
            )
        )
    }
}