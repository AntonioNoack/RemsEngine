package me.anno.graph.render.effects

import me.anno.gpu.shader.effects.ScreenSpaceAmbientOcclusion
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.utils.Color.black4

class SSAONode : ActionNode(
    "Screen Space Ambient Occlusion",
    listOf(
        "Int", "Samples",
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

        val samples = getInput(1) as Int
        if (samples < 1) return fail()

        val strength = getInput(2) as Float
        val radiusScale = getInput(3) as Float
        val blur = getInput(4) == true

        val normal = getInput(5) as? Texture ?: return fail()
        val normalZW = normal.mapping == "zw"
        val normalT = ((normal).tex as? Texture2D) ?: whiteTexture
        val depthT = (getInput(6) as? Texture) ?: return fail()

        val transform = RenderState.cameraMatrix
        val result = ScreenSpaceAmbientOcclusion
            .compute(depthT.tex, depthT.mapping, normalT, normalZW, transform, strength, radiusScale, samples, blur)

        setOutput(1, Texture.texture(result, 0, "r", null))
    }

    private fun fail() {
        setOutput(1, Texture(black4))
    }
}