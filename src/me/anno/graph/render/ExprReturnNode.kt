package me.anno.graph.render

import me.anno.ecs.components.mesh.TypeValue
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Shader
import me.anno.graph.NodeOutput
import me.anno.graph.render.compiler.ExpressionRenderer
import me.anno.graph.types.flow.ReturnNode

class ExprReturnNode : ReturnNode(
    listOf(
        "Vector4f", "Color",
        "Int", "Width",
        "Int", "Height",
        "Int", "Channels",
        "Int", "Samples",
        "Bool", "Apply Tone Mapping"
    )
), ExpressionRenderer {

    init {
        init()
    }

    override var shader: Shader? = null
    override var buffer: Framebuffer? = null
    override var typeValues: HashMap<String, TypeValue>? = null
    override fun execute(): NodeOutput? {
        throw ReturnThrowable(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        shader?.destroy()
        buffer?.destroy()
    }
}