package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode
import me.anno.graph.types.FlowGraph
import org.joml.Vector2f

val dataF12 = MathNode.MathNodeData(
    FloatMathsUnary.values(),
    listOf("Vector2f"), "Vector2f",
    { it.id }, { it.glsl }
)

class MathF12Node : MathNode<FloatMathsUnary>(dataF12), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        val a = getInput(graph, 0) as Vector2f
        setOutput(Vector2f(type.float(a.x), type.float(a.y)))
    }
}

val dataF22 = MathNode.MathNodeData(
    FloatMathsBinary.values(),
    listOf("Vector2f", "Vector2f"), "Vector2f",
    { it.id }, { it.glsl }
)

class MathF22Node : MathNode<FloatMathsBinary>(dataF22), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        val a = getInput(graph, 0) as Vector2f
        val b = getInput(graph, 1) as Vector2f
        setOutput(Vector2f(type.float(a.x, b.x), type.float(a.y, b.y)))
    }
}

val dataF32 = MathNode.MathNodeData(
    FloatMathsTernary.values(),
    listOf("Vector2f", "Vector2f", "Vector2f"), "Vector2f",
    { it.id }, { it.glsl }
)

class MathF32Node : MathNode<FloatMathsTernary>(dataF32), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        val a = getInput(graph, 0) as Vector2f
        val b = getInput(graph, 1) as Vector2f
        val c = getInput(graph, 2) as Vector2f
        setOutput(Vector2f(type.float(a.x, b.x, c.x), type.float(a.y, b.y, c.y)))
    }
}