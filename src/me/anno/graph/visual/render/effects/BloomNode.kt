package me.anno.graph.visual.render.effects

import me.anno.ecs.systems.GlobalSettings
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.effects.Bloom
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.texMSOrNull
import me.anno.graph.visual.render.Texture.Companion.texOrNull

class BloomNode : TimedRenderingNode(
    "Bloom",
    listOf(
        "Bool", "Apply Tone Mapping",
        "Texture", "Illuminated",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, false) // apply tone mapping
    }

    override fun executeAction() {

        val settings = GlobalSettings[BloomSettings::class]
        val applyToneMapping = getBoolInput(1)
        val colorT = getInput(2) as? Texture ?: return finish()
        val colorTT = colorT.texOrNull ?: return finish(colorT)
        val colorMT = if (applyToneMapping) colorT.texMSOrNull ?: colorTT else colorTT
        if (settings.strength <= 0f) return finish(colorT)

        timeRendering(name, timer) {
            val target = if (applyToneMapping) TargetType.UInt8x4 else TargetType.Float16x4
            val result = FBStack[name, colorTT.width, colorTT.height, target, 1, DepthBufferType.NONE]
            useFrame(result) {
                Bloom.bloom(colorTT, colorMT, settings.offset, settings.strength, applyToneMapping)
            }
            finish(result.getTexture0())
        }
    }
}