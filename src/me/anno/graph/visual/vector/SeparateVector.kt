@file:Suppress("unused")

package me.anno.graph.visual.vector

import me.anno.graph.visual.ComputeNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLExprNode
import me.anno.graph.visual.render.compiler.GraphCompiler
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

interface SeparateVectorNode : GLSLExprNode {
    override fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node) {
        val c = n.outputs.indexOf(out)
        g.builder.append('(')
        g.expr(n.inputs[0]) // vector
        g.builder.append(").").append("xyzw"[c])
    }
}

class SeparateVector2f : ComputeNode("Separate Vector2f", inputs, outputs), SeparateVectorNode {

    init {
        setInput(0, Vector2f())
    }

    override fun compute() {
        val v = getInput(0) as Vector2f
        setOutput(0, v.x)
        setOutput(1, v.y)
    }

    companion object {
        val inputs = listOf("Vector2f", "Vector")
        val outputs = listOf("Float", "X", "Float", "Y")
    }
}

class SeparateVector3f : ComputeNode("Separate Vector3f", inputs, outputs), SeparateVectorNode {

    init {
        setInput(0, Vector3f())
    }

    override fun compute() {
        val v = getInput(0) as Vector3f
        setOutput(0, v.x)
        setOutput(1, v.y)
        setOutput(2, v.z)
    }

    companion object {
        val inputs = listOf("Vector3f", "Vector")
        val outputs = listOf("Float", "X", "Float", "Y", "Float", "Z")
    }
}

class SeparateVector4f : ComputeNode("Separate Vector4f", inputs, outputs), SeparateVectorNode {

    init {
        setInput(0, Vector4f(0f, 0f, 0f, 1f))
    }

    override fun compute() {
        val v = getInput(0) as Vector4f
        setOutput(0, v.x)
        setOutput(1, v.y)
        setOutput(2, v.z)
        setOutput(3, v.w)
    }

    companion object {
        val inputs = listOf("Vector4f", "Vector")
        val outputs = listOf("Float", "X", "Float", "Y", "Float", "Z", "Float", "W")
    }
}