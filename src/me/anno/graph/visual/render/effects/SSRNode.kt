package me.anno.graph.visual.render.effects

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.effects.ScreenSpaceReflections
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.isZWMapping
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texMSOrNull
import me.anno.graph.visual.render.Texture.Companion.texOrNull

class SSRNode : TimedRenderingNode(
    "Screen Space Reflections",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Float", "Strength",
        "Float", "Mask Sharpness",
        "Float", "Wall Thickness",
        "Int", "Fine Steps",
        "Texture", "Illuminated",
        "Texture", "Diffuse",
        "Texture", "Normal", // optional in the future
        "Texture", "Metallic",
        "Texture", "Roughness",
        "Texture", "Depth",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1f) // strength
        setInput(4, 1f) // mask sharpness
        setInput(5, 0.2f) // wall thickness
        setInput(6, 10) // fine steps
    }

    override fun executeAction() {

        val width = getIntInput(1)
        val height = getIntInput(2)
        if (width < 1 || height < 1) return

        val strength = getFloatInput(3)
        val maskSharpness = getFloatInput(4)
        val wallThickness = getFloatInput(5)
        val fineSteps = getIntInput(6) // 10

        val illumT = getInput(7) as? Texture ?: return
        val illumMT = illumT.texMSOrNull
        val illumTT = illumT.texOrNull ?: return

        val colorTT = (getInput(8) as? Texture).texOrNull ?: whiteTexture

        val normal = getInput(9) as? Texture
        val normalZW = normal.isZWMapping
        val normalT = normal.texOrNull ?: whiteTexture

        val metallic = getInput(10) as? Texture
        val roughness = getInput(11) as? Texture
        val depthT = getInput(12) as? Texture ?: return
        val depthTT = depthT.texOrNull ?: return

        timeRendering(name, timer) {
            val transform = RenderState.cameraMatrix
            val result0 = FBStack["ssr", width, height, 4, true, 1, DepthBufferType.NONE]

            val metallicT = metallic?.texOrNull ?: whiteTexture
            val metallicM = metallic.mask1Index

            val roughnessT = roughness?.texOrNull ?: blackTexture
            val roughnessM = roughness.mask1Index

            val originalSamples = (illumMT ?: illumTT).samples
            val inPlace = illumMT == null || originalSamples <= 1
            ScreenSpaceReflections.compute(
                depthTT, depthT.mask1Index, normalT, normalZW, colorTT,
                metallicT, metallicM, roughnessT, roughnessM, illumTT,
                transform, strength, maskSharpness, wallThickness, fineSteps,
                inPlace, result0
            )
            val result = if (inPlace) result0 else mixResult(width, height, illumMT!!, result0)
            setOutput(1, Texture.texture(result, 0))
        }
    }

    private fun mixResult(width: Int, height: Int, illumMT: ITexture2D, result0: IFramebuffer): IFramebuffer {
        val result1 = FBStack["ssr-result", width, height, 3, true, illumMT.samples, DepthBufferType.NONE]
        useFrame(result1) {
            GFX.copy(illumMT)
            GFXState.blendMode.use(BlendMode.DEFAULT) {
                GFX.copy(result0)
            }
        }
        return result1
    }
}