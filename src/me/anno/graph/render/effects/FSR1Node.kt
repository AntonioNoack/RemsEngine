package me.anno.graph.render.effects

import me.anno.ecs.components.shaders.effects.FSR
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode

class FSR1Node : ActionNode(
    "FSR1",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Float", "Sharpness",
        "Texture", "Illuminated",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1f) // sharpness
    }

    // hdr? upscaling, so not really necessary
    private val f0 = Framebuffer("fsr1-0", 1, 1, arrayOf(TargetType.UByteTarget4))
    private val f1 = Framebuffer("fsr1-1", 1, 1, arrayOf(TargetType.UByteTarget4))

    override fun onDestroy() {
        super.onDestroy()
        f0.destroy()
        f1.destroy()
    }

    override fun executeAction() {

        val width = getInput(1) as Int
        val height = getInput(2) as Int
        if (width < 1 || height < 1) return

        val sharpness = getInput(3) as Float
        val color = (getInput(4) as? Texture)?.tex ?: whiteTexture

        useFrame(width, height, true, f0, copyRenderer) {
            FSR.upscale(color, 0, 0, width, height, flipY = true, applyToneMapping = false)
        }

        if (sharpness > 0f) {
            // todo sharpening doesn't work yet
            useFrame(width, height, true, f1, copyRenderer) {
                FSR.sharpen(f0.getTexture0(), sharpness, flipY = true)
            }
            setOutput(Texture(f1.getTexture0()), 1)
        } else {
            setOutput(Texture(f0.getTexture0()), 1)
        }

    }
}