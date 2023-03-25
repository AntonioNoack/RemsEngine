package me.anno.gpu.shader

import me.anno.cache.ICacheData
import me.anno.engine.ui.render.Renderers.rawAttributeRenderers
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
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
 * converts a shader with color, normal, tint and such into
 *  a) a clickable / depth-able shader
 *  b) a flat color shader
 *  c) a deferred shader
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
    private val depthShader = Array(5) { lazy { createDepthShader(it.hasFlag(1), it.hasFlag(2), it.hasFlag(4)) } }

    /** shader for rendering the depth, e.g., for pre-depth */
    open fun createDepthShader(isInstanced: Boolean, isAnimated: Boolean, limitedTransform: Boolean): Shader {
        if (vertexShader.isBlank2()) throw RuntimeException()
        var vertexShader = vertexShader
        if (isInstanced) vertexShader = "#define INSTANCED\n$vertexShader"
        if (isAnimated) vertexShader = "#define ANIMATED\n$vertexShader"
        if (limitedTransform) vertexShader = "#define LIMITED_TRANSFORM\n$vertexShader"
        return Shader(name, vertexVariables, vertexShader, varyings, fragmentVariables, "void main(){}")
    }

    /** shader for forward rendering */
    open fun createForwardShader(
        postProcessing: ShaderStage?,
        isInstanced: Boolean,
        isAnimated: Boolean,
        motionVectors: Boolean,
        limitedTransform: Boolean,
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
        if (isInstanced) vs.define("INSTANCED")
        if (isAnimated) vs.define("ANIMATED")
        if (motionVectors) vs.define("MOTION_VECTORS")

        val fs = ShaderStage(fragmentVariables, varyings, false, fragmentShader)
        if (isInstanced) fs.define("INSTANCED")
        if (isAnimated) fs.define("ANIMATED")
        if (motionVectors) fs.define("MOTION_VECTORS")

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
            val limited = GFXState.limitedTransform.currentValue
            val stateId = min(instanced.toInt() + animated.toInt(2) + motionVectors.toInt(4) + limited.toInt(8), 8)
            val shader = if (renderer == Renderer.nothingRenderer) {
                depthShader[if (limited) 4 else stateId and 3].value
            } else when (val deferred = renderer.deferredSettings) {
                null -> {
                    flatShader.getOrPut(renderer, stateId) { r, stateId2 ->
                        val shader = createForwardShader(
                            r.getPostProcessing(),
                            stateId2.hasFlag(1),
                            stateId2.hasFlag(2),
                            stateId2.hasFlag(4),
                            stateId2.hasFlag(8)
                        )
                        // LOGGER.info(shader.fragmentSource)
                        shader
                    }
                }
                else -> createDeferredShader(deferred, stateId, renderer.getPostProcessing())
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
        isInstanced: Boolean,
        isAnimated: Boolean,
        motionVectors: Boolean,
        limitedTransform: Boolean,
        postProcessing: ShaderStage?,
    ): Shader {
        val shader = deferred.createShader(
            name,
            isInstanced,
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

    fun createDeferredShader(settings: DeferredSettingsV2, stateId: Int, postProcessing: ShaderStage?): Shader {
        return deferredShaders.getOrPut(settings, stateId) { settings2, stateId2 ->
            this.createDeferredShader(
                settings2,
                stateId2.hasFlag(1),
                stateId2.hasFlag(2),
                stateId2.hasFlag(4),
                stateId2.hasFlag(8),
                postProcessing,
            )
        }
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
        val motionVectors
            get(): Boolean {
                val renderer = GFXState.currentRenderer
                return (renderer == Renderer.motionVectorRenderer ||
                        renderer == rawAttributeRenderers[DeferredLayerType.MOTION] ||
                        renderer.deferredSettings != null && DeferredLayerType.MOTION in renderer.deferredSettings.layerTypes)
            }
    }

}