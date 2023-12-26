package me.anno.graph.types.flow.vector

import me.anno.graph.types.flow.maths.FloatMathsBinary
import me.anno.graph.types.flow.maths.FloatMathsTernary
import me.anno.graph.types.flow.maths.FloatMathsUnary
import me.anno.graph.types.flow.maths.MathNode
import org.joml.Vector4f

private val dataF14 = MathNode.MathNodeData(
    FloatMathsUnary.supportedUnaryVecTypes,
    listOf("Vector4f"), "Vector4f",
    { it.id }, { it.glsl }
)

class MathF14Node : MathNode<FloatMathsUnary>(dataF14) {
    override fun compute() {
        val a = getInput(0) as Vector4f
        setOutput(0, Vector4f(type.float(a.x), type.float(a.y), type.float(a.z), type.float(a.w)))
    }
}

private val dataF24 = MathNode.MathNodeData(
    FloatMathsBinary.entries,
    listOf("Vector4f", "Vector4f"), "Vector4f",
    { it.id }, { it.glsl }
)

class MathF24Node : MathNode<FloatMathsBinary>(dataF24) {
    override fun compute() {
        val a = getInput(0) as Vector4f
        val b = getInput(1) as Vector4f
        setOutput(0, Vector4f(type.float(a.x, b.x), type.float(a.y, b.y), type.float(a.z, b.z), type.float(a.w, b.w)))
    }
}

private val dataF34 = MathNode.MathNodeData(
    FloatMathsTernary.entries,
    listOf("Vector4f", "Vector4f", "Vector4f"), "Vector4f",
    { it.id }, { it.glsl }
)

class MathF34Node : MathNode<FloatMathsTernary>(dataF34) {
    override fun compute() {
        val a = getInput(0) as Vector4f
        val b = getInput(1) as Vector4f
        val c = getInput(2) as Vector4f
        setOutput(
            0, Vector4f(
                type.float(a.x, b.x, c.x), type.float(a.y, b.y, c.y),
                type.float(a.z, b.z, c.z), type.float(a.w, b.w, c.w)
            )
        )
    }
}