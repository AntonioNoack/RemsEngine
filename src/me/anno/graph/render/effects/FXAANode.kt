package me.anno.graph.render.effects

import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.effects.FXAA
import me.anno.gpu.texture.Texture2D
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode

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
        val threshold = getInput(1) as Float
        val color = ((getInput(2) as? Texture)?.tex as? Texture2D) ?: return
        val framebuffer = FBStack[name, color.width, color.height, 4, false, 1, DepthBufferType.NONE]
        useFrame(color.width, color.height, true, framebuffer, copyRenderer) {
            FXAA.render(color, threshold)
        }
        setOutput(1, Texture.texture(framebuffer, 0))
    }
}