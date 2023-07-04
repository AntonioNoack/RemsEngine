package me.anno.gpu.shader

import me.anno.cache.ICacheData
import me.anno.engine.ui.render.Renderers.rawAttributeRenderers
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.ShaderLib.matMul
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.Maths.hasFlag
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.utils.structures.lists.Lists.none2
import me.anno.utils.structures.maps.KeyPairMap
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.isBlank2

/**
 * converts a complex shader into
 *  a) depth shader
 *  b) forward shader
 *  c) deferred shader with flexible targets
 * */
open class BaseShader(
    val name: String,
    val vertexVariables: List<Variable>,
    val vertexShader: String,
    val varyings: List<Variable>,
    var fragmentVariables: List<Variable>,
    var fragmentShader: String
) : ICacheData { // Saveable or PrefabSaveable?

    constructor(name: String, vertexSource: String, varyingSource: List<Variable>, fragmentSource: String) :
            this(name, emptyList(), vertexSource, varyingSource, emptyList(), fragmentSource)

    constructor() : this("", "", emptyList(), "")

    var glslVersion = OpenGLShader.DefaultGLSLVersion
    var textures: List<String>? = null
    var ignoredNameWarnings = HashSet<String>()

    private val flatShader = KeyPairMap<Renderer, Int, Shader>()
    private val deferredShaders = KeyPairMap<DeferredSettingsV2, Int, Shader>()
    private val depthShader = Array(5) {
        lazy {
            createDepthShader(
                it.hasFlag(1).toInt(IS_INSTANCED) +
                        it.hasFlag(2).toInt(IS_ANIMATED) +
                        it.hasFlag(4).toInt(USES_PRS_TRANSFORM)
            )
        }
    }

    /** shader for rendering the depth, e.g., for pre-depth */
    open fun createDepthShader(flags: Int): Shader {
        if (vertexShader.isBlank2()) throw RuntimeException()
        var vertexShader = vertexShader
        val builder = StringBuilder()
        createDefines(flags, builder)
        builder.append(vertexShader)
        vertexShader = builder.toString()
        return Shader(name, vertexVariables, vertexShader, varyings, fragmentVariables, "void main(){}")
    }

    /** shader for forward rendering */
    open fun createForwardShader(
        flags: Int,
        postProcessing: ShaderStage?,
    ): Shader {

        var extraStage: ShaderStage? = null
        if (fragmentVariables.none2 { it.isOutput } && fragmentShader.contains("gl_FragColor")) {
            fragmentVariables += Variable(GLSLType.V4F, "glFragColor", VariableMode.OUT)
            fragmentShader = fragmentShader.replace("gl_FragColor", "glFragColor")
            extraStage = ShaderStage(
                "extra", listOf(
                    Variable(GLSLType.V4F, "glFragColor", VariableMode.IN),
                    Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                    Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT)
                ), "finalColor = glFragColor.rgb; finalAlpha = glFragColor.a;\n"
            )
        }


        // todo add IS_TINTED to all RemsStudio shaders, if applicable
        // val hasTint = "vec4 tint;" in fragmentShader || "tint" in varyings.map { it.name }

        val builder = ShaderBuilder(name)

        val vs = ShaderStage(vertexVariables, varyings, true, vertexShader)
        val fs = ShaderStage(fragmentVariables, varyings, false, fragmentShader)

        if (flags.hasFlag(IS_INSTANCED)) {
            vs.define("INSTANCED")
            fs.define("INSTANCED")
        }

        if (flags.hasFlag(IS_ANIMATED)) {
            vs.define("ANIMATED")
            fs.define("ANIMATED")
        }

        if (flags.hasFlag(NEEDS_MOTION_VECTORS)) {
            vs.define("MOTION_VECTORS")
            fs.define("MOTION_VECTORS")
        }

        builder.addVertex(vs)
        builder.addFragment(fs)
        builder.addFragment(extraStage)
        builder.addFragment(postProcessing)
        // builder.addFragment(ColorAlphaStage.createShaderStage())

        val shader1 = builder.create()
        shader1.glslVersion = max(shader1.glslVersion, 330)
        shader1.setTextureIndices(textures)
        shader1.ignoreNameWarnings(ignoredNameWarnings)
        shader1.use()
        shader1.v4f("tint", 1f)
        return shader1

    }

    val value: Shader
        get() {
            GFX.check()
            val renderer = GFXState.currentRenderer
            val instanced = GFXState.instanced.currentValue
            val animated = GFXState.animated.currentValue
            val limited = GFXState.prsTransform.currentValue
            val stateId = min(instanced.toInt() + animated.toInt(2) + motionVectors.toInt(4) + limited.toInt(8), 8)
            val shader = if (renderer == Renderer.nothingRenderer) {
                depthShader[if (limited) 4 else stateId and 3].value
            } else when (val deferred = renderer.deferredSettings) {
                null -> {
                    flatShader.getOrPut(renderer, stateId) { r, stateId2 ->
                        val flags = stateId2.hasFlag(1).toInt(IS_INSTANCED) +
                                stateId2.hasFlag(2).toInt(IS_ANIMATED) +
                                stateId2.hasFlag(4).toInt(NEEDS_MOTION_VECTORS) +
                                stateId2.hasFlag(8).toInt(USES_PRS_TRANSFORM) +
                                NEEDS_COLORS
                        createForwardShader(flags, r.getPostProcessing(flags))
                    }
                }
                else -> createDeferredShaderById(deferred, stateId, renderer)
            }
            GFX.check()
            if (shader.use())
                bind(shader, renderer, instanced)
            GFX.check()
            return shader
        }

    open fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        renderer.uploadDefaultUniforms(shader)
    }

    fun ignoreNameWarnings(names: Collection<String>) {
        ignoredNameWarnings += names
    }

    fun ignoreNameWarnings(vararg names: String): BaseShader {
        ignoredNameWarnings += names
        return this
    }

    fun ignoreUniformWarning(name: String): BaseShader {
        ignoredNameWarnings += name
        return this
    }

    fun setTextureIndices(textures: List<String>?) {
        this.textures = textures
    }

    /** shader for deferred rendering */
    open fun createDeferredShader(
        deferred: DeferredSettingsV2,
        flags: Int,
        postProcessing: ShaderStage?,
    ): Shader {
        val shader = deferred.createShader(
            name,
            flags.hasFlag(IS_INSTANCED),
            vertexVariables,
            vertexShader,
            varyings,
            fragmentVariables,
            fragmentShader,
            textures,
            postProcessing
        )
        finish(shader)
        return shader
    }

    fun finish(shader: Shader) {
        shader.glslVersion = glslVersion
        shader.use()
        shader.setTextureIndices(textures)
        shader.ignoreNameWarnings(ignoredNameWarnings)
        shader.v4f("tint", 1f)
        GFX.check()
    }

    private fun createDeferredShaderById(
        settings: DeferredSettingsV2,
        stateId: Int,
        renderer: Renderer
    ): Shader {
        return deferredShaders.getOrPut(settings, stateId) { settings2, stateId2 ->
            val flags = stateId2.hasFlag(1).toInt(IS_INSTANCED) +
                    stateId2.hasFlag(2).toInt(IS_ANIMATED) +
                    stateId2.hasFlag(4).toInt(NEEDS_MOTION_VECTORS) +
                    stateId2.hasFlag(8).toInt(USES_PRS_TRANSFORM) +
                    NEEDS_COLORS + IS_DEFERRED
            val postProcessing = renderer.getPostProcessing(flags)
            createDeferredShader(settings2, flags, postProcessing)
        }
    }

    open fun createDefines(flags: Int, dst: StringBuilder = StringBuilder()): StringBuilder {
        dst.append(matMul)
        if (flags.hasFlag(IS_INSTANCED)) dst.append("#define INSTANCED\n")
        if (flags.hasFlag(IS_ANIMATED)) dst.append("#define ANIMATED\n")
        if (flags.hasFlag(IS_DEFERRED)) dst.append("#define DEFERRED\n")
        if (flags.hasFlag(NEEDS_COLORS)) dst.append("#define COLORS\n")
        if (flags.hasFlag(NEEDS_MOTION_VECTORS)) dst.append("#define MOTION_VECTORS\n")
        if (flags.hasFlag(USES_PRS_TRANSFORM)) dst.append("#define PRS_TRANSFORM\n")
        return dst
    }

    override fun destroy() {
        for (list in flatShader) {
            for ((_, shader) in list) {
                shader.destroy()
            }
        }
        for (list in deferredShaders) {
            for ((_, shader) in list) {
                shader.destroy()
            }
        }
    }

    companion object {

        const val IS_INSTANCED = 1
        const val IS_ANIMATED = 2
        const val IS_DEFERRED = 4

        const val NEEDS_COLORS = 8
        const val NEEDS_MOTION_VECTORS = 16

        /**
         * an optimized transform, where only position, rotation, and uniform scale are supported;
         * for instanced rendering without motion, and reduced memory-bandwidth usage
         * */
        const val USES_PRS_TRANSFORM = 32

        val motionVectors
            get(): Boolean {
                val renderer = GFXState.currentRenderer
                return (renderer == Renderer.motionVectorRenderer ||
                        renderer == rawAttributeRenderers[DeferredLayerType.MOTION] ||
                        renderer.deferredSettings != null && DeferredLayerType.MOTION in renderer.deferredSettings.layerTypes)
            }
    }

}