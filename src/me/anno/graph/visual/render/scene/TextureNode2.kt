package me.anno.graph.visual.render.scene

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeInput
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.TextureNode.Companion.violet
import me.anno.graph.visual.render.compiler.GLSLExprNode
import me.anno.graph.visual.render.compiler.GraphCompiler
import org.joml.Vector2f

// todo texture size node
@Suppress("unused")
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
), GLSLExprNode {

    init {
        setInput(0, whiteTexture)
        setInput(1, Vector2f())
        setInput(2, true)
        setInput(3, 0)
    }

    override fun calculate() = violet

    override fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node) {
        val input = out.others.firstOrNull() as? NodeInput
        if (input != null) {
            val texName = g.textures2.getOrPut(input) {
                val linear = g.constEval(inputs[2]) == true
                val currValue = input.currValue
                val useMS = currValue is Texture2D && currValue.samples > 1
                // todo different color repeat modes in GLSL
                Triple(
                    "tex2I${out.name.filter { it in 'A'..'Z' || it in 'a'..'z' }}${g.textures2.size}",
                    if (useMS) GLSLType.S2DMS else GLSLType.S2D,
                    linear
                )
            }
            if (texName.second == GLSLType.S2DMS) {
                g.builder.append("texelFetch(").append(texName.first).append(",ivec2(")
                g.expr(inputs[1]) // uv
                g.builder.append("*textureSize(").append(texName.first).append(")),gl_SampleID)")
            } else {
                g.builder.append("texture(").append(texName.first).append(',')
                g.expr(inputs[1]) // uv
                g.builder.append(')')
            }
        } else {
            g.builder.append("vec4(1.0,0.0,1.0,1.0)")
        }
    }
}