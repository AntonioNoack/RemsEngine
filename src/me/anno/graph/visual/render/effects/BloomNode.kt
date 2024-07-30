package me.anno.graph.visual.render.effects

import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.effects.Bloom
import me.anno.graph.visual.render.Texture

class BloomNode : TimedRenderingNode(
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
        val colorT = getInput(4) as? Texture ?: return
        val colorTT = colorT.texOrNull ?: return
        val colorMT = if (applyToneMapping) colorT.texMSOrNull ?: colorTT else colorTT

        timeRendering(name, timer) {
            val target = if (applyToneMapping) TargetType.UInt8x4 else TargetType.Float16x4
            val result = FBStack[name, colorTT.width, colorTT.height, target, 1, DepthBufferType.NONE]
            useFrame(result) {
                Bloom.bloom(colorTT, colorMT, offset, strength, applyToneMapping)
            }
            setOutput(1, Texture(result.getTexture0()))
        }
    }
}