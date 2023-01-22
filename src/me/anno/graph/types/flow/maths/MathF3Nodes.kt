package me.anno.graph.types.flow.maths

import me.anno.graph.types.FlowGraph
import org.joml.Vector3f

val dataF13 = MathNode.MathNodeData(
    FloatMathsUnary.supportedUnaryVecTypes,
    listOf("Vector3f"), "Vector3f",
    { it.id }, { it.glsl }
)

class MathF13Node : MathNode<FloatMathsUnary>(dataF13) {
    override fun compute(graph: FlowGraph) {
        val a = getInput(graph, 0) as Vector3f
        setOutput(Vector3f(type.float(a.x), type.float(a.y), type.float(a.z)))
    }
}

val dataF23 = MathNode.MathNodeData(
    FloatMathsBinary.values(),
    listOf("Vector3f", "Vector3f"), "Vector3f",
    { it.id }, { it.glsl }
)

class MathF23Node : MathNode<FloatMathsBinary>(dataF23) {
    override fun compute(graph: FlowGraph) {
        val a = getInput(graph, 0) as Vector3f
        val b = getInput(graph, 1) as Vector3f
        setOutput(Vector3f(type.float(a.x, b.x), type.float(a.y, b.y), type.float(a.z, b.z)))
    }
}

val dataF33 = MathNode.MathNodeData(
    FloatMathsTernary.values(),
    listOf("Vector3f", "Vector3f", "Vector3f"), "Vector3f",
    { it.id }, { it.glsl }
)

class MathF33Node : MathNode<FloatMathsTernary>(dataF33) {
    override fun compute(graph: FlowGraph) {
        val a = getInput(graph, 0) as Vector3f
        val b = getInput(graph, 1) as Vector3f
        val c = getInput(graph, 2) as Vector3f
        setOutput(Vector3f(type.float(a.x, b.x, c.x), type.float(a.y, b.y, c.y), type.float(a.z, b.z, c.z)))
    }
}