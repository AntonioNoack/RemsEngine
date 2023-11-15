package me.anno.gpu.shader

import me.anno.cache.ICacheData
import me.anno.ecs.components.mesh.MeshInstanceData
import me.anno.ecs.components.mesh.MeshVertexData
import me.anno.engine.ui.render.Renderers.rawAttributeRenderers
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.maths.Maths.hasFlag
import me.anno.maths.Maths.max
import me.anno.utils.structures.lists.Lists.none2
import me.anno.utils.types.Booleans.toInt

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

    data class ShaderKey(
        val renderer: Renderer,
        val vertexData: MeshVertexData,
        val instanceData: MeshInstanceData,
        val flags: Int
    )

    private val shaders = HashMap<ShaderKey, Shader>()

    /** shader for forward rendering */
    open fun createForwardShader(key: ShaderKey): Shader {

        val flags = key.flags
        val vertexPostProcessing = key.renderer.getVertexPostProcessing(flags)
        val pixelPostProcessing = key.renderer.getPixelPostProcessing(flags)

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

        val builder = ShaderBuilder(name)

        val vs = ShaderStage("forward-v", vertexVariables, varyings, true, vertexShader)
        val fs = ShaderStage("forward-f", fragmentVariables, varyings, false, fragmentShader)

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
        builder.addVertex(vertexPostProcessing)
        builder.addFragment(fs)
        builder.addFragment(extraStage)
        builder.addFragment(pixelPostProcessing)
        // builder.addFragment(ColorAlphaStage.createShaderStage())

        val shader = builder.create("fwd$flags")
        finish(shader, 330)
        return shader
    }

    val value: Shader
        get() {
            GFX.check()
            val renderer = GFXState.currentRenderer
            val animated = GFXState.animated.currentValue
            val instanceData = GFXState.instanceData.currentValue
            val vertexData = GFXState.vertexData.currentValue
            val isDepth = renderer == Renderer.nothingRenderer
            val flags = animated.toInt(IS_ANIMATED) or
                    motionVectors.toInt(NEEDS_MOTION_VECTORS) or
                    (!isDepth).toInt(NEEDS_COLORS) or
                    (instanceData != MeshInstanceData.DEFAULT).toInt(IS_INSTANCED) or
                    (renderer.deferredSettings != null).toInt(IS_DEFERRED)
            val key = ShaderKey(renderer, vertexData, instanceData, flags)
            val shader = shaders.getOrPut(key) {
                val r = key.renderer
                val d = r.deferredSettings
                if (d != null) createDeferredShader(key)
                else createForwardShader(key)
            }
            GFX.check()
            if (shader.use()) {
                val instanced = instanceData !== MeshInstanceData.DEFAULT
                bind(shader, renderer, instanced)
                GFX.check()
            }
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
    open fun createDeferredShader(key: ShaderKey): Shader {
        val deferred = key.renderer.deferredSettings!!
        val flags = key.flags
        val shader = deferred.createShader(
            name,
            flags.hasFlag(IS_INSTANCED),
            vertexVariables,
            vertexShader,
            varyings,
            fragmentVariables,
            fragmentShader,
            textures,
            key.renderer.getVertexPostProcessing(flags),
            key.renderer.getPixelPostProcessing(flags)
        )
        finish(shader)
        return shader
    }

    fun finish(shader: Shader, minVersion: Int = 0) {
        shader.glslVersion = max(glslVersion, minVersion)
        shader.use()
        shader.setTextureIndices(textures)
        shader.ignoreNameWarnings(ignoredNameWarnings)
        shader.v4f("tint", 1f)
        GFX.check()
    }

    open fun concatDefines(key: ShaderKey, dst: StringBuilder = StringBuilder()): StringBuilder {
        val flags = key.flags
        if (flags.hasFlag(IS_INSTANCED)) dst.append("#define INSTANCED\n")
        if (flags.hasFlag(IS_ANIMATED)) dst.append("#define ANIMATED\n")
        if (flags.hasFlag(IS_DEFERRED)) dst.append("#define DEFERRED\n")
        if (flags.hasFlag(NEEDS_COLORS)) dst.append("#define COLORS\n")
        if (flags.hasFlag(NEEDS_MOTION_VECTORS)) dst.append("#define MOTION_VECTORS\n")
        return dst
    }

    override fun destroy() {
        for (shader in shaders.values) {
            shader.destroy()
        }
    }

    companion object {

        const val IS_INSTANCED = 1
        const val IS_ANIMATED = 2
        const val IS_DEFERRED = 4

        const val NEEDS_COLORS = 8
        const val NEEDS_MOTION_VECTORS = 16

        val motionVectors
            get(): Boolean {
                val renderer = GFXState.currentRenderer
                return (renderer == Renderer.motionVectorRenderer ||
                        renderer == rawAttributeRenderers[DeferredLayerType.MOTION] ||
                        renderer.deferredSettings != null && DeferredLayerType.MOTION in renderer.deferredSettings.layerTypes)
            }
    }
}