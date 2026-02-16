package me.anno.graph.visual.render.effects

import me.anno.ecs.systems.GlobalSettings
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.shader.effects.ScreenSpaceAmbientOcclusion
import me.anno.gpu.texture.TextureLib.normalTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.isZWMapping
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texOrNull

/**
 * Node for Screen-Space Global Illumination
 * */
class SSGINode : TimedRenderingNode(
    "SSGI", listOf(
        "Texture", "Normal",
        "Texture", "Depth",
        "Texture", "Diffuse",
        "Texture", "Reflectivity",
        "Texture", "Illuminated",
    ), listOf(
        "Texture", "Illuminated"
    )
) {
    init {
        description = "Screen-Space Global Illumination"
    }

    override fun executeAction() {

        val settings = GlobalSettings[SSGISettings::class]

        val ssaoSamples = settings.numSamples
        val strength = settings.strength
        val radiusScale = settings.radiusScale
        val blur = settings.blur

        val normal = getInput(1) as? Texture ?: return finish()
        val normalZW = normal.isZWMapping
        val normalT = normal.texOrNull ?: normalTexture

        val depthT = (getInput(2) as? Texture)
        val depthTT = depthT.texOrNull ?: return finish()

        val colorTT = getTextureInput(3) ?: whiteTexture

        val reflectT = (getInput(4) as? Texture)
        val reflectTT = reflectT.texOrNull ?: whiteTexture
        val reflectTM = reflectT.mask1Index

        val illumTT = getTextureInput(5) ?: return finish()

        val data = ScreenSpaceAmbientOcclusion.SSGIData(illumTT, colorTT, reflectTT, reflectTM)

        timeRendering(name, timer) {
            val transform = RenderState.cameraMatrix
            val result = ScreenSpaceAmbientOcclusion.compute(
                data, depthTT, depthT.mask1Index, normalT, normalZW,
                transform, strength, radiusScale, ssaoSamples, blur, false
            )
            finish(Texture.texture(result, 0, "rgb", null))
        }
    }
}
