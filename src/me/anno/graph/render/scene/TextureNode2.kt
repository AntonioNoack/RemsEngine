package me.anno.graph.render.scene

import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.render.TextureNode.Companion.violet
import me.anno.graph.types.flow.CalculationNode
import org.joml.Vector2f

// todo texture size node
class TextureNode2 : CalculationNode(
    "Texture",
    // todo different color repeat modes in GLSL
    listOf(
        "Texture", "Texture",
        "Vector2f", "UV",
        "Boolean", "Linear",
        "Int", "Clamp/Repeat/MRepeat"
    ),
    listOf("Vector4f", "Color")
) {

    init {
        setInput(0, whiteTexture)
        setInput(1, Vector2f())
        setInput(2, true)
        setInput(3, 0)
    }

    override fun calculate() = violet

}