package me.anno.graph.visual.render.compiler

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.gpu.shader.Shader
import me.anno.graph.visual.actions.ActionNode

// this is a simple method for rendering, which does not allow loops
// todo alternatives, which are CubeBufferNode, Buffer3DNode, BufferArrayNode, ...

class ShaderExprNode : ActionNode(
    "Shader Expression",
    // num channels, num samples, todo target type, depth mode
    // todo blend mode/conditional clear
    // todo multi-sampled/single-sampled output
    listOf(
        "Vector4f", "Data",
        "Int", "Width",
        "Int", "Height",
        "Int", "Channels",
        "Int", "Samples",
    ),
    listOf("Texture", "Result")
), ExpressionRenderer {

    init {
        init()
    }

    override var shader: Shader? = null
    override var typeValues: HashMap<String, TypeValue>? = null
    override fun executeAction() {
        setOutput(1, render(true))
    }

    override fun destroy() {
        super.destroy()
        shader?.destroy()
        shader = null
    }
}