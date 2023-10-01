package me.anno.graph.render.effects

import me.anno.ecs.components.shaders.effects.ScreenSpaceAmbientOcclusion
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.utils.Color.black4

class SSAONode : ActionNode(
    "Screen Space Ambient Occlusion",
    listOf(
        // todo add width and height?
        "Int", "Samples",
        "Float", "Strength",
        "Bool", "Blur",
        "Texture", "Normal", // optional
        "Texture", "Depth",
    ), listOf("Texture", "Ambient Occlusion")
) {

    // todo "depth texture to normal"-node

    init {
        setInput(1, 64) // samples
        setInput(2, 1f) // strength
        setInput(3, true) // blur
        setInput(4, null) // normals
        setInput(5, null) // depth
    }

    override fun executeAction() {

        val samples = getInput(1) as Int
        if (samples < 1) return fail()

        val strength = getInput(2) as Float
        val blur = getInput(3) == true

        val normal = getInput(4) as? Texture ?: return fail()
        val normalZW = normal.mapping == "zw"
        val normalT = ((normal).tex as? Texture2D) ?: whiteTexture
        val depthT = (getInput(5) as? Texture) ?: return fail()
        if (depthT.tex !is Texture2D) return fail()

        val transform = RenderState.cameraMatrix
        val result = ScreenSpaceAmbientOcclusion
            .compute(depthT.tex, depthT.mapping, normalT, normalZW, transform, strength, samples, blur)

        setOutput(1, Texture.texture(result, 0, "r", null))
    }

    private fun fail() {
        setOutput(1, Texture(black4))
    }
}