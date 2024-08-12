package me.anno.graph.visual.render.effects

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.shader.effects.ScreenSpaceAmbientOcclusion
import me.anno.gpu.texture.TextureLib.normalTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.isZWMapping
import me.anno.graph.visual.render.Texture.Companion.mask
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.effects.SSAONode.Companion.fail

/**
 * Node for Screen-Space Global Illumination
 * */
class SSGINode : TimedRenderingNode(
    "SSGI", listOf(
        "Int", "SSGI Samples",
        "Float", "Strength",
        "Float", "Radius Scale",
        "Bool", "Blur",
        "Texture", "Normal",
        "Texture", "Depth",
        "Texture", "Diffuse",
        "Texture", "Roughness",
        "Texture", "Illuminated",
    ), listOf(
        "Texture", "Illuminated"
    )
) {
    init {
        description = "Screen-Space Global Illumination"
        setInput(1, 64) // samples
        setInput(2, 0.5f) // strength
        setInput(3, 0.2f) // radius scale
        setInput(4, true) // blur
    }

    override fun executeAction() {

        val ssaoSamples = getIntInput(1)
        val strength = getFloatInput(2)
        val radiusScale = getFloatInput(3)
        val blur = getBoolInput(4)

        val normal = getInput(5) as? Texture ?: return fail()
        val normalZW = normal.isZWMapping
        val normalT = normal.texOrNull ?: normalTexture

        val depthT = (getInput(6) as? Texture) ?: return fail()
        val depthTT = depthT.texOrNull ?: return fail()

        val colorT = (getInput(7) as? Texture) ?: return fail()
        val colorTT = colorT.texOrNull ?: whiteTexture

        val roughT = (getInput(8) as? Texture) ?: return fail()
        val roughTT = roughT.texOrNull ?: whiteTexture
        val roughTM = roughT.mask

        val illumT = (getInput(9) as? Texture) ?: return fail()
        val illumTT = illumT.texOrNull ?: return fail()

        val data = ScreenSpaceAmbientOcclusion.SSGIData(illumTT, colorTT, roughTT, roughTM)

        timeRendering(name, timer) {
            val transform = RenderState.cameraMatrix
            val result = ScreenSpaceAmbientOcclusion.compute(
                data, depthTT, depthT.mapping, normalT, normalZW,
                transform, strength, radiusScale, ssaoSamples, blur, false
            )
            setOutput(1, Texture.texture(result, 0, "rgb", null))
        }
    }
}
