package me.anno.graph.render.effects

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.shader.effects.ScreenSpaceAmbientOcclusion
import me.anno.gpu.texture.TextureLib.normalTexture
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.FlowGraphNodeUtils.getBoolInput
import me.anno.graph.types.flow.FlowGraphNodeUtils.getFloatInput
import me.anno.graph.types.flow.FlowGraphNodeUtils.getIntInput
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.utils.Color.black4

class SSAONode : ActionNode(
    "Screen Space Ambient Occlusion",
    listOf(
        "Int", "SSAO Samples",
        "Float", "Strength",
        "Float", "Radius Scale",
        "Bool", "Blur",
        "Texture", "Normal", // optional
        "Texture", "Depth",
    ), listOf("Texture", "Ambient Occlusion")
) {

    init {
        setInput(1, 64) // samples
        setInput(2, 1f) // strength
        setInput(3, 0.2f) // radius scale
        setInput(4, true) // blur
    }

    override fun executeAction() {

        val ssaoSamples = getIntInput(1)
        val strength = getFloatInput(2)
        val radiusScale = getFloatInput(3)
        val blur = getBoolInput(4)

        val normal = getInput(5) as? Texture ?: return fail()
        val normalZW = normal.mapping == "zw"
        val normalT = normal.texOrNull ?: normalTexture
        val depthT = (getInput(6) as? Texture) ?: return fail()
        val depthTT = depthT.texOrNull ?: return fail()

        val transform = RenderState.cameraMatrix
        val result = ScreenSpaceAmbientOcclusion
            .compute(depthTT, depthT.mapping, normalT, normalZW, transform, strength, radiusScale, ssaoSamples, blur)

        setOutput(1, Texture.texture(result, 0, "r", null))
    }

    private fun fail() {
        setOutput(1, Texture(black4))
    }
}