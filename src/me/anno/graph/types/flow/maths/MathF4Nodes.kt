package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode
import me.anno.graph.types.FlowGraph
import org.joml.Vector4f

val dataF14 = MathNode.MathNodeData(
    FloatMathsUnary.supportedUnaryVecTypes,
    listOf("Vector4f"), "Vector4f",
    { it.id }, { it.glsl }
)

class MathF14Node : MathNode<FloatMathsUnary>(dataF14), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        val a = getInput(graph, 0) as Vector4f
        setOutput(Vector4f(type.float(a.x), type.float(a.y), type.float(a.z), type.float(a.w)))
    }
}

val dataF24 = MathNode.MathNodeData(
    FloatMathsBinary.values(),
    listOf("Vector4f", "Vector4f"), "Vector4f",
    { it.id }, { it.glsl }
)

class MathF24Node : MathNode<FloatMathsBinary>(dataF24), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        val a = getInput(graph, 0) as Vector4f
        val b = getInput(graph, 1) as Vector4f
        setOutput(Vector4f(type.float(a.x, b.x), type.float(a.y, b.y), type.float(a.z, b.z), type.float(a.w, b.w)))
    }
}

val dataF34 = MathNode.MathNodeData(
    FloatMathsTernary.values(),
    listOf("Vector4f", "Vector4f", "Vector4f"), "Vector4f",
    { it.id }, { it.glsl }
)

class MathF34Node : MathNode<FloatMathsTernary>(dataF34), EnumNode, GLSLExprNode {
    override fun compute(graph: FlowGraph) {
        val a = getInput(graph, 0) as Vector4f
        val b = getInput(graph, 1) as Vector4f
        val c = getInput(graph, 2) as Vector4f
        setOutput(
            Vector4f(
                type.float(a.x, b.x, c.x), type.float(a.y, b.y, c.y),
                type.float(a.z, b.z, c.z), type.float(a.w, b.w, c.w)
            )
        )
    }
}