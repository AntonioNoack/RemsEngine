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
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib
import me.anno.language.translation.NameDesc
import me.anno.maths.Packing.pack32
import me.anno.utils.OS.res
import org.apache.logging.log4j.LogManager

/**
 * defines render targets combined with post-processing
 * @param deferredSettings null if not rendering multiple targets
 * */
open class Renderer(val nameDesc: NameDesc, val deferredSettings: DeferredSettings?) {

    class SplitRenderer(name: NameDesc, settings: DeferredSettings, val base: Renderer) : Renderer(name, settings) {
        override fun getVertexPostProcessing(flags: Int) = base.getVertexPostProcessing(flags)
        override fun getPixelPostProcessing(flags: Int) = base.getPixelPostProcessing(flags)
        override fun bind(shader: Shader) = base.bind(shader)
    }

    constructor(name: String, deferredSettings: DeferredSettings?) : this(NameDesc(name), deferredSettings)
    constructor(name: NameDesc) : this(name, null)
    constructor(name: String) : this(NameDesc(name))

    open fun getVertexPostProcessing(flags: Int): List<ShaderStage> = emptyList()
    open fun getPixelPostProcessing(flags: Int): List<ShaderStage> = emptyList()

    open fun bind(shader: Shader) {}

    fun split(index: Int, spliceSize: Int): Renderer {
        if (deferredSettings == null) {
            LOGGER.warn("Splitting non-deferred renderer??? ${nameDesc.name}")
            return this
        }
        return splitCache!!.getOrPut(pack32(index, spliceSize)) {
            val settings = deferredSettings.split(index, spliceSize)
            SplitRenderer(NameDesc("${nameDesc.name}/$index/$spliceSize"), settings, this)
        }
    }

    val splitCache = if (deferredSettings == null) null else HashMap<Int, Renderer>()

    override fun toString(): String = nameDesc.name

    companion object {
        private val LOGGER = LogManager.getLogger(Renderer::class)

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
                ), "if(finalAlpha < 0.01) { discard; }\nfinalResult = finalId;\n"
            )
        )

        // randomness based on object position? -> we don't have that in the shader though :/
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
                        "   random(vec2(id,9.0)), 1.0);\n"
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

        val copyRenderer = Renderer("copy")
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
            private val uvCheckerSource = res.getChild("textures/UVChecker.png")
            override fun bind(shader: Shader) {
                super.bind(shader)
                val checkerTex = TextureCache[uvCheckerSource].value ?: TextureLib.whiteTexture
                checkerTex.bind(shader, "checkerTex", Filtering.LINEAR, Clamping.REPEAT)
            }
        }

        val motionVectorRenderer get() = Renderers.attributeRenderers[DeferredLayerType.MOTION]
    }
}