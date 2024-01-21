package me.anno.gpu.shader.builder

import me.anno.gpu.DitherMode
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.Shader
import java.util.BitSet
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
    var disabledLayers: BitSet? = null

    var glslVersion = GPUShader.DefaultGLSLVersion

    val ignored = HashSet<String>()

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

    fun create(suffix: String? = null): Shader {

        val settings = settings
        val ditherMode = GFXState.ditherMode.currentValue
        if (settings != null) {
            if (fragment.stages.isEmpty()) fragment.stages.add(ShaderStage("?", emptyList(), ""))
            val lastStage = fragment.stages.last()
            for (layer in settings.semanticLayers) {
                val w2d = layer.type.workToData
                val idx = w2d.indexOf('.')
                if (idx > 0) {
                    lastStage.variables += Variable(GLSLType.V4F, w2d.substring(0, idx))
                }
            }
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
            if (vertex.stages.any { it.variables.any { v -> v.name == name && v.isOutput } }) {
                for (stage in fragment.stages) {
                    if (stage.variables.any { it.isInput && name == it.name }) {
                        // the stage uses it -> might be relevant
                        if (stage.variables.any { it.isModified && name == it.name }) {
                            // the stage also exports it ->
                            bridgeVariablesV2F[variable] =
                                Variable(variable.type, "bridge_${bridgeIndex++}", variable.arraySize)
                        }
                    }/* else if (stage.variables.any { it.isOutput && name == it.name }) {
                        // this stage outputs it, but does not import it -> theoretically,
                        // we can skip the bridge here completely;
                        // in practice, idk whether variable shadowing is allowed like that
                    }*/
                }
            } else if (vertex.stages.any { it.variables.any { v -> v.name == name && !v.isOutput } }) {
                for (stage in fragment.stages) {
                    if (stage.variables.any { it.isInput && name == it.name }) {
                        bridgeVariablesI2F[variable] =
                            Variable(variable.type, "bridge_${bridgeIndex++}", variable.arraySize)
                                .apply {
                                    isFlat = variable.isFlat
                                }
                    }
                }
            }
        }

        // create the code
        val vertCode = vertex.createCode(
            false, settings, disabledLayers,
            ditherMode, bridgeVariablesV2F, bridgeVariablesI2F
        )
        val attributes = vertex.attributes
        val fragCode = fragment.createCode(
            true, settings, disabledLayers,
            ditherMode, bridgeVariablesV2F, bridgeVariablesI2F
        )
        val varying = (vertex.imported + vertex.exported).toList()
            .filter { it !in bridgeVariablesV2F && it !in bridgeVariablesI2F } +
                bridgeVariablesV2F.values +
                bridgeVariablesI2F.values

        val shader = Shader(
            if (suffix == null) name else "$name-$suffix", attributes + vertex.uniforms, vertCode,
            varying, fragment.uniforms.sortedBy { it.name }, fragCode
        )
        shader.glslVersion = max(330, max(glslVersion, shader.glslVersion))
        val textureIndices = ArrayList<String>()
        collectTextureIndices(textureIndices, vertex.uniforms)
        collectTextureIndices(textureIndices, fragment.uniforms)
        shader.setTextureIndices(textureIndices)
        shader.ignoreNameWarnings(ignored)
        return shader
    }

    fun ignore(shader: Shader, stage: ShaderStage) {
        for (param in stage.variables.filter { !it.isAttribute }) {
            if (param.arraySize >= 0 && param.type.isSampler) {
                for (i in 0 until param.arraySize) {
                    shader.ignoreNameWarnings(param.name + i)
                }
            }
        }
        shader.ignoreNameWarnings(stage.variables.filter { !it.isAttribute }.map { it.name })
    }

    companion object {

        /***
         * a little auto-formatting
         */
        fun indent(text: String): String {
            val lines = text.split("\n")
            val result = StringBuilder(lines.size * 3)
            var depth = 0
            for (i in lines.indices) {
                if (i > 0) result.append('\n')
                val line0 = lines[i]
                val line1 = line0.trim()
                var endIndex = line1.indexOf("//")
                if (endIndex < 0) endIndex = line1.length
                val line2 = line1.substring(0, endIndex)
                val depthDelta =
                    line2.count { it == '(' || it == '{' || it == '[' } - line2.count { it == ')' || it == ']' || it == '}' }
                val firstDepth = when (line2.getOrElse(0) { '-' }) {
                    ')', '}', ']' -> true
                    else -> false
                }
                if (firstDepth) depth += depthDelta
                for (j in 0 until depth) {
                    result.append("  ")
                }
                if (!firstDepth) depth += depthDelta
                result.append(line2)
            }
            return result.toString()
        }
    }
}