package me.anno.graph.render

import me.anno.Engine
import me.anno.cache.instances.VideoCache
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.TypeValueV2
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.graph.Node
import me.anno.graph.NodeInput
import me.anno.graph.NodeOutput
import me.anno.graph.render.MaterialGraph.convert
import me.anno.graph.render.MaterialGraph.kotlinToGLSL
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.StartNode
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.graph.types.flow.actions.PrintNode
import me.anno.graph.types.flow.control.DoWhileNode
import me.anno.graph.types.flow.control.ForNode
import me.anno.graph.types.flow.control.IfElseNode
import me.anno.graph.types.flow.control.WhileNode
import me.anno.graph.types.flow.local.GetLocalVariableNode
import me.anno.graph.types.flow.local.SetLocalVariableNode
import me.anno.graph.types.flow.maths.*
import me.anno.graph.types.flow.vector.*
import me.anno.image.ImageGPUCache
import me.anno.io.files.FileReference
import me.anno.ui.editor.files.FileExplorerEntry
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToLong
import me.anno.video.ffmpeg.FFMPEGMetadata.Companion.getMeta
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*
import kotlin.math.max

class MaterialGraphCompiler(
    start: StartNode,
    val g: FlowGraph,
    budget: Int // prevent hangs by limiting the total number of iteration a shader is allowed to perform
) {

    // todo uniform input block? maybe :)

    val builder = StringBuilder()
    val processedNodes = HashSet<Node>(g.nodes.size)

    val prefix = "tmp_"
    val conDefines = HashMap<NodeOutput, String>()

    lateinit var shader: ECSMeshShader

    var k = 0

    val localVars = HashMap<String, Pair<String, String>>() // name -> newName,type
    fun getLocalVarName(name: String, type: String?): String {
        return localVars.getOrPut("$name/$type") {
            Pair("l${localVars.size}", type!!)
        }.first
    }

    fun shallExport(l: DeferredLayerType, c: NodeInput): Boolean {
        return l == DeferredLayerType.COLOR || c.others.isNotEmpty() || c.value != c.defaultValue
    }

    val funcBuilder = StringBuilder()
    val typeToFunc = HashMap<String, String>() // key -> name
    val textureVars = HashMap<TextureNode, String>() // node -> name
    val movies = HashMap<MovieNode, Pair<String, Boolean>>() // file -> name, linear
    val textures = HashMap<FileReference, Pair<String, Boolean>>() // file -> name, linear
    fun defineFunc(name: String, prefix: String, suffix: String): String {
        funcBuilder.append(prefix).append(' ').append(name).append(suffix).append('\n')
        return name
    }

    fun expr(out: NodeOutput, n: Node): String {
        val v = conDefines[out]
        if (v != null) return v
        return when (n) {
            is GLSLExprNode -> {
                val c = n.outputs!!.indexOf(out)
                val name = n.getShaderFuncName(c)
                typeToFunc.getOrPut(name) { defineFunc(name, kotlinToGLSL(out.type), n.defineShaderFunc(c)) }
                val inputs = n.inputs
                when (inputs?.size ?: 0) {
                    0 -> "$name()"
                    else -> "$name(${inputs!!.joinToString(","){ expr(it) }})"
                }
            }
            is DotProductF2, is DotProductF3, is DotProductF4 -> {
                val inputs = n.inputs!!
                val a = expr(inputs[0])
                val b = expr(inputs[1])
                "dot($a,$b)"
            }
            is CrossProductF3 -> {
                val inputs = n.inputs!!
                val a = expr(inputs[0])
                val b = expr(inputs[1])
                "cross($a,$b)"
            }
            is CombineVector2f -> {
                val inputs = n.inputs!!
                val a = expr(inputs[0])
                val b = expr(inputs[1])
                "vec2($a,$b)"
            }
            is CombineVector3f -> {
                val inputs = n.inputs!!
                val a = expr(inputs[0])
                val b = expr(inputs[1])
                val c = expr(inputs[2])
                "vec3($a,$b,$c)"
            }
            is CombineVector4f -> {
                val inputs = n.inputs!!
                val a = expr(inputs[0])
                val b = expr(inputs[1])
                val c = expr(inputs[2])
                val d = expr(inputs[3])
                "vec4($a,$b,$c,$d)"
            }
            is SeparateVector2f, is SeparateVector3f, is SeparateVector4f -> {
                val c = n.outputs!!.indexOf(out)
                val a = expr(n.inputs!![0])
                "($a).${"xyzw"[c]}"
            }
            is GetLocalVariableNode -> getLocalVarName(n.getKey(g), n.type)
            is SetLocalVariableNode -> getLocalVarName(n.getKey(g), n.type)
            is CompareNode -> {
                val inputs = n.inputs!!
                val an = inputs[0]
                val bn = inputs[1]
                val a = expr(an)
                val b = convert(bn.type, an.type, expr(bn))!!
                val symbol = n.compType.glslName
                "($a)$symbol($b)"
            }
            is ValueNode -> expr(n.inputs!![0])
            is TextureNode -> {
                val uv = expr(n.inputs!![0])
                val texName = textures.getOrPut(n.file) {
                    val linear = constEval(n.inputs!![1]) == true
                    Pair("texI${textures.size}", linear)
                }.first
                "texture($texName,$uv)"
            }
            is MovieNode -> {
                val uv = expr(n.inputs!![0])
                val texName = movies.getOrPut(n) {
                    val linear = constEval(n.inputs!![1]) == true
                    Pair("movI${movies.size}", linear)
                }.first
                "texture($texName,$uv)"
            }
            is ColorNode -> {
                val c = n.value
                "vec4(${c.x},${c.y},${c.z},${c.w})"
            }
            is GameTime -> {
                val key = "uGameTime"
                typeValues.getOrPut(key) {
                    TypeValueV2(GLSLType.V1F) { Engine.gameTimeF }
                }
                key
            }
            else -> throw IllegalArgumentException("Unknown node ${n.javaClass.name}")
        }
    }

    fun expr(c: NodeInput): String {
        val n0 = c.others.firstOrNull()
        if (n0 is NodeOutput) {
            return convert(n0.type, c.type, expr(n0, n0.node!!))
                ?: throw IllegalStateException("Cannot convert ${n0.type} to ${c.type}!")
        }
        val v = c.value
        return (when (c.type) {
            "Float", "Double" -> AnyToFloat.getFloat(v, 0, c.defaultValue as? Float ?: 0f)
            "Int", "Long" -> AnyToLong.getLong(v, 0, c.defaultValue as? Long ?: 0L)
            "Vector2f" -> {
                if (v !is Vector2f) return "vec2(0)"
                "vec2(${v.x},${v.y})"
            }
            "Vector3f" -> {
                if (v !is Vector3f) return "vec3(0)"
                "vec3(${v.x},${v.y},${v.z})"
            }
            "Vector4f" -> {
                if (v !is Vector4f) return "vec4(0)"
                "vec4(${v.x},${v.y},${v.z},${v.w})"
            }
            "Bool", "Boolean" -> {
                if (v !is Boolean) return "false"
                v.toString()
            }
            else -> throw IllegalArgumentException("Unknown type ${c.type}")
        }).toString()
    }

    fun constEval(c: NodeInput): Any? {
        return if (c.others.isEmpty()) c.value ?: c.defaultValue
        else null
    }

    /**
     * creates code; returns true, if extra return is needed
     * */
    fun createTree(n: Node?, depth: Int): Boolean {
        n ?: return true
        if (!processedNodes.add(n)) {
            throw IllegalStateException("Illegal loop for ${n.javaClass.name}")
        }
        return when (n) {
            is StartNode -> createTree(n.getOutputNode(0), depth)
            is ForNode -> {
                val ki = k++
                val body = n.getOutputNode(0)
                if (body != null) {
                    val startValue = expr(n.inputs!![1])
                    val endValue = expr(n.inputs!![2])
                    val step = expr(n.inputs!![3])
                    val desc = expr(n.inputs!![4])
                    builder.append(
                        "" +
                                "bool d$ki=$desc;\n" +
                                "for(int i$ki=$startValue-(d$ki?1:0);d$ki?i$ki>=$endValue:i$ki<$endValue;i$ki+=$step"
                    )
                    builder.append("){\n")
                    createTree(body, depth + 1)
                    builder.append("}\n")
                }
                createTree(n.getOutputNode(2), depth)
            }
            is MaterialReturnNode -> {
                // define all values :)
                for (i in MaterialGraph.layers.indices) {
                    val l = MaterialGraph.layers[i]
                    val c = n.inputs!![i + 1]
                    if (shallExport(l, c)) {
                        // set value :)
                        val x = expr(c)
                        builder.append(l.glslName).append("=")
                        // clamping could be skipped, if we were sure that the value is within bounds
                        when (if (l.highDynamicRange) null else l.map01) {
                            "*0.5+0.5" -> builder.append("clamp(").append(x).append(",-1.0,1.0)")
                            "" -> builder.append("clamp(").append(x).append(",0.0,1.0)")
                            null -> builder.append(x)
                            else -> builder.append(x)
                        }
                        builder.append(";\n")
                    } // else skip this
                }
                // jump to return
                builder.append("return false;\n")
                false
            }
            is DiscardNode -> {
                builder.append("return true;\n")
                false
            }
            is WhileNode -> {
                val body = n.getOutputNode(0)
                val cc = constEval(n.inputs!![1])
                if (body != null && cc != false) {
                    val cond = expr(n.inputs!![1])
                    builder.append("while((")
                    builder.append(cond)
                    builder.append(") && (budget--)>0){\n")
                    createTree(body, depth + 1)
                    builder.append("}\n")
                }
                createTree(n.getOutputNode(1), depth)
            }
            is DoWhileNode -> {
                val body = n.getOutputNode(0)
                val cc = constEval(n.inputs!![1])
                if (body != null && cc != false) {
                    builder.append("do {")
                    createTree(body, depth + 1)
                    val cond = expr(n.inputs!![1])
                    builder.append("} while((\n")
                    builder.append(cond)
                    builder.append(") && (budget--)>0);\n")
                }
                createTree(n.getOutputNode(1), depth)
            }
            is IfElseNode -> {
                val tr = n.getOutputNode(0)
                val fs = n.getOutputNode(1)
                // constant eval if possible
                when (constEval(n.inputs!![1])) {
                    true -> createTree(tr, depth)
                    false -> createTree(fs, depth)
                    else -> {
                        val cond = expr(n.inputs!![1])
                        if (tr != null && fs != null) {
                            builder.append("if(")
                            builder.append(cond)
                            builder.append("){\n")
                            val x = createTree(tr, depth)
                            builder.append("} else {\n")
                            val y = createTree(fs, depth)
                            builder.append("}\n")
                            x || y
                        } else if (tr != null || fs != null) {
                            builder.append(if (tr != null) "if((" else "if(!(")
                            builder.append(cond)
                            builder.append(")){\n")
                            val tmp = createTree(tr ?: fs, depth)
                            builder.append("}\n")
                            tmp
                        } else true// else nothing
                    }
                }
            }
            is SetLocalVariableNode -> {
                if (n.type != "?") {
                    val value = expr(n.inputs!![2])
                    builder.append(getLocalVarName(n.getKey(g), n.type))
                        .append("=").append(value).append(";\n")
                }
                // continue
                createTree(n.getOutputNode(0), depth)
            }
            // we could use it later for debugging :)
            is PrintNode -> createTree(n.getOutputNode(0), depth)
            else -> throw NotImplementedError("Unsupported node type ${n.javaClass.name}")
        }
    }

    val typeValues = HashMap<String, TypeValue>()

    fun filter(shader: Shader, name: String, tex: Texture2D, linear: Boolean): Texture2D {
        val filter = if (linear) GPUFiltering.LINEAR else GPUFiltering.NEAREST
        if (tex.filtering != filter || tex.clamping != Clamping.REPEAT) {
            val idx = shader.getTextureIndex(name)
            if (idx >= 0) {
                tex.bind(idx)
                tex.ensureFilterAndClamping(filter, Clamping.REPEAT)
            }
        }
        return tex
    }

    init {

        builder.append("bool calc(")
        val outs = start.outputs!!
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

        // traverse graph to find, which layers were exported
        val exportedLayers = BitSet(MaterialGraph.layers.size)
        fun traverse(node: Node?) {
            node ?: return
            if (!processedNodes.add(node)) {
                throw IllegalStateException("Illegal loop for ${node.javaClass.name}")
            }
            when (node) {
                is StartNode -> traverse(node.getOutputNode(0))
                is ForNode -> {
                    traverse(node.getOutputNode(0))
                    traverse(node.getOutputNode(2))
                }
                is WhileNode, is DoWhileNode, is IfElseNode -> {
                    traverse(node.getOutputNode(0))
                    traverse(node.getOutputNode(1))
                }
                is MaterialReturnNode -> {
                    for (i in MaterialGraph.layers.indices) {
                        if (!exportedLayers[i] && shallExport(MaterialGraph.layers[i], node.inputs!![i + 1])) {
                            exportedLayers[i] = true
                        }
                    }
                }
                is ActionNode -> traverse(node.getOutputNode(0))
            }
        }
        traverse(start)
        processedNodes.clear()

        for (i in MaterialGraph.layers.indices) {
            if (exportedLayers[i]) {
                val l = MaterialGraph.layers[i]
                if (!first) builder.append(", ")
                builder
                    .append("inout ")
                    .append(DeferredSettingsV2.glslTypes[l.dimensions - 1])
                    .append(" ").append(l.glslName)
                first = false
            }
        }
        builder.append("){\nint budget=").append(budget).append(";\n")
        val funcHeader = builder.toString()
        builder.clear()
        if (createTree(start, 1))
            builder.append("return false;\n")
        builder.append("}\n")

        val funcBody = builder.toString()
        builder.clear()

        for ((k, v) in localVars) {
            val type = kotlinToGLSL(v.second)
            builder.append(type).append(' ').append(v.first)
                .append('=').append(type).append("(0);//").append(k).append("\n")
        }
        for ((_, v) in textureVars) {
            builder.append("vec4 ").append(v).append("=vec4(0);").append("\n")
        }

        val usedVars = ArrayList<Variable>()
        for ((file, data) in textures) {
            val (name, linear) = data
            typeValues[name] =
                TypeValueV2(GLSLType.S2D) {
                    val tex = ImageGPUCache[file, true]
                    if (tex != null) filter(shader.value, name, tex, linear)
                    else TextureLib.missingTexture
                }
        }
        var lastGraphInvalidation = 0L
        for ((node, data) in movies) {
            val (name, linear) = data
            typeValues[name] =
                TypeValueV2(GLSLType.S2D) {
                    val file = node.file
                    val meta = getMeta(file, true)
                    if (meta != null && meta.hasVideo) {
                        val time1 = Engine.gameTime
                        if (time1 != lastGraphInvalidation) {
                            g.invalidate()
                            lastGraphInvalidation = time1
                        }
                        val time = node.getInput(g, 2) as Float
                        val frameCount = max(1, meta.videoFrameCount)
                        var frameIndex = (time * meta.videoFPS).toInt() % frameCount
                        if (frameIndex < 0) frameIndex += frameCount
                        val bufferLength = FileExplorerEntry.videoBufferLength
                        val timeout = 1000L
                        val fps = meta.videoFPS
                        // load future and previous frames
                        for (di in -2..2) {
                            if (di != 0) VideoCache.getVideoFrame(
                                file, 1, (frameIndex + bufferLength * di) % frameCount,
                                bufferLength, fps, timeout, meta, true
                            )
                        }
                        val tex = VideoCache.getVideoFrame(
                            file, 1, frameIndex,
                            bufferLength, fps, timeout, meta, true
                        )
                        if (tex != null && tex.get2DShader() == ShaderLib.shader2DRGBA) {
                            val tex2 = tex.getTextures()[0]
                            filter(shader.value, name, tex2, linear)
                        } else TextureLib.blackTexture

                    } else TextureLib.blackTexture
                }
        }
        val funcVars = builder.toString()
        builder.clear()

        val functions = funcBuilder.toString() + funcHeader + funcVars + funcBody

        // call method with all required parameters
        val layers = MaterialGraph.layers
        val layer0Name = layers[0].glslName
        if (exportedLayers[0]) {
            val v = DeferredLayerType.COLOR.defaultValueARGB
            builder.append("vec4 ").append(layer0Name).append("=vec4(${v.x},${v.y},${v.z},1.0);\n")
        }
        builder.append("if(calc(")
        first = true
        // all inputs
        for (i in 1 until start.outputs!!.size) {
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
        println(functions)
        println(funcCall)

        for ((k, v) in typeValues) {
            usedVars += Variable(v.type, k)
        }

        shader = object : ECSMeshShader(g.name) {

            override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
                super.bind(shader, renderer, instanced)
                for ((k, v) in typeValues) {
                    v.bind(shader, k)
                }
            }

            override fun createFragmentStage(
                isInstanced: Boolean,
                isAnimated: Boolean,
                motionVectors: Boolean
            ): ShaderStage {
                // super.createFragmentStage(isInstanced, isAnimated, motionVectors)
                // todo only if not already added
                // todo only if used
                val vars = usedVars + super.createFragmentVariables(isInstanced, isAnimated, motionVectors)
                return ShaderStage(
                    "calc", vars,
                    discardByCullingPlane +
                            normalTanBitanCalculation +
                            funcCall +
                            // todo only if clear coat is defined
                            // v0 + clearCoatCalculation +
                            reflectionPlaneCalculation +
                            (if (motionVectors) finalMotionCalculation else "")
                ).add(functions)
            }
        }
    }

}