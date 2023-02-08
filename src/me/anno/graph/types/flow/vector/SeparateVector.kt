package me.anno.graph.types.flow.vector

import me.anno.graph.types.flow.ValueNode
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class SeparateVector2f : ValueNode("Separate Vector2f", inputs, outputs) {

    init {
        setInput(0, Vector2f())
    }

    override fun compute() {
        val v = getInput(0) as Vector2f
        setOutput(v.x, 0)
        setOutput(v.y, 1)
    }

    companion object {
        val inputs = listOf("Vector2f", "Vector")
        val outputs = listOf("Float", "X", "Float", "Y")
    }

}

class SeparateVector3f : ValueNode("Separate Vector3f", inputs, outputs) {

    init {
        setInput(0, Vector3f())
    }

    override fun compute() {
        val v = getInput(0) as Vector3f
        setOutput(v.x, 0)
        setOutput(v.y, 1)
        setOutput(v.z, 2)
    }

    companion object {
        val inputs = listOf("Vector3f", "Vector")
        val outputs = listOf("Float", "X", "Float", "Y", "Float", "Z")
    }

}

class SeparateVector4f : ValueNode("Separate Vector4f", inputs, outputs) {

    init {
        setInput(0, Vector4f())
    }

    override fun compute() {
        val v = getInput(0) as Vector4f
        setOutput(v.x, 0)
        setOutput(v.y, 1)
        setOutput(v.z, 2)
        setOutput(v.w, 3)
    }

    companion object {
        val inputs = listOf("Vector4f", "Vector")
        val outputs = listOf("Float", "X", "Float", "Y", "Float", "Z", "Float", "W")
    }

}