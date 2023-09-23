package me.anno.gpu.shader

import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.ShaderFuncLib.randomGLSL
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.ImageGPUCache
import me.anno.utils.files.UVChecker

/**
 * defines render targets combined with post-processing
 * @param deferredSettings null if not rendering multiple targets
 * */
open class Renderer(val name: String, val deferredSettings: DeferredSettingsV2? = null) {

    open fun getPostProcessing(flags: Int): List<ShaderStage> = emptyList()

    open fun uploadDefaultUniforms(shader: Shader) {}

    fun split(index: Int, spliceSize: Int): Renderer {
        if (deferredSettings == null) return this
        return cache!!.getOrPut(index.shl(16) + spliceSize) {
            val settings = deferredSettings.split(index, spliceSize)
            object : Renderer("$name/$index/$spliceSize", settings) {
                override fun getPostProcessing(flags: Int) = this@Renderer.getPostProcessing(flags)
                override fun uploadDefaultUniforms(shader: Shader) {
                    this@Renderer.uploadDefaultUniforms(shader)
                }
            }
        }
    }

    val cache = if (deferredSettings == null) null else HashMap<Int, Renderer>()

    override fun toString(): String = name

    companion object {

        val colorRenderer = SimpleRenderer(
            "color", ShaderStage(
                "color", listOf(
                    Variable(GLSLType.V4F, "tint"),
                    Variable(GLSLType.V3F, "finalColor", VariableMode.INMOD),
                    Variable(GLSLType.V3F, "finalEmissive", VariableMode.INMOD),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "SPResult", VariableMode.OUT),
                ),
                "" +
                        colorToSRGB +
                        "SPResult = vec4(finalColor\n" +
                        "   #ifndef IS_TINTED\n * tint.rgb\n #endif\n," +
                        "clamp(finalAlpha\n #ifndef IS_TINTED\n * tint.a\n #endif\n, 0.0, 1.0));\n"
            )
        )

        @Suppress("unused")
        val colorSqRenderer = SimpleRenderer(
            "colorSq", ShaderStage(
                "colorSq", listOf(
                    Variable(GLSLType.V4F, "tint"),
                    Variable(GLSLType.V3F, "finalColor", VariableMode.INMOD),
                    Variable(GLSLType.V3F, "finalEmissive", VariableMode.INMOD),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "SPResult", VariableMode.OUT),
                ),
                "" +
                        colorToSRGB +
                        "vec3 tmpCol = finalColor\n" +
                        "#ifndef IS_TINTED\n" +
                        " * tint.rgb\n" +
                        "#endif\n" +
                        ";SPResult = vec4(tmpCol * tmpCol, clamp(finalAlpha, 0.0, 1.0) * tint.a);\n"
            )
        )

        val idRenderer = SimpleRenderer(
            "id", ShaderStage(
                "id", listOf(
                    Variable(GLSLType.V4F, "gfxId"),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "finalResult", VariableMode.OUT),
                ), "if(finalAlpha < 0.01) discard; finalResult = gfxId;\n"
            )
        )

        val groupRenderer = SimpleRenderer(
            "id", ShaderStage(
                "id", listOf(
                    Variable(GLSLType.V4F, "gfxId"),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "finalResult", VariableMode.OUT),
                ), "if(finalAlpha < 0.01) discard; finalResult = vec4(gfxId.w);\n"
            )
        )

        // todo randomness based on object position?
        val randomIdRenderer = SimpleRenderer(
            "randomId", ShaderStage(
                "randomId", listOf(
                    Variable(GLSLType.V4F, "gfxId"),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "finalResult", VariableMode.OUT),
                ), "if(finalAlpha < 0.01) discard;\n" +
                        "float id = dot(gfxId,vec4(65536.0,256.0,1.0,1.0/256.0));\n" +
                        "finalResult = vec4(\n" +
                        "   random(vec2(id,1.0)),\n" +
                        "   random(vec2(id,5.0)),\n" +
                        "   random(vec2(id,9.0)), 1.0);\n"
            ).add(randomGLSL)
        )

        val nothingRenderer = SimpleRenderer("depth", ShaderStage("", emptyList(), ""))
        val depthRenderer = SimpleRenderer(
            "depth", ShaderStage(
                "depth", listOf(
                    Variable(GLSLType.V1F, "zDistance"),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                ), "if(finalAlpha<0.01) discard; finalResult = vec4(zDistance, 0.0, zDistance * zDistance, 1.0);\n"
            )
        )
        val copyRenderer = Renderer("copy", null)
        val triangleVisRenderer = SimpleRenderer(
            "triangleVis", ShaderStage(
                "triangleVis", listOf(
                    Variable(GLSLType.V1I, "randomId"),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                ),
                "" +
                        "if(finalAlpha<0.01) discard;\n" +
                        "float flRandomId = float(randomId);\n" +
                        "vec2 seed = vec2(sin(flRandomId), cos(flRandomId));\n" +
                        "finalResult = vec4(random(seed.xy), random(seed.yx), random(100.0 - seed.yx), 1.0);\n"
            ).add(randomGLSL)
        )

        val uvRenderer = object : SimpleRenderer(
            "uv-checker", ShaderStage(
                "uv-checker", listOf(
                    Variable(GLSLType.V2F, "uv"),
                    Variable(GLSLType.S2D, "checkerTex"),
                    Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                ), "finalResult = vec4(texture(checkerTex, uv).rgb, 1.0);\n"
            )
        ) {
            override fun uploadDefaultUniforms(shader: Shader) {
                super.uploadDefaultUniforms(shader)
                val checkerTex = ImageGPUCache[UVChecker.value, true] ?: whiteTexture
                checkerTex.bind(shader, "checkerTex", GPUFiltering.LINEAR, Clamping.REPEAT)
            }
        }

        val motionVectorRenderer get() = attributeRenderers[DeferredLayerType.MOTION]
    }
}