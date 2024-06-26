package me.anno.graph.visual.render.effects

import me.anno.gpu.GFXState.popDrawCallName
import me.anno.gpu.GFXState.pushDrawCallName
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.effects.FXAA
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.actions.ActionNode

/**
 * fast approximate edge reconstruction:
 * smooths harsh pixelated lines
 * */
class FXAANode : ActionNode(
    "FXAA",
    listOf(
        "Float", "Threshold",
        "Texture", "Illuminated",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 1e-5f) // threshold
    }

    override fun executeAction() {
        pushDrawCallName(name)
        val threshold = getFloatInput(1)
        val color = (getInput(2) as? Texture)?.texOrNull ?: missingTexture
        val framebuffer = FBStack[name, color.width, color.height, 4, false, 1, DepthBufferType.NONE]
        useFrame(color.width, color.height, true, framebuffer, copyRenderer) {
            FXAA.render(color, threshold)
        }
        setOutput(1, Texture.texture(framebuffer, 0))
        popDrawCallName()
    }
}