@file:Suppress("unused")

package me.anno.graph.visual.vector

import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.FlowGraphNodeUtils.getFloatInput
import me.anno.graph.visual.render.compiler.GLSLFuncNode
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class CombineVector2f : CalculationNode("Combine Vector2f", inputs, outputs), GLSLFuncNode {

    init {
        setInput(0, 0f)
        setInput(1, 0f)
    }

    override fun getShaderFuncName(outputIndex: Int): String = "vec2"
    override fun calculate(): Vector2f {
        val x = getFloatInput(0)
        val y = getFloatInput(1)
        return Vector2f(x, y)
    }

    companion object {
        val inputs = listOf("Float", "X", "Float", "Y")
        val outputs = listOf("Vector2f", "Vector")
    }

}

class CombineVector3f : CalculationNode("Combine Vector3f", inputs, outputs), GLSLFuncNode {

    init {
        setInput(0, 0f)
        setInput(1, 0f)
        setInput(2, 0f)
    }

    override fun getShaderFuncName(outputIndex: Int): String = "vec3"
    override fun calculate(): Vector3f {
        val x = getFloatInput(0)
        val y = getFloatInput(1)
        val z = getFloatInput(2)
        return Vector3f(x, y, z)
    }

    companion object {
        val inputs = listOf("Float", "X", "Float", "Y", "Float", "Z")
        val outputs = listOf("Vector3f", "Vector")
    }

}

class CombineVector4f : CalculationNode("Combine Vector4f", inputs, outputs), GLSLFuncNode {

    init {
        setInput(0, 0f)
        setInput(1, 0f)
        setInput(2, 0f)
        setInput(3, 1f)
    }

    override fun getShaderFuncName(outputIndex: Int): String = "vec4"
    override fun calculate(): Vector4f {
        val x = getFloatInput(0)
        val y = getFloatInput(1)
        val z = getFloatInput(2)
        val w = getFloatInput(3)
        return Vector4f(x, y, z, w)
    }

    companion object {
        val inputs = listOf("Float", "X", "Float", "Y", "Float", "Z", "Float", "W")
        val outputs = listOf("Vector4f", "Vector")
    }

}