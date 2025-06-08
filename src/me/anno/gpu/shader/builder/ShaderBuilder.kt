package me.anno.gpu.shader.builder

import me.anno.gpu.DitherMode
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.Shader
import me.anno.utils.structures.arrays.BooleanArrayList
import me.anno.utils.structures.lists.Lists.any2
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

    var glslVersion = GPUShader.DEFAULT_GLSL_VERSION

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

    private fun needsVertexFragmentBridge(variable: Variable): Boolean {
        val name = variable.name
        return vertex.stages.any2 { it.getVariablesByName(name).any2 { v -> v.isOutput } } &&
                fragment.stages.any2 { fStage ->
                    val byName = fStage.getVariablesByName(name)
                    byName.any2 { it.isInput } && byName.any2 { it.isModified }
                }
    }

    private fun createVertexFragmentBridge(
        variable: Variable, bridgeIndex0: Int,
        bridges: HashMap<Variable, Variable>
    ): Int {
        // the stage uses it -> might be relevant
        // and also exports it -> build a bridge
        val bridge = Variable(variable.type, "vf_bridge_${bridgeIndex0}", variable.arraySize)
        bridges[variable] = bridge
        return bridgeIndex0 + 1
    }

    private fun canUseVariableAsVarying(type: GLSLType): Boolean {
        // the following types cannot be used as varyings...
        return !(type.isSampler || type == GLSLType.V1B || type == GLSLType.V2B || type == GLSLType.V3B || type == GLSLType.V4B)
    }

    private fun needsAttribFragmentBridge(variable: Variable): Boolean {
        val name = variable.name
        return canUseVariableAsVarying(variable.type) &&
                vertex.stages.any2 { it.getVariablesByName(name).any2 { v -> v.isAttribute } } &&
                fragment.stages.any2 { it.getVariablesByName(name).any2 { v -> v.isInput } }
    }

    private fun createAttribFragmentBridge(
        variable: Variable, bridgeIndex0: Int,
        bridges: HashMap<Variable, Variable>
    ): Int {
        val bridge = Variable(variable.type, "attr_bridge_${bridgeIndex0}", variable.arraySize)
        bridge.isFlat = variable.isFlat
        bridges[variable] = bridge
        return bridgeIndex0 + 1
    }

    private fun createBridgesForVariable(
        variable: Variable, bridgeIndex0: Int,
        vertexFragmentBridges: HashMap<Variable, Variable>,
        attribFragmentBridges: HashMap<Variable, Variable>
    ): Int {
        return when {
            needsVertexFragmentBridge(variable) ->
                createVertexFragmentBridge(variable, bridgeIndex0, vertexFragmentBridges)
            needsAttribFragmentBridge(variable) ->
                createAttribFragmentBridge(variable, bridgeIndex0, attribFragmentBridges)
            else -> bridgeIndex0
        }
    }

    private fun collectVaryings(
        bridgeVariablesV2F: Map<Variable, Variable>,
        bridgeVariablesI2F: Map<Variable, Variable>
    ): List<Variable> {
        val dst = ArrayList<Variable>(vertex.imported.size + vertex.exported.size)
        dst.addAll(vertex.imported)
        dst.addAll(vertex.exported)
        dst.removeIf { it in bridgeVariablesI2F || it in bridgeVariablesV2F }
        dst.addAll(bridgeVariablesV2F.values)
        dst.addAll(bridgeVariablesI2F.values)
        return dst
    }

    private fun findImports(): Set<Variable> {
        val vertexDefined = vertex.findImportsAndDefineValues(null, emptySet(), emptySet())
        fragment.findImportsAndDefineValues(vertex, vertexDefined, vertex.uniforms)
        return vertexDefined
    }

    fun create(key: BaseShader.ShaderKey, suffix: String): Shader {

        val settings = settings
        if (settings != null) {
            handleWorkToDataAccessors(settings)
        }

        // combine the code
        val vertexDefined = findImports()

        // variables, that fragment imports & exports & vertex exports
        val vertexFragmentBridges = HashMap<Variable, Variable>()
        val attribFragmentBridges = HashMap<Variable, Variable>()
        var bridgeIndex = 0
        for (variable in vertexDefined) {
            bridgeIndex = createBridgesForVariable(
                variable, bridgeIndex,
                vertexFragmentBridges, attribFragmentBridges
            )
        }

        // create the code
        val vertCode = vertex.createCode(
            key, false, settings, disabledLayers,
            key.ditherMode, vertexFragmentBridges, attribFragmentBridges, this
        )

        val fragCode = fragment.createCode(
            key, true, settings, disabledLayers,
            key.ditherMode, vertexFragmentBridges, attribFragmentBridges, this
        )

        val varyings = collectVaryings(vertexFragmentBridges, attribFragmentBridges)
        val shader = Shader(
            "$name-$suffix", vertex.attributes + vertex.uniforms, vertCode,
            varyings, fragment.uniforms.sortedBy { it.name }, fragCode
        )
        shader.glslVersion = max(330, max(glslVersion, shader.glslVersion))
        val textureIndices = ArrayList<String>()
        collectTextureIndices(textureIndices, vertex.uniforms)
        collectTextureIndices(textureIndices, fragment.uniforms)
        shader.setTextureIndices(textureIndices)
        return shader
    }
}