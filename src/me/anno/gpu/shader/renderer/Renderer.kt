package me.anno.gpu.shader.renderer

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.Renderers
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib
import me.anno.gpu.texture.TextureCache
import me.anno.io.files.FileReference.Companion.getReference

/**
 * defines render targets combined with post-processing
 * @param deferredSettings null if not rendering multiple targets
 * */
open class Renderer(val name: String, val deferredSettings: DeferredSettings?) {

    constructor(name: String): this(name, null)

    open fun getVertexPostProcessing(flags: Int): List<ShaderStage> = emptyList()
    open fun getPixelPostProcessing(flags: Int): List<ShaderStage> = emptyList()

    open fun uploadDefaultUniforms(shader: Shader) {}

    fun split(index: Int, spliceSize: Int): Renderer {
        if (deferredSettings == null) return this
        return cache!!.getOrPut(index.shl(16) + spliceSize) {
            val settings = deferredSettings.split(index, spliceSize)
            object : Renderer("$name/$index/$spliceSize", settings) {
                override fun getVertexPostProcessing(flags: Int) = this@Renderer.getVertexPostProcessing(flags)
                override fun getPixelPostProcessing(flags: Int) = this@Renderer.getPixelPostProcessing(flags)
                override fun uploadDefaultUniforms(shader: Shader) = this@Renderer.uploadDefaultUniforms(shader)
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
                        ECSMeshShader.colorToSRGB +
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
                        ECSMeshShader.colorToSRGB +
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
                    Variable(GLSLType.V4F, "finalId"),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "finalResult", VariableMode.OUT),
                ), "if(finalAlpha < 0.01) discard; finalResult = finalId;\n"
            )
        )

        // todo randomness based on object position?
        val randomIdRenderer = SimpleRenderer(
            "randomId", ShaderStage(
                "randomId", listOf(
                    Variable(GLSLType.V4F, "finalId"),
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "finalResult", VariableMode.OUT),
                ), "if(finalAlpha < 0.01) discard;\n" +
                        "float id = dot(finalId,vec4(65536.0,256.0,1.0,1.0/256.0));\n" +
                        "finalResult = vec4(\n" +
                        "   random(vec2(id,1.0)),\n" +
                        "   random(vec2(id,5.0)),\n" +
                        "   random(vec2(id,9.0)), 1.0);\n" +
                        // can be used to get a non-random look
                        "// finalResult = vec4(vec3(fract(finalId.r * 5.2)),1.0);\n"
            ).add(ShaderFuncLib.randomGLSL)
        )

        val nothingRenderer = SimpleRenderer("depth", ShaderStage("", emptyList(), ""))
        val depthRenderer = SimpleRenderer(
            "depth", ShaderStage(
                "depth", listOf(
                    Variable(GLSLType.V1F, "finalAlpha"),
                    Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                ), "if(finalAlpha<0.01) { discard; }\n" +
                        "float zDistance = 1.0 / gl_FragCoord.w;\n" +
                        "finalResult = vec4(zDistance, 0.0, zDistance * zDistance, 1.0);\n"
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
            ).add(ShaderFuncLib.randomGLSL)
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
            private val uvCheckerSource = getReference("res://textures/UVChecker.png")
            override fun uploadDefaultUniforms(shader: Shader) {
                super.uploadDefaultUniforms(shader)
                val checkerTex = TextureCache[uvCheckerSource, true] ?: TextureLib.whiteTexture
                checkerTex.bind(shader, "checkerTex", Filtering.LINEAR, Clamping.REPEAT)
            }
        }

        val motionVectorRenderer get() = Renderers.attributeRenderers[DeferredLayerType.MOTION]
    }
}