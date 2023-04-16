package me.anno.graph.render.effects

import me.anno.ecs.components.camera.effects.DepthOfFieldEffect
import me.anno.gpu.texture.Texture2D
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode

class DepthOfFieldNode : ActionNode(
    "Depth of Field",
    listOf(
        "Float", "Focus Point",
        "Float", "Focus Scale",
        "Float", "Rad Scale", // Smaller = nicer blur, larger = faster
        "Float", "Max Blur Size", // in pixels
        "Float", "Spherical",
        "Bool", "Apply Tone Mapping",
        "Texture", "Illuminated",
        "Texture", "Depth"
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 1f)
        setInput(2, 0.25f)
        setInput(3, 0.5f)
        setInput(4, 20f)
        setInput(5, 0f)
        setInput(6, false)
    }

    override fun executeAction() {

        val focusPoint = getInput(1) as Float
        val focusScale = getInput(2) as Float
        val radScale = getInput(3) as Float
        val maxBlurSize = getInput(4) as Float
        val spherical = getInput(5) as Float

        val applyToneMapping = getInput(6) == true
        val color = ((getInput(7) as? Texture)?.tex as? Texture2D) ?: return
        val depth = ((getInput(8) as? Texture)?.tex as? Texture2D) ?: return

        val result = DepthOfFieldEffect.render(
            color, depth, spherical, focusPoint, focusScale,
            maxBlurSize, radScale, applyToneMapping
        ).getTexture0()
        setOutput(Texture(result), 1)

    }
}