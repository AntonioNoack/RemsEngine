package me.anno.gpu.shader.builder

import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.OpenGLShader
import me.anno.utils.structures.lists.Lists.any2
import org.apache.logging.log4j.LogManager

class MainStage {

    // defined via in vec<s> name
    val attributes = ArrayList<Variable>()

    // color & position computations
    val stages = ArrayList<ShaderStage>()

    // define existing constants
    val defined = HashSet<Variable>()

    fun add(stage: ShaderStage) {
        stages.add(stage)
        attributes.addAll(stage.attributes)
        for (function in stage.functions) {
            defineFunction(function)
        }
    }

    val imported = HashSet<Variable>()
    val exported = HashSet<Variable>()

    val uniforms = HashSet<Variable>()

    fun define(variable: Variable) {
        defined.add(variable)
    }

    fun findImportsAndDefineValues(
        previous: MainStage?,
        previousDefined: Set<Variable>,
        previousUniforms: Set<Variable>
    ): Pair<Set<Variable>, Set<Variable>> {

        val defined = HashSet(defined)
        defined.addAll(attributes)

        val definedByPrevious = HashSet<Variable>(
            previousDefined.filter { it !in defined }
        )

        uniforms.clear()
        for (stage in stages) {
            for (variable in stage.variables) {
                if (variable.isInput && !variable.isAttribute) {
                    if (variable !in defined) {
                        if (variable in definedByPrevious &&
                            !variable.type.glslName.startsWith("sampler") &&
                            variable !in previousUniforms
                        ) {
                            // we need this variable
                            imported.add(variable)
                            previous?.exported?.add(variable)
                        } else {
                            uniforms.add(variable)
                            defined += variable
                        }
                    }
                }
            }
            for (variable in stage.variables) {
                if (variable.isOutput) {
                    defined += variable
                    // now it's locally overridden
                    definedByPrevious.remove(variable)
                }
            }
        }

        return Pair(defined, uniforms)

    }

    fun defineUniformSamplerArrayFunctions(code: StringBuilder, uniform: Variable) {

        val isMoreThanOne = uniform.arraySize > 1 // if there is only one value, we can optimize it
        val isCubemap = uniform.type == GLSLType.SCube
        val name = uniform.name

        // base color function
        code.append("vec4 texture_array_")
        code.append(name)
        code.append(
            if (isCubemap) "(int index, vec3 uv){\n"
            else "(int index, vec2 uv){\n"
        )
        if (isMoreThanOne) code.append("switch(index){\n")
        for (index in 0 until uniform.arraySize) {
            if (isMoreThanOne) {
                code.append("case ")
                code.append(index)
                code.append(": ")
            }
            code.append("return texture(")
            code.append(name)
            code.append(index)
            code.append(", uv);\n")
        }
        if (isMoreThanOne) {
            code.append("default: return vec4(0.0);\n")
            code.append("}\n")
        }
        code.append("}\n")

        // texture size function
        code.append("ivec2 texture_array_size_")
        code.append(name)
        code.append("(int index, int lod){\n")
        if (isMoreThanOne) code.append("switch(index){\n")
        for (index in 0 until uniform.arraySize) {
            if (isMoreThanOne) {
                code.append("case ")
                code.append(index)
                code.append(": ")
            }
            code.append("return textureSize(")
            code.append(name)
            code.append(index)
            code.append(", lod);\n")
        }
        if (isMoreThanOne) {
            code.append("default: return ivec2(1);\n")
            code.append("}\n")
        }
        code.append("}\n")

        // function with interpolation for depth,
        // as sampler2DShadow is supposed to work
        code.append("float texture_array_depth_")
        code.append(name)
        if (isCubemap) {
            code.append("(int index, vec3 uv, float depth){\n")
            code.append("float d0;\n")
            if (isMoreThanOne) code.append("switch(index){\n")
            for (index in 0 until uniform.arraySize) {
                val nameIndex = name + index.toString()
                if (isMoreThanOne) {
                    code.append("case ")
                    code.append(index)
                    code.append(": ")
                }
                // interpolation? we would need to know the side, and switch case on that
                if (isMoreThanOne) {
                    code.append("d0=texture(").append(nameIndex).append(", uv).r; break;\n")
                } else {
                    code.append("d0=texture(").append(nameIndex).append(", uv).r;\n")
                }
            }
            if (isMoreThanOne) {
                code.append("default: return 0.0;\n")
                code.append("}\n")
            }
            code.append(
                "" +
                        // depth bias
                        // dynamic bias is hard...
                        "depth += 0.005;\n" +
                        "return float(d0>depth);\n"
            )
            code.append("}\n")
        } else {
            // todo implement percentage closer filtering for prettier results
            code.append("(int index, vec2 uv, float depth){\n")
            code.append("int size;vec2 f;float d,d0,d1,d2,d3,fSize;uv=uv*.5+.5;\n")
            if (isMoreThanOne) code.append("switch(index){\n")
            for (index in 0 until uniform.arraySize) {
                val nameIndex = name + index.toString()
                if (isMoreThanOne) {
                    code.append("case ")
                    code.append(index)
                    code.append(":\n")
                }
                code.append(
                    "" +
                            "size = textureSize($nameIndex,0).x;\n" +
                            "fSize = float(size);\n" +
                            "d = 1.0/fSize;\n" +
                            "f = fract(uv*fSize);\n" +
                            "d0=texture($nameIndex, uv          ).r;\n" +
                            "d1=texture($nameIndex, uv+vec2(0,d)).r;\n" +
                            "d2=texture($nameIndex, uv+vec2(d,0)).r;\n" +
                            "d3=texture($nameIndex, uv+vec2(d,d)).r;\n"

                )
                if (isMoreThanOne) {
                    code.append("break;\n")
                }
            }
            if (isMoreThanOne) {
                code.append("default: return 0.0;\n")
                code.append("}\n")
            }
            code.append(
                "" +
                        // depth bias
                        // dynamic bias is hard...
                        "depth += 0.005;\n" +
                        "return mix(mix(" +
                        "   float(d0>depth),\n" +
                        "   float(d1>depth),\n" +
                        "f.y), mix(\n" +
                        "   float(d2>depth),\n" +
                        "   float(d3>depth),\n" +
                        "f.y), f.x);\n"
            )
            code.append("}\n")
        }
    }

