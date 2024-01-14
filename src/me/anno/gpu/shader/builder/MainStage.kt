package me.anno.gpu.shader.builder

import me.anno.gpu.DitherMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderFuncLib.randomGLSL
import me.anno.gpu.shader.ShaderLib
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.none2
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.ifBlank2
import org.apache.logging.log4j.LogManager
import java.util.*

class MainStage {

    // defined via in vec<s> name
    val attributes = ArrayList<Variable>()

    // color & position computations
    val stages = ArrayList<ShaderStage>()

    // define existing constants
    val defined = HashSet<Variable>()

    fun add(stage: ShaderStage) {
        stages.add(stage)
        for (attr in stage.attributes) {
            if (attributes.none2 { it.name == attr.name }) {
                attributes.add(attr)
            }
        }
        defineFunction(Function(randomGLSL))
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
    ): Set<Variable> {

        val defined = HashSet(defined)
        defined.addAll(attributes)

        val definedByPrevious = previousDefined
            .filter { it !in defined }
            .toHashSet()

        uniforms.clear()
        for (stage in stages) {
            for (variable in stage.variables) {
                if (variable.isInput && !variable.isAttribute) {
                    if (variable !in defined) {
                        if (variable in definedByPrevious &&
                            !variable.type.isSampler &&
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

        return defined
    }

    fun defineUniformSamplerArrayFunctions(code: StringBuilder, uniform: Variable) {

        val isMoreThanOne = uniform.arraySize > 1 // if there is only one value, we can optimize it
        val isCubemap = when (uniform.type) {
            GLSLType.SCube, GLSLType.SCubeShadow -> true
            else -> false
        }
        val isShadow = when (uniform.type) {
            GLSLType.S2DShadow,
            GLSLType.SCubeShadow,
            GLSLType.S2DAShadow -> true
            else -> false
        }
        val isArray = when (uniform.type) {
            GLSLType.S2DA, GLSLType.S2DAShadow -> true
            else -> false
        }
        val name = uniform.name

        // base color function
        if (!isShadow) {
            code.append("vec4 texture_array_")
            code.append(name)
            val dim = 2 + isCubemap.toInt() + isArray.toInt()
            code.append(
                when (dim) {
                    2 -> "(int index, vec2 uv){\n"
                    3 -> "(int index, vec3 uv){\n"
                    4 -> "(int index, vec4 uv){\n"
                    else -> throw NotImplementedError()
                }
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
                if (isArray) throw NotImplementedError()
                code.append("(int index, vec3 uv, float depth){\n")
                code.append("float bias = 0.05 * depth;\n")
                code.append("vec4 uvw = vec4(uv,depth+bias);\n")
                code.append("ivec2 size; float du, sum=0.0; vec3 u; bool x,z; vec4 dx,dy;\n")
                if (isMoreThanOne) code.append("switch(index){\n")
                for (index in 0 until uniform.arraySize) {
                    val nameIndex = name + index.toString()
                    if (isMoreThanOne) code.append("case ").append(index).append(": ")
                    code.append(
                        "" +
                                "du=0.5/float(textureSize($nameIndex,0).x);\n" +
                                "u = abs(uvw.xyz);\n" +
                                "x = u.x >= u.y && u.x > u.z;\n" +
                                "z = !x && u.z >= u.y;\n" +
                                // not ideal...
                                "dx = x ? vec4(0,du,0,0) : vec4(du,0,0,0);\n" +
                                "dy = z ? vec4(0,du,0,0) : vec4(0,0,du,0);\n" +
                                "for(float j=-2.0;j<2.5;j++){\n" +
                                "   for(float i=-2.0;i<2.5;i++){\n"
                    )
                    if (GFX.supportsDepthTextures) {
                        code.append("sum += texture($nameIndex, uvw+i*dx+j*dy);\n")
                    } else {
                        // todo use smoothstep with bias?
                        code.append("sum += step(texture($nameIndex, uvw.xyz+i*dx.xyz+j*dy.xyz).x,uvw.w);\n")
                    }
                    code.append("}}\n return sum*0.04;\n")
                }
                if (isMoreThanOne) code.append("default: return 0.0;\n}\n")
                code.append("}\n")
            } else {
                if (isArray) {
                    code.append("(int index, vec3 uv, float depth){\n")
                    code.append("float bias = 0.005;\n")
                    code.append("vec4 uvw = vec4(uv*.5+.5,depth+bias);\n")
                } else {
                    code.append("(int index, vec2 uv, float depth){\n")
                    code.append("float bias = 0.005;\n")
                    code.append("vec3 uvw = vec3(uv*.5+.5,depth+bias);\n")
                }
                code.append("float sum,du;\n")
                if (isMoreThanOne) code.append("switch(index){\n")
                for (index in 0 until uniform.arraySize) {
                    val nameIndex = name + index.toString()
                    if (isMoreThanOne) code.append("case ").append(index).append(":\n")
                    // 5x5 percentage closer filtering for prettier results
                    code.append(
                        "" +
                                "du = 1.0/float(textureSize($nameIndex,0).x);\n" +
                                "for(int j=-2;j<=2;j++){\n" +
                                "   for(int i=-2;i<=2;i++){\n"
                    )
                    if (GFX.supportsDepthTextures) {
                        code.append("sum += texture(").append(nameIndex).append(", uvw+du*")
                        code.append(if (isArray) "vec4(i,j,0.0,0.0));\n" else "vec3(i,j,0.0));\n")
                    } else {
                        // todo use smoothstep with bias?
                        code.append("sum += step(texture(").append(nameIndex).append(", ")
                        code.append(
                            if (isArray) "uvw.xyz+du*vec3(i,j,0.0)).x,uvw.w);\n"
                            else "uvw.xy+du*vec2(i,j)).x,uvw.z);\n"
                        )
                    }
                    code.append("}}\n return sum*0.04;\n")
                }
                if (isMoreThanOne) code.append("default: return 0.0;\n}\n")
                code.append("}\n")
            }
        }

        // texture size function
        code.append(if (isArray) "ivec3 " else "ivec2 ")
        code.append("texture_array_size_")
        if (isArray) code.append("2d_")
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
            code.append("default: return ")
                .append(if (isArray) "ivec3(1,1,1)" else "ivec2(1,1)")
                .append(";\n}\n")
        }
        code.append("}\n")

    }

    fun createCode(
        isFragmentStage: Boolean,
        settings: DeferredSettings?,
        disabledLayers: BitSet?,
        ditherMode: DitherMode,
        bridgeVariablesV2F: Map<Variable, Variable>,
        bridgeVariablesI2F: Map<Variable, Variable>
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
            if (settings == null) {
                uniforms.add(Variable(GLSLType.V4F, "BuildColor", VariableMode.OUT))
            } else {
                // register all layers
                settings.appendLayerDeclarators(disabledLayers, uniforms)
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
        if (isFragmentStage) {
            if (settings != null) {
                functions2.add(ShaderLib.octNormalPacking)
            }
            if (ditherMode == DitherMode.DITHER2X2) {
                functions2.add(ShaderLib.dither2x2)
            }
        }

        for (func in functions2) code.append(func)
        if (functions2.isNotEmpty()) code.append('\n')

        // for all uniforms, which are sampler arrays, define the appropriate access function
        for (uniform in uniforms) {
            if (uniform.arraySize >= 0 && uniform.type.isSampler) {
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
            for ((local, varying) in bridgeVariablesV2F) {
                local.declare0(code, null)
                code.append("=").append(varying.name).append("; // bridge-step#1\n")
                defined += local
            }
            for ((local, varying) in bridgeVariablesI2F) {
                local.declare0(code, null)
                code.append("=").append(varying.name).append("; // bridge-step#2\n")
                defined += local
            }
            for (variable in bridgeVariables2) {
                variable.declare0(code, null)
                code.append("=get_").append(variable.name).append("(); // bridge-step#3\n")
                defined += variable
            }
        } else {
            for ((local, _) in bridgeVariablesV2F) {
                local.declare(code, null, true)
                defined += local
            }
        }

        // write all stages
        for (i in stages.indices) {
            val stage = stages[i]
            val params = stage.variables
            var first = true
            // if this function defines a variable, which has been undefined before, define it
            for (param in params.sortedBy { it.type }) {
                if (param.isModified && param !in defined) {

                    if (first) {
                        first = false
                        code.append("// --- ").append(stage.callName).append(" ---\n")
                    }

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
            if (first) code.append("{ // --- ").append(stage.callName).append(" ---\n")
            else code.append("{\n")
            code.append(stage.body)
            if (!code.endsWith('\n')) code.append('\n')
            code.append("}\n")
        }

        if (!isFragmentStage) {
            for ((local, varying) in bridgeVariablesV2F) {
                code.append(varying.name).append('=').append(local.name).append("; // bridge-step#7\n")
            }
            for ((local, varying) in bridgeVariablesI2F) {
                code.append(varying.name).append('=').append(local.name).append("; // bridge-step#8\n")
            }
        }

        if (isFragmentStage && Variable(GLSLType.V1F, "finalAlpha") in defined) {
            code.append("#ifndef CUSTOM_DITHER\n")
            when (ditherMode) {
                DitherMode.ALPHA_THRESHOLD_INV255 -> {
                    code.append("if(finalAlpha<${1f / 255f}) { discard; }\n")
                }
                DitherMode.ALPHA_THRESHOLD_HALF -> {
                    code.append("if(finalAlpha<0.5) { discard; }\n")
                }
                DitherMode.ALPHA_THRESHOLD_ONE -> {
                    code.append("if(finalAlpha<1.0) { discard; }\n")
                }
                DitherMode.DITHER2X2 -> {
                    code.append("if(dither2x2(finalAlpha)) { discard; }\n")
                }
                DitherMode.DRAW_EVERYTHING -> {} // done
                else -> throw NotImplementedError()
            }
            code.append("#endif\n")
        } else if(isFragmentStage){
            println("skipping $ditherMode, because !finalAlpha")
        }

        // write to the outputs for fragment shader
        if (isFragmentStage) {
            if (settings == null) {
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
                val layerTypes = settings.layerTypes
                for (type in layerTypes) {
                    // only needed if output is not disabled
                    if (disabledLayers == null || !disabledLayers[settings.findLayer(type)!!.texIndex]) {
                        // write the default values, if not already defined
                        val idx = type.workToData.indexOf('.')
                        if ('.' in type.workToData) {
                            val glslName = type.workToData.substring(0, idx)
                            if (defined.none { glslName == it.name }) {
                                code.append("vec4 ").append(glslName).append(" = ")
                                type.appendDefaultValue(code, 4)
                                code.append(";\n")
                            }
                        } else {
                            if (defined.none { type.glslName == it.name }) {
                                type.appendDefinition(code)
                                code.append(" = ")
                                type.appendDefaultValue(code)
                                code.append(";\n")
                            }
                        }
                    }
                }
                settings.appendLayerWriters(code, disabledLayers)
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
