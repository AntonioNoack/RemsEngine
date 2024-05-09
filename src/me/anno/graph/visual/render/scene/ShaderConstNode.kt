package me.anno.graph.visual.render.scene

import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.render.compiler.GLSLConstNode
import org.joml.Vector2f

open class ShaderConstNode(name: String, val type: String, val glsl: String) :
    CalculationNode(name, emptyList(), type), GLSLConstNode {
    override fun calculate() = Vector2f(0f)
    override fun getGLSLName(outputIndex: Int) = glsl
}

class UViNode : ShaderConstNode("UVi", "Vector2f", "gl_FragCoord")
class UVNode : ShaderConstNode("UV", "Vector2f", "uv")
