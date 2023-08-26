package me.anno.graph.types.flow.vector

import me.anno.graph.types.flow.maths.FloatMathsBinary
import me.anno.graph.types.flow.maths.FloatMathsTernary
import me.anno.graph.types.flow.maths.FloatMathsUnary
import me.anno.graph.types.flow.maths.MathNode
import org.joml.Vector2f

private val dataF12 = MathNode.MathNodeData(
    FloatMathsUnary.supportedUnaryVecTypes,
    listOf("Vector2f"), "Vector2f",
    { it.id }, { it.glsl }
)

class MathF12Node : MathNode<FloatMathsUnary>(dataF12) {
    override fun compute() {
        val a = getInput(0) as Vector2f
        setOutput(0, Vector2f(type.float(a.x), type.float(a.y)))
    }
}

private val dataF22 = MathNode.MathNodeData(
    FloatMathsBinary.values(),
    listOf("Vector2f", "Vector2f"), "Vector2f",
    { it.id }, { it.glsl }
)

class MathF22Node : MathNode<FloatMathsBinary>(dataF22) {
    override fun compute() {
        val a = getInput(0) as Vector2f
        val b = getInput(1) as Vector2f
        setOutput(0, Vector2f(type.float(a.x, b.x), type.float(a.y, b.y)))
    }
}

private val dataF32 = MathNode.MathNodeData(
    FloatMathsTernary.values(),
    listOf("Vector2f", "Vector2f", "Vector2f"), "Vector2f",
    { it.id }, { it.glsl }
)

class MathF32Node : MathNode<FloatMathsTernary>(dataF32) {
    override fun compute() {
        val a = getInput(0) as Vector2f
        val b = getInput(1) as Vector2f
        val c = getInput(2) as Vector2f
        setOutput(0, Vector2f(type.float(a.x, b.x, c.x), type.float(a.y, b.y, c.y)))
    }
}