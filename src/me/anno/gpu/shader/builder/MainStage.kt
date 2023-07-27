package me.anno.gpu.shader.builder

import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Strings.ifBlank2
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.collections.LinkedHashSet

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

        val definedByPrevious = previousDefined.filter { it !in defined }.toHashSet()

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
        val isCubemap = uniform.type == GLSLType.SCube || uniform.type == GLSLType.SCubeShadow
        val isShadow = uniform.type == GLSLType.S2DShadow || uniform.type == GLSLType.SCubeShadow
        val name = uniform.name

        // base color function
        if (!isShadow) {
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
        } else {
            // function with interpolation for depth,
            // as sampler2DShadow is supposed to work
            code.append("float texture_array_depth_")
            code.append(name)
            // todo base bias on the normal as suggested by https://digitalrune.github.io/DigitalRune-Documentation/html/3f4d959e-9c98-4a97-8d85-7a73c26145d7.htm ? :)
            if (isCubemap) {
                code.append("(int index, vec3 uv, float depth){\n")
                code.append("float bias = 0.005;\n")
                code.append("vec4 uvw = vec4(uv,depth+bias);\n")
                code.append("vec2 size; float du, sum=0.0; vec3 u; bool x,z; vec4 dx,dy;\n")
                if (isMoreThanOne) code.append("switch(index){\n")
                for (index in 0 until uniform.arraySize) {
                    val nameIndex = name + index.toString()
                    if (isMoreThanOne) code.append("case ").append(index).append(": ")
                    code.append(
                        "" +
                                "size = textureSize($nameIndex,0); du=0.5/size.x;\n" +
                                "u = abs(uvw.xyz);\n" +
                                "x = u.x >= u.y && u.x > u.z;\n" +
                                "z = !x && u.z >= u.y;\n" +
                                // not ideal...
                                "dx = x ? vec4(0,du,0,0) : vec4(du,0,0,0);\n" +
                                "dy = z ? vec4(0,du,0,0) : vec4(0,0,du,0);\n" +
                                "for(float j=-2.0;j<2.5;j++){\n" +
                                "   for(float i=-2.0;i<2.5;i++){\n" +
                                "       sum += texture($nameIndex, uvw+i*dx+j*dy);\n" +
                                "   }\n" +
                                "}\n" +
                                "return sum*0.04;\n"
                    )
                }
                if (isMoreThanOne) code.append("default: return 0.0;\n}\n")
                code.append("}\n")
            } else {
                code.append("(int index, vec2 uv, float depth){\n")
                code.append("float bias = 0.005;\n")
                code.append("vec3 uvw = vec3(uv*.5+.5,depth+bias);\n")
                code.append("ivec2 size;float sum,du;\n")
                if (isMoreThanOne) code.append("switch(index){\n")
                for (index in 0 until uniform.arraySize) {
                    val nameIndex = name + index.toString()
                    if (isMoreThanOne) code.append("case ").append(index).append(":\n")
                    code.append(
                        "" +
                                // 5x5 percentage closer filtering for prettier results
                                "size = textureSize($nameIndex,0);\n" +
                                "du = 1.0/float(size.x);\n" +
                                "for(int j=-2;j<=2;j++){\n" +
                                "   for(int i=-2;i<=2;i++){\n" +
                                "       sum += texture($nameIndex, uvw+du*vec3(i,j,0.0));\n" +
                                "   }\n" +
                                "}\n" +
                                "return sum*0.04;\n"
                    )
                }
                if (isMoreThanOne) code.append("default: return 0.0;\n}\n")
                code.append("}\n")
            }
        }

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
    }

    fun createCode(
        isFragmentStage: Boolean,
        outputs: DeferredSettingsV2?,
        disabledLayers: BitSet?,
        bridgeVariables1: Map<Variable, Variable>
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

        if (isFragmentStage) {
            // fragment shader
            if (outputs == null) {
                code.append("layout(location=0) out vec4 BuildColor;\n")
            } else {
                // register all layers
                outputs.appendLayerDeclarators(code, disabledLayers)
            }
            code.append('\n')
        }

        // find Uniform -> Stage -> finalXXX bridges
        val bridgeVariables2 = HashSet<Variable>()
        if (isFragmentStage) {
            for (index in stages.indices) {
                val stage = stages[index]
                vars@ for (variable in stage.variables) {
                    if ((variable.inOutMode == VariableMode.INOUT) &&
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
        // work-around: like in C, declare the function header, and then GLSL will find the dependency by itself :)
        if (outputs != null) functions2.add(ShaderLib.octNormalPacking)
        for (func in functions2) code.append(func)
        if (functions2.isNotEmpty()) code.append('\n')

        // for all uniforms, which are sampler arrays, define the appropriate access function
        for (uniform in uniforms) {
            if (uniform.arraySize >= 0 && uniform.type.glslName.startsWith("sampler")) {
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
            for ((local, varying) in bridgeVariables1) {
                local.declare0(code, null)
                code.append("=").append(varying.name).append("; // bridge1\n")
                defined += local
            }
            for (variable in bridgeVariables2) {
                variable.declare0(code, null)
                code.append("=get_").append(variable.name).append("(); // bridge2\n")
                defined += variable
            }
        } else {
            for ((local, _) in bridgeVariables1) {
                local.declare(code, null, true)
                defined += local
            }
        }

        // write all stages
        for (i in stages.indices) {
            val stage = stages[i]
            code.append("// start of stage ").append(stage.callName).append('\n')
            val params = stage.variables
            // if this function defines a variable, which has been undefined before, define it
            for (param in params.sortedBy { it.type }) {
                if (param.isOutput && param !in defined) {
                    // write default value if name matches deferred layer
                    // if the shader works properly, it is overridden anyway
                    val dlt = DeferredLayerType.byName[param.name]
                    if (dlt != null && dlt.workDims == param.type.components) {
                        param.declare0(code, null)
                        code.append('=')
                        dlt.appendDefaultValue(code)
                        code.append(";\n")
                    } else {
                        param.declare(code, null, true)
                    }
                    defined += param
                }
            }
            code.append("if(true){// stage ").append(stage.callName).append('\n')
            code.append(stage.body)
            if (!code.endsWith('\n')) code.append('\n')
            code.append("}// end of stage ").append(stage.callName).append('\n')
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
            for ((local, varying) in bridgeVariables1) {
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
                        code.append("BuildColor = ")
                            .append(lastOutputs[0].name).append(";\n")
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
                    // only needed if output is not disabled
                    if (disabledLayers == null || !disabledLayers[outputs.findLayer(type)!!.texIndex]) {
                        // write the default values, if not already defined
                        if (defined.none { type.glslName == it.name }) {
                            type.appendDefinition(code)
                            code.append(" = ")
                            type.appendDefaultValue(code)
                            code.append(";\n")
                        }
                    }
                }
                outputs.appendLayerWriters(code, disabledLayers)
            }
        }
        code.append("}\n")
        return code.toString()
    }

    val functions1 = HashSet<String>()
    val functions2 = LinkedHashSet<String>()

    private fun defineFunction(function: Function) {
        val key = function.header.ifBlank2(function.body)
        val previous = key in functions1
        if (previous) {
            if (function.body !in functions2)
                LOGGER.warn("Overriding shader function! $key")
            return
        }
        functions2.add(function.body)
    }

    companion object {
        private val LOGGER = LogManager.getLogger(MainStage::class)
    }

}
