package me.anno.graph.visual.render.effects

import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.effects.FXAA
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.graph.visual.render.Texture

/**
 * fast approximate edge reconstruction:
 * smooths harsh pixelated lines
 * */
class FXAANode : TimedRenderingNode(
    "FXAA",
    listOf("Float", "Threshold", "Texture", "Illuminated"),
    listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 0.1f) // threshold
    }

    override fun executeAction() {
        val threshold = getFloatInput(1)
        val color = getTextureInput(2)
        if (color != null) {
            timeRendering(name, timer) {
                val framebuffer = FBStack[name, color.width, color.height, 4, false, 1, DepthBufferType.NONE]
                useFrame(color.width, color.height, true, framebuffer, copyRenderer) {
                    FXAA.render(color, threshold)
                }
                setOutput(1, Texture.texture(framebuffer, 0))
            }
        } else setOutput(1, null)
    }
}