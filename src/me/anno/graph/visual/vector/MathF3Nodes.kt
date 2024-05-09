package me.anno.graph.visual.vector

import me.anno.graph.visual.scalar.FloatMathsBinary
import me.anno.graph.visual.scalar.FloatMathsTernary
import me.anno.graph.visual.scalar.FloatMathsUnary
import me.anno.graph.visual.scalar.MathNode
import org.joml.Vector3f

private val dataF13 = MathNode.MathNodeData(
    FloatMathsUnary.supportedUnaryVecTypes,
    listOf("Vector3f"), "Vector3f",
    { it.id }, { it.glsl }
)

class MathF13Node : MathNode<FloatMathsUnary>(dataF13) {
    override fun compute() {
        val a = getInput(0) as Vector3f
        setOutput(0, Vector3f(type.float(a.x), type.float(a.y), type.float(a.z)))
    }
}

private val dataF23 = MathNode.MathNodeData(
    FloatMathsBinary.entries,
    listOf("Vector3f", "Vector3f"), "Vector3f",
    { it.id }, { it.glsl }
)

class MathF23Node : MathNode<FloatMathsBinary>(dataF23) {
    override fun compute() {
        val a = getInput(0) as Vector3f
        val b = getInput(1) as Vector3f
        setOutput(0, Vector3f(type.float(a.x, b.x), type.float(a.y, b.y), type.float(a.z, b.z)))
    }
}

private val dataF33 = MathNode.MathNodeData(
    FloatMathsTernary.entries,
    listOf("Vector3f", "Vector3f", "Vector3f"), "Vector3f",
    { it.id }, { it.glsl }
)

class MathF33Node : MathNode<FloatMathsTernary>(dataF33) {
    override fun compute() {
        val a = getInput(0) as Vector3f
        val b = getInput(1) as Vector3f
        val c = getInput(2) as Vector3f
        setOutput(0, Vector3f(type.float(a.x, b.x, c.x), type.float(a.y, b.y, c.y), type.float(a.z, b.z, c.z)))
    }
}