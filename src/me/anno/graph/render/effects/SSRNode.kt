package me.anno.graph.render.effects

import me.anno.gpu.shader.effects.ScreenSpaceReflections
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.deferred.DeferredSettings.Companion.singleToVector
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.utils.Color.black4
import org.joml.Vector4f

class SSRNode : ActionNode(
    "Screen Space Reflections",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Float", "Strength",
        "Float", "Mask Sharpness",
        "Float", "Wall Thickness",
        "Int", "Fine Steps",
        "Bool", "Apply Tone Mapping",
        "Texture", "Illuminated",
        "Texture", "Diffuse",
        "Texture", "Emissive",
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
        setInput(7, false) // apply tone mapping
    }

    override fun executeAction() {

        val width = getInput(1) as Int
        val height = getInput(2) as Int
        if (width < 1 || height < 1) return

        val strength = getInput(3) as Float
        val maskSharpness = getInput(4) as Float
        val wallThickness = getInput(5) as Float
        val fineSteps = getInput(6) as Int // 10
        val applyToneMapping = getInput(7) == true

        val illuminated = (getInput(8) as? Texture)?.tex ?: return

        val color = (getInput(9) as? Texture)?.tex ?: whiteTexture
        // we might use this again later...
        // val emissive = (getInput(10) as? Texture)?.tex ?: blackTexture

        val normal = getInput(11) as? Texture
        val normalZW = normal?.mapping == "zw"
        val normalT = ((normal)?.tex as? Texture2D) ?: whiteTexture

        val metallic = getInput(12) as? Texture
        val roughness = getInput(13) as? Texture
        val depthT = getInput(14) as? Texture ?: return

        val transform = RenderState.cameraMatrix

        val samples = 1
        val framebuffer = FBStack["ssr", width, height, 4, true, samples, DepthBufferType.NONE]

        val metallicT = metallic?.tex ?: whiteTexture
        val metallicM = if (metallicT != whiteTexture) metallic!!.mask!!
        else metallic?.color?.run { Vector4f(x, 0f, 0f, 0f) } ?: singleToVector["r"]!!

        val roughnessT = roughness?.tex ?: whiteTexture
        val roughnessM = if (roughnessT != whiteTexture) roughness!!.mask!!
        else roughness?.color?.run { Vector4f(x, 0f, 0f, 0f) } ?: black4

        val result = ScreenSpaceReflections.compute(
            depthT.tex, depthT.mask!!,
            normalT, normalZW, color, metallicT, metallicM, roughnessT, roughnessM, illuminated,
            transform, strength, maskSharpness, wallThickness, fineSteps, applyToneMapping, framebuffer
        )
        setOutput(1, Texture.texture(result, 0))
    }
}