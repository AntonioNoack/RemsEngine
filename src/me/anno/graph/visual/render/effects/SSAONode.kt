package me.anno.graph.visual.render.effects

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.shader.effects.ScreenSpaceAmbientOcclusion
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.normalTexture
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.isZWMapping
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texMSOrNull
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

class SSAONode : TimedRenderingNode(
    "SSAO",
    listOf(
        "Int", "SSAO Samples",
        "Float", "Strength",
        "Float", "Radius Scale",
        "Bool", "Blur",
        "Bool", "Inverse",
        "Texture", "Normal", // optional
        "Texture", "Depth",
    ), listOf("Texture", "Ambient Occlusion")
) {

    init {
        description = "Screen Space Ambient Occlusion"
        setInput(1, if (OS.isAndroid || OS.isWeb) 8 else 64) // samples
        setInput(2, 1f) // strength
        setInput(3, 0.2f) // radius scale
        setInput(4, true) // blur
        setInput(5, false) // inverse
    }

    override fun executeAction() {

        val ssaoSamples = getIntInput(1)
        val strength = getFloatInput(2)
        val radiusScale = getFloatInput(3)
        val blur = getBoolInput(4)
        val inverse = getBoolInput(5)

        val normal = getInput(6) as? Texture ?: return fail()
        val normalZW = normal.isZWMapping
        val normalT = normal.texMSOrNull ?: normalTexture
        val depthT = (getInput(7) as? Texture) ?: return fail()
        val depthTT = depthT.texMSOrNull ?: return fail()

        timeRendering(name, timer) {
            val transform = RenderState.cameraMatrix
            val result = ScreenSpaceAmbientOcclusion.compute(
                null, depthTT, depthT.mask1Index, normalT, normalZW,
                transform, strength, radiusScale, ssaoSamples, blur, inverse
            )
            setOutput(1, Texture.texture(result, 0, "r", null))
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(SSAONode::class)
        fun Node.fail() {
            LOGGER.warn("Failed $className, '$name'!")
            setOutput(1, Texture(blackTexture))
        }
    }
}