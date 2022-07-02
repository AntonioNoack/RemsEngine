package me.anno.gpu.shader

import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import org.joml.Vector3fc
import org.joml.Vector4fc

open class Renderer(
    val name: String,
    val isFakeColor: Boolean,
    val drawMode: ShaderPlus.DrawMode,
    val deferredSettings: DeferredSettingsV2? = null // null, if not deferred
) {

    open fun getPostProcessing(): ShaderStage? = null

    open fun uploadDefaultUniforms(shader: Shader) {}

    open fun shaderColor(shader: Shader, name: String, r: Float, g: Float, b: Float, a: Float) {
        shader.v4f(name, r, g, b, a)
    }

    fun shaderColor(shader: Shader, name: String, color: Int) {
        shaderColor(shader, name, color.r01(), color.g01(), color.b01(), color.a01())
    }

    fun shaderColor(shader: Shader, name: String, value: Vector4fc?) {
        if (value == null) shaderColor(shader, name, 1f, 1f, 1f, 1f)
        else shaderColor(shader, name, value.x(), value.y(), value.z(), value.w())
    }

    fun shaderColor(shader: Shader, name: String, value: Vector3fc?) {
        if (value == null) shaderColor(shader, name, 1f, 1f, 1f, 1f)
        else shaderColor(shader, name, value.x(), value.y(), value.z(), 1f)
    }

    fun split(index: Int, spliceSize: Int): Renderer {
        if (deferredSettings == null) return this
        return cache!!.getOrPut(index.shl(16) + spliceSize) {
            val settings = deferredSettings.split(index, spliceSize)
            object : Renderer("$name/$index/$spliceSize", isFakeColor, drawMode, settings) {
                override fun getPostProcessing(): ShaderStage? = this@Renderer.getPostProcessing()
                override fun uploadDefaultUniforms(shader: Shader) {
                    this@Renderer.getPostProcessing()
                }
            }
        }
    }

    val cache = if (deferredSettings == null) null else HashMap<Int, Renderer>()

    override fun toString(): String = name

    companion object {

        val colorRenderer = Renderer("color", false, ShaderPlus.DrawMode.COLOR, null)
        val colorSqRenderer = Renderer("colorSq", false, ShaderPlus.DrawMode.COLOR_SQUARED, null)
        val idRenderer = object : SimpleRenderer(
            "id", true, ShaderPlus.DrawMode.COPY, ShaderStage(
                listOf(
                    Variable(GLSLType.V4F, "tint"),
                    Variable(GLSLType.V1F, "finalAlpha", VariableMode.INOUT),
                    Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                ), "if(finalAlpha < 0.01) discard; finalColor = tint.rgb; finalAlpha = tint.a;\n"
            )
        ) {
            override fun shaderColor(shader: Shader, name: String, r: Float, g: Float, b: Float, a: Float) {
                val id = GFX.drawnId
                shader.v4f(name, id.b01(), id.g01(), id.r01(), id.a01())
            }
        }

        val nothingRenderer = SimpleRenderer("depth", true, ShaderPlus.DrawMode.COPY, ShaderStage(emptyList(), ""))
        val depthRenderer = SimpleRenderer(
            "depth", true, ShaderPlus.DrawMode.COPY,
            ShaderStage(
                listOf(
                    Variable(GLSLType.V1F, "zDistance"),
                    Variable(GLSLType.V1F, "finalAlpha", VariableMode.INOUT),
                    Variable(GLSLType.V3F, "finalColor", VariableMode.OUT)
                ), "if(finalAlpha<0.01) discard; finalColor = vec3(zDistance, 0.0, zDistance * zDistance);\n"
            )
        )
        val copyRenderer = Renderer("copy", false, ShaderPlus.DrawMode.COPY, null)
        val triangleVisRenderer = SimpleRenderer(
            "randomId", true, ShaderPlus.DrawMode.COPY, ShaderStage(
                listOf(
                    Variable(GLSLType.V1I, "randomId"),
                    Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                    Variable(GLSLType.V1F, "finalAlpha", VariableMode.INOUT)
                ),
                "" +
                        "if(finalAlpha<0.01) discard;\n" +
                        "float flRandomId = float(randomId);\n" +
                        "vec2 seed = vec2(sin(flRandomId), cos(flRandomId));\n" +
                        ShaderPlus.randomFunc +
                        "finalColor = vec3(GET_RANDOM(seed.xy), GET_RANDOM(seed.yx), GET_RANDOM(100.0 - seed.yx)); finalAlpha = 1.0;\n"
            )
        )
        val motionVectorRenderer = attributeRenderers[DeferredLayerType.MOTION]

    }

}