package me.anno.graph.render.effects

import me.anno.ecs.components.shaders.effects.Bloom
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
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
        setInput(1, 0.0f) // offset
        setInput(2, 0.001f) // strength
        setInput(3, false) // apply tone mapping
    }

    override fun executeAction() {

        val offset = getInput(1) as Float
        val strength = getInput(2) as Float
        val applyToneMapping = getInput(3) == true
        val color = ((getInput(4) as? Texture)?.tex as? Texture2D) ?: return

        val target = if (applyToneMapping) TargetType.UByteTarget4 else TargetType.FP16Target4
        val result = FBStack["bloom", color.width, color.height, target, 1, false]
        useFrame(result) {
            Bloom.bloom(color, offset, strength, applyToneMapping)
        }
        setOutput(1, Texture(result.getTexture0()))
    }
}