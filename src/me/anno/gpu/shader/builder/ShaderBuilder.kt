package me.anno.gpu.shader.builder

import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.GeoShader
import me.anno.gpu.shader.OpenGLShader
import me.anno.gpu.shader.Shader
import me.anno.utils.LOGGER
import kotlin.math.max

class ShaderBuilder(val name: String) {

    constructor(name: String, settingsV2: DeferredSettingsV2?) : this(name) {
        outputs = settingsV2
    }

    // there exist 3 passes: vertex, fragment, geometry
    // input variables can be defined on multiple ways: uniforms / attributes
    // output colors can be further and further processed

    val vertex = MainStage().apply {
        define(Variable(GLSLType.V4F, "gl_Position", true))
    }

    val fragment = MainStage()

    val geometry: GeoShader? = null

    var outputs: DeferredSettingsV2? = null

    var glslVersion = OpenGLShader.DefaultGLSLVersion

    val ignored = HashSet<String>()

    fun addVertex(stage: ShaderStage?) {
        vertex.add(stage ?: return)
    }

    fun addFragment(stage: ShaderStage?) {
        fragment.add(stage ?: return)
    }

    private fun collectTextureIndices(textureIndices: MutableList<String>, uniforms: Collection<Variable>) {
        for (uniform in uniforms) {
            when (uniform.type) {
                GLSLType.S2D, GLSLType.S3D, GLSLType.SCube -> {
                    if (uniform.arraySize > 0) {
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
                else -> {}
            }
        }
    }

    fun create(): Shader {

        // combine the code
        // find imports
        val (vertexDefined, vertexUniforms) = vertex.findImportsAndDefineValues(null, emptySet(), emptySet())
        fragment.findImportsAndDefineValues(vertex, vertexDefined, vertexUniforms)

        // LOGGER.info("Vertex-Defined: $vertexDefined, Vertex-Uniforms: $vertexUniforms")

        val bridgeVariables =
            HashMap<Variable, Variable>() // variables, that fragment imports & exports & vertex exports
        for (variable in vertexDefined) {
            val name = variable.name
            if (vertex.stages.any { it.variables.any { v -> v.name == name && v.isOutput } }) {
                for (stage in fragment.stages) {
                    if (stage.variables.any { it.isInput && name == it.name }) {
                        // the stage uses it -> might be relevant
                        if (stage.variables.any { it.isOutput && name == it.name }) {
                            // the stage also exports it ->
                            // LOGGER.info("Bridge is being created for $variable")
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
        val vertCode = vertex.createCode(false, outputs, bridgeVariables)
        val fragCode = fragment.createCode(true, outputs, bridgeVariables)
        val varying = (vertex.imported + vertex.exported).toList()
            .filter { it !in bridgeVariables } + bridgeVariables.values
        val shader = Shader(name, geometry?.code, vertCode, varying, fragCode)
        shader.glslVersion = max(330, max(glslVersion, shader.glslVersion))
        val textureIndices = ArrayList<String>()
        collectTextureIndices(textureIndices, vertex.uniforms)
        collectTextureIndices(textureIndices, fragment.uniforms)
        // LOGGER.info("Textures($name): $textureIndices")
        shader.setTextureIndices(textureIndices)
        shader.ignoreUniformWarnings(ignored)
        /*for (stage in vertex.stages) ignore(shader, stage)
        for (stage in fragment.stages) ignore(shader, stage)*/
        return shader
    }

    fun ignore(shader: Shader, stage: ShaderStage) {
        for (param in stage.variables.filter { !it.isAttribute }) {
            if (param.arraySize > 0 && param.type.glslName.startsWith("sampler")) {
                for (i in 0 until param.arraySize) {
                    shader.ignoreUniformWarning(param.name + i)
                }
            }
        }
        shader.ignoreUniformWarnings(stage.variables.filter { !it.isAttribute }.map { it.name })
    }

    companion object {

        // a little auto-formatting
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