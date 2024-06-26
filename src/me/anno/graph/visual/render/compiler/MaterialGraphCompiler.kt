package me.anno.graph.visual.render.compiler

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.StartNode
import me.anno.graph.visual.render.DiscardNode
import me.anno.graph.visual.render.MaterialGraph
import me.anno.graph.visual.render.MaterialGraph.kotlinToGLSL
import me.anno.graph.visual.render.MaterialReturnNode

class MaterialGraphCompiler(
    start: StartNode,
    g: FlowGraph,
    budget: Int, // prevent hangs by limiting the total number of iteration a shader is allowed to perform
) : GraphCompiler(g) {

    val shader: ECSMeshShader
    override val currentShader: Shader get() = shader.value

    override fun handleReturnNode(node: ReturnNode) {
        when (node) {
            is MaterialReturnNode -> {
                // define all values :)
                for (i in MaterialGraph.layers.indices) {
                    val l = MaterialGraph.layers[i]
                    val c = node.inputs[i + 1]
                    if (shallExport(l, c)) {
                        // set value :)
                        builder.append(l.glslName).append("=")
                        // clamping could be skipped, if we were sure that the value is within bounds
                        if (l.highDynamicRange) {
                            expr(c)
                        } else when (l.workToData) {
                            "*0.5+0.5" -> {
                                builder.append("clamp(")
                                expr(c)
                                builder.append(",-1.0,1.0)")
                            }
                            else -> {
                                builder.append("clamp(")
                                expr(c)
                                builder.append(",0.0,1.0)")
                            }
                        }
                        builder.append(";\n")
                    } // else skip this
                }
                // jump to return
                builder.append("return false;\n")
            }
            is DiscardNode -> {
                builder.append("return true;\n")
            }
            else -> throw NotImplementedError(node.className)
        }
    }

    init {

        /////////////////////////////////////////////////////
        // header, defining useful local variables and inout types

        builder.append("bool calc(")
        val outs = start.outputs
        var first = true
        for (i in 1 until outs.size) {
            val o = outs[i]
            if (o.others.isNotEmpty()) {
                // is used :)
                if (!first) builder.append(", ")
                val tmpName = conDefines.getOrPut(o) { prefix + conDefines.size }
                builder.append(kotlinToGLSL(o.type))
                    .append(" ").append(tmpName)
                first = false
            }
        }

        val exportedLayers = findExportSet(start, MaterialGraph.layers)
        for (i in MaterialGraph.layers.indices) {
            if (exportedLayers[i]) {
                val l = MaterialGraph.layers[i]
                if (!first) builder.append(", ")
                builder
                    .append("inout ")
                    .append(GLSLType.floats[l.workDims - 1])
                    .append(" ").append(l.glslName)
                first = false
            }
        }
        builder.append("){\n")

        defineBudget(builder, budget)

        val funcHeader = builder.toString()
        builder.clear()

        /////////////////////////////////////////////////////
        // body, defines variables

        if (buildCode(start, 1))
            builder.append("return false;\n")
        builder.append("}\n")

        val funcBody = builder.toString()
        builder.clear()

        /////////////////////////////////////////////////////
        // variables, will be inserted before body

        defineLocalVars(builder)

        val funcVars = builder.toString()
        builder.clear()

        val functions = extraFunctions.toString() + funcHeader + funcVars + funcBody

        /////////////////////////////////////////////////////
        // call from main()

        // call method with all required parameters
        val layers = MaterialGraph.layers
        val layer0Name = layers[0].glslName
        if (exportedLayers[0]) {
            val v = DeferredLayerType.COLOR.defaultWorkValue
            builder.append("vec4 ").append(layer0Name).append("=vec4(${v.x},${v.y},${v.z},1.0);\n")
        }
        builder.append("if(calc(")
        first = true
        val usedVars = ArrayList<Variable>()
        // all inputs
        for (i in 1 until outs.size) {
            if (start.getOutputNode(i) != null) {
                if (!first) builder.append(", ")
                builder.append(
                    when (i) {
                        1 -> {
                            usedVars += Variable(GLSLType.V3F, "localPosition")
                            "localPosition"
                        }
                        2 -> {
                            usedVars += Variable(GLSLType.V3F, "finalPosition", VariableMode.INOUT)
                            "finalPosition"
                        }
                        3 -> {
                            usedVars += Variable(GLSLType.V2F, "uv")
                            "uv"
                        }
                        4 -> {
                            usedVars += Variable(GLSLType.V3F, "normal")
                            "normal"
                        }
                        5 -> {
                            usedVars += Variable(GLSLType.V4F, "tangent")
                            "tangent"
                        }
                        6 -> "finalBitangent"
                        7 -> {
                            usedVars += Variable(GLSLType.V4F, "vertexColor0")
                            "vertexColor0"
                        }
                        else -> throw NotImplementedError()
                    }
                )
                first = false
            }
        }

        // all outputs
        for (i in layers.indices) {
            if (exportedLayers[i]) {
                if (!first) builder.append(", ")
                builder.append(layers[i].glslName)
                first = false
            }
        }
        builder.append(")) discard;\n")
        if (exportedLayers[0]) {
            builder.append("finalColor=").append(layer0Name).append(".rgb;\n")
            builder.append("finalAlpha=").append(layer0Name).append(".a;\n")
        }

        val funcCall = builder.toString()
        // println(functions)
        // println(funcCall)

        /////////////////////////////////////////////////////
        // adding last variables

        usedVars.ensureCapacity(usedVars.size + typeValues.size + extraVariables.size)
        usedVars.addAll(extraVariables)
        for ((k, v) in typeValues) {
            usedVars += Variable(v.type, k)
        }

        /////////////////////////////////////////////////////
        // finishing shader

        shader = object : ECSMeshShader(g.name) {

            override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
                super.bind(shader, renderer, instanced)
                for ((k, v) in typeValues) {
                    v.bind(shader, k)
                }
            }

            override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
                val vars = usedVars + super.createFragmentVariables(key)
                return listOf(
                    ShaderStage(
                        "calc", vars,
                        discardByCullingPlane +
                                normalTanBitanCalculation +
                                funcCall +
                                // todo node for clear coat calculation
                                // todo optional sheen calculation
                                reflectionCalculation +
                                finalMotionCalculation
                    ).add(functions).add(RendererLib.getReflectivity)
                )
            }
        }
    }
}