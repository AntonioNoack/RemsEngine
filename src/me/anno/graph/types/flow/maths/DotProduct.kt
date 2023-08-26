package me.anno.graph.types.flow.maths

import me.anno.graph.types.flow.ValueNode
import org.joml.*

class DotProductF2 : ValueNode("Vector2f Dot", listOf("Vector2f", "A", "Vector2f", "B"), "Float") {
    override fun compute() {
        val a = getInput(0) as Vector2f
        val b = getInput(1) as Vector2f
        setOutput(0, a.dot(b))
    }
}

class DotProductF3 : ValueNode("Vector3f Dot", listOf("Vector3f", "A", "Vector3f", "B"), "Float") {
    override fun compute() {
        val a = getInput(0) as Vector3f
        val b = getInput(1) as Vector3f
        setOutput(0, a.dot(b))
    }
}

class DotProductF4 : ValueNode("Vector4f Dot", listOf("Vector4f", "A", "Vector4f", "B"), "Float") {
    override fun compute() {
        val a = getInput(0) as Vector4f
        val b = getInput(1) as Vector4f
        setOutput(0, a.dot(b))
    }
}

class DotProductD2 : ValueNode("Vector2d Dot", listOf("Vector2d", "A", "Vector2d", "B"), "Double") {
    override fun compute() {
        val a = getInput(0) as Vector2d
        val b = getInput(1) as Vector2d
        setOutput(0, a.dot(b))
    }
}

class DotProductD3 : ValueNode("Vector3d Dot", listOf("Vector3d", "A", "Vector3d", "B"), "Double") {
    override fun compute() {
        val a = getInput(0) as Vector3d
        val b = getInput(1) as Vector3d
        setOutput(0, a.dot(b))
    }
}

class DotProductD4 : ValueNode("Vector4d Dot", listOf("Vector4d", "A", "Vector4d", "B"), "Double") {
    override fun compute() {
        val a = getInput(0) as Vector4d
        val b = getInput(1) as Vector4d
        setOutput(0, a.dot(b))
    }
}