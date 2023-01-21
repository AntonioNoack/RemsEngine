package me.anno.graph.types.flow.maths

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ValueNode
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f

class CrossProductF2 : ValueNode("Vector2f Cross", listOf("Vector2f", "A", "Vector2f", "B"), "Float"), GLSLExprNode {
    override fun getShaderFuncName(outputIndex: Int) = "cross2d"
    override fun defineShaderFunc(outputIndex: Int) = "(vec2 a, vec2 b){return a.x*b.y-a.y*b.x;}"
    override fun compute(graph: FlowGraph) {
        val a = getInput(graph, 0) as Vector2f
        val b = getInput(graph, 1) as Vector2f
        setOutput(a.cross(b))
    }
}

class CrossProductF3 : ValueNode("Vector3f Cross", listOf("Vector3f", "A", "Vector3f", "B"), "Vector3f") {
    override fun compute(graph: FlowGraph) {
        val a = getInput(graph, 0) as Vector3f
        val b = getInput(graph, 1) as Vector3f
        setOutput(a.cross(b, Vector3f()))
    }
}

class CrossProductD2 : ValueNode("Vector2d Cross", listOf("Vector2d", "A", "Vector2d", "B"), "Double"), GLSLExprNode {
    override fun getShaderFuncName(outputIndex: Int) = "cross2d"
    override fun defineShaderFunc(outputIndex: Int) = "(vec2 a, vec2 b){return a.x*b.y-a.y*b.x;}"
    override fun compute(graph: FlowGraph) {
        val a = getInput(graph, 0) as Vector2d
        val b = getInput(graph, 1) as Vector2d
        setOutput(a.cross(b))
    }
}

class CrossProductD3 : ValueNode("Vector3f Cross", listOf("Vector3d", "A", "Vector3d", "B"), "Vector3d") {
    override fun compute(graph: FlowGraph) {
        val a = getInput(graph, 0) as Vector3d
        val b = getInput(graph, 1) as Vector3d
        setOutput(a.cross(b, Vector3d()))
    }
}
