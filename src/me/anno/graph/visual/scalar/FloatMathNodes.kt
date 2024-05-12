package me.anno.graph.visual.scalar

import me.anno.graph.visual.FlowGraphNodeUtils.getDoubleInput
import me.anno.graph.visual.node.Node
import me.anno.utils.structures.maps.LazyMap

private val types = "Float,Double".split(',')

private fun Node.setOutputI(v: Double) {
    setOutput(0, if (outputs[0].type == "Float") v.toFloat() else v)
}

private val dataD1 = LazyMap { type: String ->
    MathNodeData(
        FloatMathUnary.entries,
        listOf(type), type,
        FloatMathUnary::id, FloatMathUnary::glsl
    )
}

class MathF1Node : TypedMathNode<FloatMathUnary>(dataD1, types) {
    override fun compute() {
        setOutputI(enumType.double(getDoubleInput(0)))
    }
}

private val dataD2 = LazyMap { type: String ->
    MathNodeData(
        FloatMathBinary.entries,
        listOf(type, type), type,
        FloatMathBinary::id, FloatMathBinary::glsl
    )
}

class MathF2Node : TypedMathNode<FloatMathBinary>(dataD2, types) {
    override fun compute() {
        setOutputI(enumType.f64(getDoubleInput(0), getDoubleInput(1)))
    }
}

private val dataD3 = LazyMap { type: String ->
    MathNodeData(
        FloatMathTernary.entries,
        listOf(type, type, type), type,
        FloatMathTernary::id, FloatMathTernary::glsl
    )
}

class MathF3Node : TypedMathNode<FloatMathTernary>(dataD3, types) {
    override fun compute() {
        setOutputI(enumType.f64(getDoubleInput(0), getDoubleInput(1), getDoubleInput(2)))
    }
}