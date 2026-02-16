package me.anno.graph.visual.render.effects

import me.anno.ecs.systems.GlobalSettings
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.shader.effects.ScreenSpaceAmbientOcclusion
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.normalTexture
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.isZWMapping
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texOrNull

class SSAONode : TimedRenderingNode(
    "SSAO",
    listOf(
        "Bool", "Blur",
        "Bool", "Inverse",
        "Texture", "Normal", // optional
        "Texture", "Depth",
    ), listOf("Texture", "Ambient Occlusion")
) {

    init {
        description = "Screen Space Ambient Occlusion"
        setInput(1, true) // blur
        setInput(2, false) // inverse
    }

    override fun executeAction() {

        val blur = getBoolInput(1)
        val inverse = getBoolInput(2)

        val normal = getInput(3) as? Texture ?: return finish()
        val normalZW = normal.isZWMapping
        val normalT = normal.texOrNull ?: normalTexture
        val depthT = (getInput(4) as? Texture) ?: return finish()
        val depthTT = depthT.texOrNull ?: return finish()

        val settings = GlobalSettings[SSAOSettings::class]
        if (!(settings.strength > 0f && settings.numSamples > 0 && settings.radiusScale > 0f)) {
            return finish()
        }

        timeRendering(name, timer) {
            val transform = RenderState.cameraMatrix
            val result = ScreenSpaceAmbientOcclusion.compute(
                null, depthTT, depthT.mask1Index, normalT, normalZW,
                transform, settings.strength, settings.radiusScale, settings.numSamples, blur, inverse
            )
            setOutput(1, Texture.texture(result, 0, "r", null))
        }
    }
}