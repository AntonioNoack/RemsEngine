package me.anno.graph.render.effects

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.graph.render.Texture
import me.anno.graph.render.scene.RenderViewNode
import org.joml.Vector4f
import kotlin.math.min

// todo integrate all useful effects from Three.js
//  - https://threejs.org/examples/?q=post#webgl_postprocessing_pixel, specifically their edge and depth detection

class OutlineEffectNode : RenderViewNode(
    "OutlineEffect",
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Int", "Radius",
        "IntArray", "Group IDs",
        "List<Color4>", "Fill Colors",
        "List<Color4>", "Line Colors",
        "Texture", "Illuminated",
        "Texture", "GroupID",
    ),
    listOf("Texture", "Illuminated")
) {

    init {
        // todo fractional thickness would be cool
        setInput(1, 256)
        setInput(2, 256)
        setInput(3, 1)
        setInput(4, 2) // radius
        setInput(5, intArrayOf(1))
        setInput(6, listOf(Vector4f(1f, 1f, 1f, 0.5f))) // fill colors
        setInput(7, listOf(Vector4f(1f, 1f, 1f, 1f))) // line colors
    }

    override fun executeAction() {
        val w = getInput(1) as Int
        val h = getInput(2) as Int
        // not really supported...
        val samples = getInput(3) as Int
        val radius = getInput(4) as Int
        val idsTex = getInput(9) as? Texture ?: return
        val colorTex = getInput(8) as? Texture ?: return
        val groupIds = getInput(5) as? IntArray ?: return
        val fillColors = getInput(6) as? List<*> ?: return
        val lineColors = getInput(7) as? List<*> ?: return
        val fillColors1 = fillColors.filterIsInstance<Vector4f>().toTypedArray()
        val lineColors1 = lineColors.filterIsInstance<Vector4f>().toTypedArray()
        val numGroupsI = min(groupIds.size, min(fillColors1.size, lineColors1.size))
        if (radius >= 0 && numGroupsI > 0) {
            val dst = FBStack["outline", w, h, 4, true, samples, DepthBufferType.NONE]
            GFXState.useFrame(dst) {
                render(
                    colorTex.tex, idsTex,
                    min(groupIds.size, min(fillColors1.size, lineColors1.size)),
                    radius, groupIds, fillColors1, lineColors1
                )
            }
            setOutput(1, Texture.texture(dst, 0))
        } else setOutput(1, colorTex)
    }

    companion object {

        const val MAX_NUM_GROUPS = 32

        private val groupIds2 = FloatArray(MAX_NUM_GROUPS)
        private val fillColors2 = FloatArray(MAX_NUM_GROUPS * 4)
        private val lineColors2 = FloatArray(MAX_NUM_GROUPS * 4)

        private fun fill(v: FloatArray, i: Int, color: Vector4f) {
            v[i] = color.x
            v[i + 1] = color.y
            v[i + 2] = color.z
            v[i + 3] = color.w
        }

        fun render(
            color: ITexture2D, ids: Texture, numGroups: Int, radius: Int,
            groupIds: IntArray, fillColors: Array<Vector4f>, lineColors: Array<Vector4f>
        ) {
            val numGroupsI = min(numGroups, MAX_NUM_GROUPS)
            GFXState.renderPurely {
                val shader = shader
                shader.use()
                shader.v1i("radius", radius)
                val th = radius * 2 + 1
                shader.v1f("radSq", (th * th).toFloat())
                shader.v1f("invRadSq", 1f / (th * th))
                for (i in 0 until numGroupsI) {
                    groupIds2[i] = groupIds[i] / 255f
                    val i4 = i shl 2
                    fill(fillColors2, i4, fillColors[i])
                    if (radius > 0) fill(lineColors2, i4, lineColors[i])
                }
                if (radius > 0) shader.v4fs("lineColors", lineColors2)
                shader.v4fs("fillColors", fillColors2)
                shader.v1fs("groupIds", groupIds2)
                shader.v1i("numGroups", numGroupsI)
                shader.v4f("groupTexMask", ids.mask!!)
                color.bindTrulyNearest(0)
                ids.tex.bindTrulyNearest(1)
                GFX.flat01.draw(shader)
            }
        }


        val shader = Shader(
            "outlines", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList, listOf(
                Variable(GLSLType.V1I, "radius"),
                Variable(GLSLType.V1F, "radSq"),
                Variable(GLSLType.V1F, "invRadSq"),
                Variable(GLSLType.V1I, "numGroups"),
                Variable(GLSLType.V1F, "groupIds", MAX_NUM_GROUPS),
                Variable(GLSLType.V4F, "lineColors", MAX_NUM_GROUPS),
                Variable(GLSLType.V4F, "fillColors", MAX_NUM_GROUPS),
                Variable(GLSLType.S2D, "idTex"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.V4F, "groupTexMask"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "void main(){\n" +
                    "   vec2 dx = dFdx(uv);\n" +
                    "   vec2 dy = dFdy(uv);\n" +
                    "   float sum = 0.0;\n" +
                    "   vec3 color = texture(colorTex,uv).rgb;\n" +
                    "   for(int i=0;i<numGroups;i++){\n" +
                    "       float groupId = groupIds[i];\n" +
                    "       for(int y=-radius;y<=radius;y++){\n" +
                    "           for(int x=-radius;x<=radius;x++){\n" +
                    "               vec2 uv2 = uv + dx * float(x) + dy * float(y);\n" +
                    "               float groupId1 = dot(groupTexMask,texture(idTex,uv2));\n" +
                    "               sum += step(0.5/255.0, abs(groupId - groupId1));\n" +
                    "           }\n" +
                    "       }\n" +
                    "       if(sum < radSq){\n" + // "if(...)" could be skipped, may provide a marginal performance boost
                    "           float percentage = sum * invRadSq;\n" +
                    "           float percentage2 = 4.0 * percentage * (1.0 - percentage);\n" +
                    "           percentage2 = clamp(percentage2 * float(radius), 0.0, 1.0);\n" +
                    "           vec4 fillColor = fillColors[i];\n" +
                    "           vec4 lineColor = lineColors[i];\n" +
                    "           color = mix(\n" +
                    "               mix(color, fillColor.rgb, (1.0 - fillColor.a) * (1.0 - percentage)),\n" +
                    "               lineColor.rgb, percentage2 * lineColor.a\n" +
                    "           );\n" +
                    "       }\n" +
                    "   }\n" +
                    "   result = vec4(color, 1.0);\n" +
                    "}\n"
        ).setTextureIndices("colorTex", "idTex") as Shader
    }
}