    fun createCode(
        isFragmentStage: Boolean,
        outputs: DeferredSettingsV2?,
        bridgeVariables: Map<Variable, Variable>
    ): String {

        // set what is all defined
        defined += imported

        val code = StringBuilder()

        // defines
        for (stage in stages) {
            for (define in stage.defines) {
                code.append("#define ")
                code.append(define)
                code.append('\n')
            }
        }

        // attributes
        if (!isFragmentStage) {
            for (variable in attributes.sortedBy { it.size }) {
                variable.declare(code, OpenGLShader.attribute)
            }
            if (attributes.isNotEmpty()) code.append('\n')
        }

        if (isFragmentStage) {
            // fragment shader
            if (outputs == null) {
                code.append("layout(location=0) out vec4 BuildColor;\n")
            } else {
                // register all layers
                outputs.appendLayerDeclarators(code)
            }
            code.append('\n')
        }

        // define the missing variables
        // sorted by size, so small uniforms get a small location,
        // which in return allows them to be cached
        for (variable in uniforms.sortedBy { it.size }) {
            variable.declare(code, "uniform")
        }
        if (uniforms.isNotEmpty()) code.append('\n')

        // find Uniform -> Stage -> finalXXX bridges
        val bridgeVariables2 = HashSet<Variable>()
        if (isFragmentStage) {
            for (index in stages.indices) {
                val stage = stages[index]
                vars@ for (variable in stage.variables) {
                    if (variable.inOutMode == VariableMode.INOUT &&
                        uniforms.any { it.name == variable.name }
                    ) {
                        for (i in 0 until index) {
                            val previousStage = stages[i]
                            if (previousStage.variables.any2 {
                                    it.isOutput && it.name == variable.name
                                }) {// we found the producer of our variable :)
                                continue@vars
                            }
                        }
                        bridgeVariables2.add(variable)
                        // define helper function
                        code.append(variable.type.glslName)
                            .append(" get_").append(variable.name)
                            .append("(){ return ").append(variable.name)
                            .append("; }\n")
                    }
                }
            }
        }

        // define all required functions
        for ((_, func) in functions) code.append(func)
        if (functions.isNotEmpty()) code.append('\n')

        // for all uniforms, which are sampler arrays, define the appropriate access function
        for (uniform in uniforms) {
            if (uniform.arraySize > 0 && uniform.type.glslName.startsWith("sampler")) {
                defineUniformSamplerArrayFunctions(code, uniform)
            }
        }

        code.append("void main(){\n")

        val defined = HashSet(defined)
        defined += uniforms
        defined += imported
        defined += exported

        // assign bridge variables/varyings
        if (isFragmentStage) {
            for ((local, varying) in bridgeVariables) {
                local.declare(code, null)
                code.append(local.name).append('=').append(varying.name).append(";\n")
            }
            for (variable in bridgeVariables2) {
                variable.declare(code, null)
                code.append(variable.name).append("=get_")
                    .append(variable.name).append("();\n")
            }
        } else {
            for ((local, _) in bridgeVariables) {
                local.declare(code, null)
            }
        }

        // write all stages
        for (i in stages.indices) {
            val stage = stages[i]
            code.append("// start of stage ").append(stage.callName).append('\n')
            val params = stage.variables
            // if this function defines a variable, which has been undefined before, define it
            for (param in params) {
                if (param.isOutput && param !in defined) {
                    param.declare(code, null)
                    // write default value if name matches deferred layer
                    // if the shader works properly, it is overridden anyway
                    val dlt = DeferredLayerType.byName[param.name]
                    if (dlt != null && dlt.dimensions == param.type.components) {
                        code.append(param.name).append("=")
                        dlt.appendDefaultValue(code)
                        code.append(";\n")
                    }
                    defined += param
                }
            }
            code.append("// stage\n")
            code.append(stage.body)
            if (!code.endsWith('\n')) code.append('\n')
            code.append("// end of stage\n")
            // expensive
            // inlining is better
            // maybe only because we had this large matrix as a param, but still, it was massively slower (30fps -> 7fps)
            /*code.append("  ")
            code.append(stage.callName)
            code.append("(")
            for (j in params.indices) {
                if (j > 0) code.append(',')
                code.append(params[j].name)
            }
            code.append(");\n")*/
        }

        if (!isFragmentStage) {
            for ((local, varying) in bridgeVariables) {
                code.append(varying.name).append('=').append(local.name).append(";\n")
            }
        }

        // write to the outputs for fragment shader
        if (isFragmentStage) {
            if (outputs == null) {
                // use last layer, if defined
                val lastLayer = stages.lastOrNull()
                val lastOutputs = lastLayer?.variables?.filter { it.isOutput } ?: emptyList()
                val outputSum = lastOutputs.sumOf { it.type.components }
                when {
                    outputSum == 0 -> {
                        code.append("BuildColor = vec4(1.0);\n")
                    }
                    outputSum == 4 && lastOutputs.size == 1 -> {
                        code.append("BuildColor = ${lastOutputs[0].name};\n")
                    }
                    outputSum in 1..4 -> {
                        code.append("BuildColor = vec4(")
                        for (i in lastOutputs.indices) {
                            if (i > 0) code.append(',')
                            code.append(lastOutputs[i].name)
                        }
                        for (i in outputSum until 4) {
                            code.append(",1")
                        }
                        code.append(");\n")
                    }
                    else -> {
                        code.append("BuildColor = vec4(finalColor, finalAlpha);\n")
                    }
                }
            } else {

                val layerTypes = outputs.layerTypes
                for (type in layerTypes) {
                    // write the default values, if not already defined
                    if (type.glslName !in defined.map { it.name }) {
                        type.appendDefinition(code)
                        code.append(" = ")
                        type.appendDefaultValue(code)
                        code.append(";\n")
                    }
                }

                outputs.appendLayerWriters(code)

            }
        }
        code.append("}\n")
        return code.toString()
    }

    val functions = HashMap<String, String>()

    private fun defineFunction(function: Function) {
        val key = function.header.ifBlank { function.body }
        val value = function.body
        val previous = functions[key]
        if (previous != null && previous != value) {
            LOGGER.warn("Overriding shader function! key $key")
        }
        functions[key] = function.body
    }

    companion object {
        private val LOGGER = LogManager.getLogger(MainStage::class)
    }

}
