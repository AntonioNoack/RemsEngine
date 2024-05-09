@file:Suppress("unused")

package me.anno.graph.visual.vector

import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.FlowGraphNodeUtils.getFloatInput
import me.anno.graph.visual.render.compiler.GLSLFuncNode
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class NormalizeNode2 : CalculationNode(
    "Vector2f Normalize",
    listOf("Vector2f", "Vector", "Float", "Length"),
    "Vector2f"
), GLSLFuncNode {

    init {
        setInput(0, Vector2f())
        setInput(1, 1f)
    }

    override fun calculate(): Vector2f {
        val v = getInput(0) as Vector2f
        val l = getFloatInput(1)
        return v.normalize(l, Vector2f())
    }

    override fun getShaderFuncName(outputIndex: Int) = "norm2"
    override fun defineShaderFunc(outputIndex: Int): String {
        return "(vec2 v, float l){return normalize(v)*l;}"
    }
}


class NormalizeNode3 : CalculationNode(
    "Vector3f Normalize",
    listOf("Vector3f", "Vector", "Float", "Length"),
    "Vector3f"
), GLSLFuncNode {

    init {
        setInput(0, Vector3f())
        setInput(1, 1f)
    }

    override fun calculate(): Vector3f {
        val v = getInput(0) as Vector3f
        val l = getFloatInput(1)
        return v.normalize(l, Vector3f())
    }

    override fun getShaderFuncName(outputIndex: Int) = "norm3"
    override fun defineShaderFunc(outputIndex: Int): String {
        return "(vec3 v, float l){return normalize(v)*l;}"
    }
}

class NormalizeNode4 : CalculationNode(
    "Vector4f Normalize",
    listOf("Vector4f", "Vector", "Float", "Length"),
    "Vector4f"
), GLSLFuncNode {

    init {
        setInput(0, Vector4f())
        setInput(1, 1f)
    }

    override fun calculate(): Vector4f {
        val v = getInput(0) as Vector4f
        val l = getFloatInput(1)
        return v.normalize(l, Vector4f())
    }

    override fun getShaderFuncName(outputIndex: Int) = "norm4"
    override fun defineShaderFunc(outputIndex: Int): String {
        return "(vec4 v, float l){return normalize(v)*l;}"
    }
}