package me.anno.graph.render.effects

import me.anno.ecs.components.shaders.effects.Bloom
import me.anno.gpu.texture.Texture2D
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode

class BloomNode : ActionNode(
    "Bloom",
    listOf(
        "Float", "Offset",
        "Float", "Strength",
        "Bool", "Apply Tone Mapping",
        "Texture", "Illuminated",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 3.0f) // offset
        setInput(2, 0.5f) // strength
        setInput(3, false) // apply tone mapping
    }

    override fun executeAction() {

        val offset = getInput(1) as Float
        val strength = getInput(2) as Float
        val applyToneMapping = getInput(3) == true
        val color = ((getInput(4) as? Texture)?.tex as? Texture2D) ?: return

        val result = Bloom.bloom2(color, offset, strength, applyToneMapping)
        setOutput(Texture(result.getTexture0()), 1)

    }
}