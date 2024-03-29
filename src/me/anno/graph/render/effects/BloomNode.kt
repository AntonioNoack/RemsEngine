package me.anno.graph.render.effects

import me.anno.gpu.shader.effects.Bloom
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.FlowGraphNodeUtils.getBoolInput
import me.anno.graph.types.flow.FlowGraphNodeUtils.getFloatInput
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

        val offset = getFloatInput(1)
        val strength = getFloatInput(2)
        val applyToneMapping = getBoolInput(3)
        val color = ((getInput(4) as? Texture)?.tex as? Texture2D) ?: return

        val target = if (applyToneMapping) TargetType.UInt8x4 else TargetType.Float16x4
        val result = FBStack[name, color.width, color.height, target, 1, DepthBufferType.NONE]
        useFrame(result) {
            Bloom.bloom(color, offset, strength, applyToneMapping)
        }
        setOutput(1, Texture(result.getTexture0()))
    }
}