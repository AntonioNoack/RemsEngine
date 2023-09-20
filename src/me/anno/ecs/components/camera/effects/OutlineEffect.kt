package me.anno.ecs.components.camera.effects

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import org.joml.Vector4f
import kotlin.math.min

class OutlineEffect : CameraEffect() {

    @Docs("How thick the line shall be; 0 disables the line, but still fills the color; -1 disabled the effect completely; O(n²) complexity, so don't make it too thick")
    @Range(-1.0, 20.0)
    var radius = 3

    @Docs("How many groups are to be checked")
    @Range(0.0, 4.0)
    var numGroups = 1

    @Type("IntArray")
    var groupIds = IntArray(MAX_NUM_GROUPS)

    @Type("Array<Color4>")
    var fillColors = Array(MAX_NUM_GROUPS) { Vector4f() }

    @Type("Array<Color4>")
    var lineColors = Array(MAX_NUM_GROUPS) { Vector4f() }

    init {
        // default settings: white outline and fill on groupId 1
        lineColors[0].set(1f)
        fillColors[0].set(1f, 1f, 1f, 0.5f)
        groupIds[0] = 1
    }

    override fun listInputs() = inputs
    override fun listOutputs() = outputs

    override fun render(
        buffer: IFramebuffer,
        format: DeferredSettingsV2,
        layers: MutableMap<DeferredLayerType, IFramebuffer>
    ) {
        val numGroups = min(numGroups, MAX_NUM_GROUPS)
        val radius = radius
        if (radius >= 0 && numGroups > 0) {
            val color = layers[DeferredLayerType.SDR_RESULT]!!.getTexture0()
            val ids = layers[DeferredLayerType.ID]!!.getTexture0()
            val result = FBStack["outline", color.width, color.height, 4, BufferQuality.LOW_8, 1, false]
            renderPurely {
                val shader = shader
                shader.use()
                shader.v1i("radius", radius)
                val th = radius * 2 + 1
                shader.v1f("radSq", (th * th).toFloat())
                shader.v1f("invRadSq", 1f / (th * th))
                for (i in 0 until numGroups) {
                    groupIds2[i] = groupIds[i] / 255f
                    val i4 = i shl 2
                    fill(fillColors2, i4, fillColors[i])
                    if (radius > 0) fill(lineColors2, i4, lineColors[i])
                }
                if (radius > 0) shader.v4fs("lineColors", lineColors2)
                shader.v4fs("fillColors", fillColors2)
                shader.v1fs("groupIds", groupIds2)
                shader.v1i("numGroups", numGroups)
                color.bindTrulyNearest(0)
                ids.bindTrulyNearest(1)
                useFrame(result) {
                    GFX.flat01.draw(shader)
                }
            }
            write(layers, DeferredLayerType.SDR_RESULT, result)
        }
    }

    companion object {

        private val inputs = listOf(DeferredLayerType.SDR_RESULT, DeferredLayerType.ID)
        private val outputs = listOf(DeferredLayerType.SDR_RESULT)
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

        val shader = Shader(
            "outlines", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList, listOf(
                Variable(GLSLType.V1I, "radius"),
                Variable(GLSLType.V1F, "radSq"),
                Variable(GLSLType.V1F, "invRadSq"),
                Variable(GLSLType.V1I, "numGroups"),
                Variable(GLSLType.V1F, "groupIds", MAX_NUM_GROUPS),
                Variable(GLSLType.V4F, "lineColors", MAX_NUM_GROUPS),
                Variable(GLSLType.V4F, "fillColors", MAX_NUM_GROUPS),
                Variable(GLSLType.S2D, "idTex"),
                Variable(GLSLType.S2D, "colorTex"),
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
                    "               sum += step(0.5/255.0, abs(groupId - texture(idTex,uv2).a));\n" +
                    "           }\n" +
                    "       }\n" +
                    "       if(sum < radSq){\n" + // "if(...)" could be skipped, may provide a marginal performance boost
                    "           float percentage = sum * invRadSq;\n" +
                    "           float percentage2 = 4.0 * percentage * (1.0 - percentage);\n" +
                    "           vec4 fillColor = fillColors[i];\n" +
                    "           vec4 lineColor = lineColors[i];\n" +
                    "           color = mix(\n" +
                    "               mix(color, fillColor.rgb, fillColor.a * (1.0 - percentage)),\n" +
                    "               lineColor.rgb, percentage2 * lineColor.a\n" +
                    "           );\n" +
                    "       }\n" +
                    "   }\n" +
                    "   result = vec4(color, 1.0);\n" +
                    "}\n"
        ).setTextureIndices("colorTex", "idTex") as Shader

    }

}