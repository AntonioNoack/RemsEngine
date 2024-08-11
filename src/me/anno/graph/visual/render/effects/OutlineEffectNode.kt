package me.anno.graph.visual.render.effects

import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.utils.structures.lists.Lists.createList
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector4f
import kotlin.math.min

class OutlineEffectNode : RenderViewNode(
    "OutlineEffect",
    listOf(
        "Int", "Width",
        "Int", "Height",
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
        // todo fractional thickness might be cool
        setInput(1, 256)
        setInput(2, 256)
        setInput(3, 2) // radius
        setInput(4, intArrayOf(1))
        setInput(5, listOf(Vector4f(1f, 1f, 1f, 0.5f))) // fill colors
        setInput(6, listOf(Vector4f(1f, 1f, 1f, 1f))) // line colors
    }

    override fun executeAction() {
        val w = getIntInput(1)
        val h = getIntInput(2)
        // not really supported...
        val radius = getIntInput(3)
        val idsTex = getInput(8) as? Texture ?: return
        val colorTex = getInput(7) as? Texture ?: return
        val groupIds = getInput(4) as? IntArray ?: return
        val fillColors = getInput(5) as? List<*> ?: return
        val lineColors = getInput(6) as? List<*> ?: return
        val fillColors1 = fillColors.filterIsInstance<Vector4f>()
        val lineColors1 = lineColors.filterIsInstance<Vector4f>()
        val numGroupsI = min(groupIds.size, min(fillColors1.size, lineColors1.size))
        if (radius >= 0 && numGroupsI > 0) {
            timeRendering(name, timer) {
                val dst = FBStack[name, w, h, 4, true, 1, DepthBufferType.NONE]
                GFXState.useFrame(dst) {
                    render(
                        colorTex, idsTex,
                        min(groupIds.size, min(fillColors1.size, lineColors1.size)),
                        radius, groupIds, fillColors1, lineColors1
                    )
                }
                setOutput(1, Texture.texture(dst, 0))
            }
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
            color: Texture, ids: Texture, numGroups: Int, radius: Int,
            groupIds: IntArray, fillColors: List<Vector4f>, lineColors: List<Vector4f>
        ) {
            val samples = min(color.texMS.samples, ids.texMS.samples)
            val useMS = samples > 1
            val numGroupsI = min(numGroups, MAX_NUM_GROUPS)
            GFXState.renderPurely {
                val shader = shader[useMS.toInt()]
                shader.use()
                shader.v1i("radius", radius)
                val th = radius * 2 + 1
                val radSq = (th * th * samples).toFloat()
                shader.v1f("radSq", radSq)
                shader.v1f("invRadSq", 1f / radSq)
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
                shader.v1i("samples", samples)
                shader.v4f("groupTexMask", ids.mask ?: Vector4f(1f))
                getTex(color, useMS).bindTrulyNearest(shader, "colorTex")
                getTex(ids, useMS).bindTrulyNearest(shader, "idTex")
                SimpleBuffer.flat01.draw(shader)
            }
        }

        private fun getTex(texture: Texture, useMS: Boolean): ITexture2D {
            return (if (useMS) texture.texMSOrNull else texture.texOrNull) ?: blackTexture
        }

        val shader = createList(2) {
            val useMS = it != 0
            val shader = Shader(
                "outlines", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList, listOf(
                    Variable(GLSLType.V1I, "radius"),
                    Variable(GLSLType.V1F, "radSq"),
                    Variable(GLSLType.V1F, "invRadSq"),
                    Variable(GLSLType.V1I, "numGroups"),
                    Variable(GLSLType.V1I, "samples"),
                    Variable(GLSLType.V1F, "groupIds", MAX_NUM_GROUPS),
                    Variable(GLSLType.V4F, "lineColors", MAX_NUM_GROUPS),
                    Variable(GLSLType.V4F, "fillColors", MAX_NUM_GROUPS),
                    Variable(if (useMS) GLSLType.S2DMS else GLSLType.S2D, "idTex"),
                    Variable(if (useMS) GLSLType.S2DMS else GLSLType.S2D, "colorTex"),
                    Variable(GLSLType.V4F, "groupTexMask"),
                    Variable(GLSLType.V4F, "result", VariableMode.OUT)
                ), "" +
                        "void main(){\n" +
                        "   vec2 dx = dFdx(uv);\n" +
                        "   vec2 dy = dFdy(uv);\n" +
                        "   float sum = 0.0;\n" +
                        "   vec3 color = texture(colorTex,uv).rgb;\n" +
                        (if (useMS) {
                            "" +
                                    "ivec2 size = textureSize(idTex);\n" +
                                    "ivec2 uvi = ivec2(uv*vec2(size));\n"
                        } else "") +
                        "   for(int i=0;i<numGroups;i++){\n" +
                        "       float groupId = groupIds[i];\n" +
                        "       for(int y=-radius;y<=radius;y++){\n" +
                        "           for(int x=-radius;x<=radius;x++){\n" +
                        (if (useMS) {
                            "" +
                                    "for(int j=0;j<samples;j++){\n" +
                                    "   ivec2 uv2 = clamp(uvi + ivec2(x,y),ivec2(0),size-1);\n" +
                                    "   float groupId1 = dot(groupTexMask,texelFetch(idTex,uv2,j));\n" +
                                    "   sum += step(0.5/255.0, abs(groupId - groupId1));\n" +
                                    "}\n"
                        } else {
                            "" +
                                    "vec2 uv2 = uv + dx * float(x) + dy * float(y);\n" +
                                    "float groupId1 = dot(groupTexMask,texture(idTex,uv2));\n" +
                                    "sum += step(0.5/255.0, abs(groupId - groupId1));\n"
                        }) +
                        "           }\n" +
                        "       }\n" +
                        "       if(sum < radSq){\n" +
                        "           float percentage = sum * invRadSq;\n" +
                        "           float percentage2 = 1.0 - 2.0 * abs(percentage - 0.5);\n" +
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
            )
            shader.ignoreNameWarnings("samples")
            shader
        }
    }
}