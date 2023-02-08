package me.anno.graph.render.scene

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode
import me.anno.graph.types.flow.maths.GLSLExprNode

open class ShaderConstNode(name: String, val glsl: String) : CalculationNode(name), GLSLExprNode {
    override fun calculate() = throw NotImplementedError("Only available in shader")
    override fun defineShaderFunc(outputIndex: Int) = null
    override fun getShaderFuncName(outputIndex: Int) = glsl
}

class UViNode : ShaderConstNode("UVi", "gl_FragCoord")
class UVNode : ShaderConstNode("UV", "uv")
