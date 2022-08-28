package me.anno.gpu.shader

import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.ImageGPUCache
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.files.UVChecker
import org.joml.Vector3f
import org.joml.Vector4f

/**
 * defines render targets combined with post-processing
 * @param deferredSettings null if not rendering multiple targets
 * */
open class Renderer(val name: String, val deferredSettings: DeferredSettingsV2? = null) {

    open fun getPostProcessing(): ShaderStage? = null

    open fun uploadDefaultUniforms(shader: Shader) {}

    open fun shaderColor(shader: Shader, name: String, r: Float, g: Float, b: Float, a: Float) {
        shader.v4f(name, r, g, b, a)
    }

    fun shaderColor(shader: Shader, name: String, color: Int) {
        shaderColor(shader, name, color.r01(), color.g01(), color.b01(), color.a01())
    }

    fun shaderColor(shader: Shader, name: String, value: Vector4f?) {
        if (value == null) shaderColor(shader, name, 1f, 1f, 1f, 1f)
        else shaderColor(shader, name, value.x, value.y, value.z, value.w)
    }

    fun shaderColor(shader: Shader, name: String, value: Vector3f?) {
        if (value == null) shaderColor(shader, name, 1f, 1f, 1f, 1f)
        else shaderColor(shader, name, value.x, value.y, value.z, 1f)
    }

    fun split(index: Int, spliceSize: Int): Renderer {
        if (deferredSettings == null) return this
        return cache!!.getOrPut(index.shl(16) + spliceSize) {
            val settings = deferredSettings.split(index, spliceSize)
            object : Renderer("$name/$index/$spliceSize", settings) {
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

        val colorRenderer = SimpleRenderer(
            "color", null, ShaderStage(
                listOf(
                    Variable(GLSLType.V4F, "tint"),
                    Variable(GLSLType.V3F, "finalColor"),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "SPResult", VariableMode.OUT),
                ),
                "" +
                        RandomEffect.randomFunc +
                        "SPResult = vec4(finalColor\n" +
                        "   #ifndef IS_TINTED\n * tint.rgb\n #endif\n," +
                        "clamp(finalAlpha\n #ifndef IS_TINTED\n * tint.a\n #endif\n, 0.0, 1.0));\n"
            )
        )

        val colorSqRenderer = SimpleRenderer(
            "colorSq", null, ShaderStage(
                listOf(
                    Variable(GLSLType.V4F, "tint"),
                    Variable(GLSLType.V3F, "finalColor"),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "SPResult", VariableMode.OUT),
                ),
                "" +
                        RandomEffect.randomFunc +
                        "vec3 tmpCol = finalColor\n" +
                        "#ifndef IS_TINTED\n" +
                        " * tint.rgb\n" +
                        "#endif\n" +
                        "SPResult = vec4(tmpCol * tmpCol, clamp(finalAlpha, 0.0, 1.0) * tint.a);\n"
            )
        )

        val idRenderer = object : SimpleRenderer(
            "id", ShaderStage(
                listOf(
                    Variable(GLSLType.V4F, "tint"),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "finalResult", VariableMode.OUT),
                ), "if(finalAlpha < 0.01) discard; finalResult = tint;\n"
            )
        ) {
            override fun shaderColor(shader: Shader, name: String, r: Float, g: Float, b: Float, a: Float) {
                val id = GFX.drawnId
                shader.v4f(name, id.b01(), id.g01(), id.r01(), id.a01())
            }
        }

        val nothingRenderer = SimpleRenderer("depth", ShaderStage(emptyList(), ""))
        val depthRenderer = SimpleRenderer(
            "depth", ShaderStage(
                listOf(
                    Variable(GLSLType.V1F, "zDistance"),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                ), "if(finalAlpha<0.01) discard; finalResult = vec4(zDistance, 0.0, zDistance * zDistance, 1.0);\n"
            )
        )
        val copyRenderer = Renderer("copy", null)
        val triangleVisRenderer = SimpleRenderer(
            "randomId", ShaderStage(
                listOf(
                    Variable(GLSLType.V1I, "randomId"),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                ),
                "" +
                        "if(finalAlpha<0.01) discard;\n" +
                        "float flRandomId = float(randomId);\n" +
                        "vec2 seed = vec2(sin(flRandomId), cos(flRandomId));\n" +
                        RandomEffect.randomFunc +
                        "finalResult = vec4(GET_RANDOM(seed.xy), GET_RANDOM(seed.yx), GET_RANDOM(100.0 - seed.yx), 1.0);\n"
            )
        )
        val motionVectorRenderer = attributeRenderers[DeferredLayerType.MOTION]

        val uvRenderer = object : SimpleRenderer(
            "uv-checker", ShaderStage(
                listOf(
                    Variable(GLSLType.V2F, "uv"),
                    Variable(GLSLType.S2D, "checkerTex"),
                    Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                ), "finalResult = vec4(texture(checkerTex, uv).rgb, 1.0);\n"
            )
        ) {
            override fun uploadDefaultUniforms(shader: Shader) {
                super.uploadDefaultUniforms(shader)
                val checkerTex = ImageGPUCache.getImage(UVChecker.value, true) ?: whiteTexture
                checkerTex.bind(shader, "checkerTex", GPUFiltering.LINEAR, Clamping.REPEAT)
            }
        }

    }

}