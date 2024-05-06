package me.anno.graph.render

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.gpu.shader.Shader
import me.anno.graph.NodeOutput
import me.anno.graph.render.compiler.ExpressionRenderer
import me.anno.graph.types.flow.ReturnNode

class ExprReturnNode : ReturnNode(
    listOf(
        "Vector4f", "Illuminated",
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
    override var typeValues: HashMap<String, TypeValue>? = null
    override fun execute(): NodeOutput? {
        throw ReturnThrowable(this)
    }

    override fun destroy() {
        super.destroy()
        shader?.destroy()
    }
}