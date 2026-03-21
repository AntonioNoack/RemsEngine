package me.anno.graph.visual.render

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.gpu.shader.Shader
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.ExpressionRenderer
import me.anno.graph.visual.render.effects.ToneMappingNode.Companion.EXPOSURE_NAME

class RenderReturnNode : ReturnNode(
    listOf(
        "Vector4f", "Illuminated",
        "Int", "Width",
        "Int", "Height",
        "Int", "Channels",
        "Int", "Samples",
        "Float", EXPOSURE_NAME, // set to a value of 0 to disable tone-mapping
        "Texture", "Depth",
    )
), ExpressionRenderer {

    init {
        init()
    }

    override var shader: Shader? = null
    override var typeValues: HashMap<String, TypeValue>? = null

    override fun destroy() {
        super.destroy()
        shader?.destroy()
    }
}