package me.anno.graph.render.scene

import me.anno.graph.types.flow.CalculationNode
import me.anno.graph.types.flow.maths.GLSLExprNode
import org.joml.Vector2f

open class ShaderConstNode(name: String, val glsl: String) :
    CalculationNode(name, emptyList(), "Vector2f"), GLSLExprNode {
    override fun calculate() = Vector2f(0f)
    override fun defineShaderFunc(outputIndex: Int) = null
    override fun getShaderFuncName(outputIndex: Int) = glsl
}

class UViNode : ShaderConstNode("UVi", "gl_FragCoord")
class UVNode : ShaderConstNode("UV", "uv")
