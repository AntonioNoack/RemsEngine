package me.anno.gpu.shader.builder

import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.OpenGLShader
import me.anno.gpu.shader.Shader
import java.util.*
import kotlin.math.max

class ShaderBuilder(val name: String) {

    constructor(name: String, settingsV2: DeferredSettings?) : this(name) {
        outputs = settingsV2
    }

    // there exist 3 passes: vertex, fragment, geometry
    // input variables can be defined on multiple ways: uniforms / attributes
    // output colors can be further and further processed

    val vertex = MainStage().apply {
        define(Variable(GLSLType.V4F, "gl_Position", true))
    }

    val fragment = MainStage()

    var outputs: DeferredSettings? = null
    var disabledLayers: BitSet? = null

    var glslVersion = OpenGLShader.DefaultGLSLVersion

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

    private fun collectTextureIndices(textureIndices: MutableList<String>, uniforms: Collection<Variable>) {
        for (uniform in uniforms) {
            if (uniform.type.glslName.startsWith("sampler")) {
                if (uniform.arraySize >= 0) {
                    if ("${uniform.name}0" !in textureIndices) {
                        for (i in 0 until uniform.arraySize) {
                            // todo with brackets or without?
                            textureIndices.add(uniform.name + i)
                        }
                    }
                } else if (uniform.name !in textureIndices) {
                    textureIndices.add(uniform.name)
                }
            }
        }
    }

    fun create(suffix: String? = null): Shader {

        // combine the code
        // find imports
        val (vertexDefined, vertexUniforms) = vertex.findImportsAndDefineValues(null, emptySet(), emptySet())
        fragment.findImportsAndDefineValues(vertex, vertexDefined, vertexUniforms)

        // variables, that fragment imports & exports & vertex exports
        val bridgeVariables = HashMap<Variable, Variable>()
        for (variable in vertexDefined) {
            val name = variable.name
            if (vertex.stages.any { it.variables.any { v -> v.name == name && v.isOutput } }) {
                for (stage in fragment.stages) {
                    if (stage.variables.any { it.isInput && name == it.name }) {
                        // the stage uses it -> might be relevant
                        if (stage.variables.any { it.isModified && name == it.name }) {
                            // the stage also exports it ->
                            bridgeVariables[variable] =
                                Variable(variable.type, "bridge_${bridgeVariables.size}", variable.arraySize)
                        }
                    }/* else if (stage.variables.any { it.isOutput && name == it.name }) {
                        // this stage outputs it, but does not import it -> theoretically,
                        // we can skip the bridge here completely;
                        // in practice, idk whether variable shadowing is allowed like that
                    }*/
                }
            }
        }

        // create the code
        val vertCode = vertex.createCode(false, outputs, disabledLayers, bridgeVariables)
        val attributes = vertex.attributes
        val fragCode = fragment.createCode(true, outputs, disabledLayers, bridgeVariables)
        val varying = (vertex.imported + vertex.exported).toList()
            .filter { it !in bridgeVariables } + bridgeVariables.values
        val shader = object : Shader(
            if (suffix == null) name else "$name-$suffix", attributes + vertex.uniforms, vertCode,
            varying, fragment.uniforms.toList(), fragCode
        ) {
            override fun compile() {
                super.compile()
                use()
                v4f("tint", -1)
            }
        }
        shader.glslVersion = max(330, max(glslVersion, shader.glslVersion))
        val textureIndices = ArrayList<String>()
        collectTextureIndices(textureIndices, vertex.uniforms)
        collectTextureIndices(textureIndices, fragment.uniforms)
        // LOGGER.info("Textures($name): $textureIndices")
        shader.setTextureIndices(textureIndices)
        shader.ignoreNameWarnings(ignored)
        /*for (stage in vertex.stages) ignore(shader, stage)
        for (stage in fragment.stages) ignore(shader, stage)*/
        return shader
    }

    fun ignore(shader: Shader, stage: ShaderStage) {
        for (param in stage.variables.filter { !it.isAttribute }) {
            if (param.arraySize >= 0 && param.type.glslName.startsWith("sampler")) {
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