package me.anno.gpu.shader.builder

import me.anno.gpu.DitherMode
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.Shader
import me.anno.utils.structures.arrays.BooleanArrayList
import kotlin.math.max

/**
 * Combines multiple vertex/fragment stages into a single shader.
 * Resolves variables and functions somewhat.
 * */
class ShaderBuilder(val name: String) {

    constructor(name: String, settingsV2: DeferredSettings?, ditherMode: DitherMode) : this(name) {
        settings = settingsV2
        this.ditherMode = ditherMode
    }

    var ditherMode = GFXState.ditherMode.currentValue

    // there exist 3 passes: vertex, fragment, geometry
    // input variables can be defined on multiple ways: uniforms / attributes
    // output colors can be further processed

    val vertex = MainStage().apply {
        define(Variable(GLSLType.V4F, "gl_Position", true))
    }

    val fragment = MainStage()

    var settings: DeferredSettings? = null
    var disabledLayers: BooleanArrayList? = null
    var useRandomness = true

    var glslVersion = GPUShader.DefaultGLSLVersion

    fun addVertex(stage: ShaderStage?) {
        vertex.add(stage ?: return)
    }

    fun addVertex(stages: List<ShaderStage>) {
        for (stage in stages) vertex.add(stage)
    }

    fun addFragment(stage: ShaderStage?) {
        fragment.add(stage ?: return)
    }

    fun addFragment(stages: List<ShaderStage>) {
        for (stage in stages) fragment.add(stage)
    }

    private fun sortedAdd(textureIndices: MutableList<String>, name: String) {
        val idx = textureIndices.binarySearch(name)
        if (idx < 0) textureIndices.add(-idx - 1, name)
    }

    private fun collectTextureIndices(textureIndices: MutableList<String>, uniforms: Collection<Variable>) {
        for (uniform in uniforms) {
            if (uniform.type.isSampler) {
                if (uniform.arraySize >= 0) {
                    if ("${uniform.name}0" !in textureIndices) {
                        for (i in 0 until uniform.arraySize) {
                            sortedAdd(textureIndices, uniform.name + i)
                        }
                    }
                } else {
                    sortedAdd(textureIndices, uniform.name)
                }
            }
        }
    }

    private fun handleWorkToDataAccessors(settings: DeferredSettings) {
        val addedVariables = ArrayList<Variable>()
        for (layer in settings.semanticLayers) {
            val w2d = layer.type.workToData
            val idx = w2d.indexOf('.')
            if (idx > 0) {
                addedVariables.add(Variable(GLSLType.V4F, w2d.substring(0, idx)))
            }
        }
        if (addedVariables.isNotEmpty()) {
            if (fragment.stages.isEmpty()) {
                fragment.stages.add(ShaderStage("?", addedVariables, ""))
            } else {
                fragment.stages.last().addVariables(addedVariables)
            }
        }
    }

    fun create(key: BaseShader.ShaderKey, suffix: String): Shader {

        val settings = settings
        val ditherMode = GFXState.ditherMode.currentValue
        if (settings != null) {
            handleWorkToDataAccessors(settings)
        }

        // combine the code
        // find imports
        val vertexDefined = vertex.findImportsAndDefineValues(null, emptySet(), emptySet())
        fragment.findImportsAndDefineValues(vertex, vertexDefined, vertex.uniforms)

        // variables, that fragment imports & exports & vertex exports
        val bridgeVariablesV2F = HashMap<Variable, Variable>()
        val bridgeVariablesI2F = HashMap<Variable, Variable>()
        var bridgeIndex = 0
        for (variable in vertexDefined) {
            val name = variable.name
            if (vertex.stages.any { it.getVariablesByName(name).any { v -> v.isOutput } }) {
                for (stage in fragment.stages) {
                    val byName = stage.getVariablesByName(name)
                    if (byName.any { it.isInput } && byName.any { it.isModified }) {
                        // the stage uses it -> might be relevant
                        // and also exports it -> build a bridge
                        val bridge = Variable(variable.type, "vf_bridge_${bridgeIndex++}", variable.arraySize)
                        bridgeVariablesV2F[variable] = bridge
                    }
                }
            } else if ( // the following types cannot be used as varyings...
                !(variable.type.isSampler || variable.type == GLSLType.V1B || variable.type == GLSLType.V2B ||
                        variable.type == GLSLType.V3B || variable.type == GLSLType.V4B)
            ) { // test if we need an attribute-fragment-bridge
                if (vertex.stages.any { it.getVariablesByName(name).any { v -> v.isAttribute } }) {
                    for (stage in fragment.stages) {
                        if (stage.getVariablesByName(name).any { it.isInput }) {
                            val bridge = Variable(variable.type, "attr_bridge_${bridgeIndex++}", variable.arraySize)
                            bridge.isFlat = variable.isFlat
                            bridgeVariablesI2F[variable] = bridge
                        }
                    }
                }
            }
        }

        // create the code
        val vertCode = vertex.createCode(
            key, false, settings, disabledLayers,
            ditherMode, bridgeVariablesV2F, bridgeVariablesI2F, this
        )
        val fragCode = fragment.createCode(
            key, true, settings, disabledLayers,
            ditherMode, bridgeVariablesV2F, bridgeVariablesI2F, this
        )
        val varying = (vertex.imported + vertex.exported).toList()
            .filter { it !in bridgeVariablesV2F && it !in bridgeVariablesI2F } +
                bridgeVariablesV2F.values +
                bridgeVariablesI2F.values

        val shader = Shader(
            "$name-$suffix", vertex.attributes + vertex.uniforms, vertCode,
            varying, fragment.uniforms.sortedBy { it.name }, fragCode
        )
        shader.glslVersion = max(330, max(glslVersion, shader.glslVersion))
        val textureIndices = ArrayList<String>()
        collectTextureIndices(textureIndices, vertex.uniforms)
        collectTextureIndices(textureIndices, fragment.uniforms)
        shader.setTextureIndices(textureIndices)
        return shader
    }
}