package me.anno.graph.render.compiler

import me.anno.Engine
import me.anno.cache.instances.VideoCache
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.TypeValueV2
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.*
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.Node
import me.anno.graph.NodeConnector
import me.anno.graph.NodeInput
import me.anno.graph.NodeOutput
import me.anno.graph.render.*
import me.anno.graph.render.MaterialGraph.convert
import me.anno.graph.render.MaterialGraph.kotlinToGLSL
import me.anno.graph.render.MaterialGraph.types
import me.anno.graph.render.scene.TextureNode2
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ReturnNode
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
import me.anno.utils.Color.white4
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToLong
import me.anno.video.ffmpeg.FFMPEGMetadata
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*
import kotlin.math.max

abstract class GraphCompiler(val g: FlowGraph) {

    // todo uniform input block? maybe :)

    abstract val currentShader: Shader

    val builder = StringBuilder()
    val processedNodes = HashSet<Node>(g.nodes.size)

    val typeValues = HashMap<String, TypeValue>()

    val prefix = "tmp_"
    val conDefines = HashMap<NodeOutput, String>()

    var k = 0

    val localVars = HashMap<String, Pair<String, String>>() // name -> newName,type
    fun getLocalVarName(name: String, type: String?): String {
        return localVars.getOrPut("$name/$type") {
            Pair("l${localVars.size}", type!!)
        }.first
    }

    fun shallExport(l: DeferredLayerType, c: NodeInput): Boolean {
        return l == DeferredLayerType.COLOR || c.others.isNotEmpty() || c.currValue != c.defaultValue
    }

    val extraFunctions = StringBuilder()
    val extraVariables = ArrayList<Variable>()
    val typeToFunc = HashMap<String, String?>() // key -> name
    val movies = HashMap<MovieNode, Pair<String, Boolean>>() // file -> name, linear
    val textures = HashMap<FileReference, Pair<String, Boolean>>() // file -> name, linear
    val textures2 = HashMap<NodeInput, Pair<String, Boolean>>() // file -> name, linear

    init {
        typeToFunc["ONP"] = ""
        typeToFunc["R2D"] = ""
        extraFunctions.append(octNormalPacking)
        extraFunctions.append(rawToDepth)
        extraVariables.addAll(depthVars)
    }

    fun defineFunc(name: String, prefix: String, suffix: String?): String? {
        suffix ?: return null
        extraFunctions.append(prefix).append(' ').append(name).append(suffix).append('\n')
        return name
    }

    fun aType(an: NodeConnector, bn: NodeInput): String {
        if (an.type == "Texture") {
            val tex = bn.getValue() as? Texture ?: return "Vector4f"
            val map = tex.mapping
            val enc = tex.encoding
            val dim = enc?.workDims ?: map.length
            return if (dim == 0) {
                if (tex.tex == whiteTexture && tex.color == white4) "Float" else "Vector4f"
            } else types[dim - 1]
        } else return an.type
    }

    fun expr(out: NodeOutput, n: Node): String {
        val v = conDefines[out]
        if (v != null) return v
        return when (n) {
            is GLSLExprNode -> {
                val c = n.outputs!!.indexOf(out)
                val name = n.getShaderFuncName(c)
                val func = typeToFunc.getOrPut(name) { defineFunc(name, kotlinToGLSL(out.type), n.defineShaderFunc(c)) }
                if (func != null) {
                    val inputs = n.inputs
                    when (inputs?.size ?: 0) {
                        0 -> "$name()"
                        else -> "$name(${inputs!!.joinToString(",") { expr(it) }})"
                    }
                } else name
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
            is GetLocalVariableNode -> getLocalVarName(n.key, n.type)
            is SetLocalVariableNode -> getLocalVarName(n.key, n.type)
            is CompareNode -> {
                val inputs = n.inputs!!
                val an = inputs[0]
                val bn = inputs[1]
                val a = expr(an)
                val b = convert(bn.type, aType(an, bn), expr(bn))!!
                val symbol = n.compType.glslName
                "($a)$symbol($b)"
            }
            is ValueNode -> expr(n.inputs!![0])
            is TextureNode -> {
                val uv = expr(n.inputs!![0])
                val texName = textures.getOrPut(n.file) {
                    val linear = constEval(n.inputs!![1]) == true
                    // todo different color repeat modes in GLSL
                    Pair("tex1I${textures.size}", linear)
                }.first
                "texture($texName,$uv)"
            }
            is TextureNode2 -> {
                val uv = expr(n.inputs!![1])
                val input = out.others.firstOrNull() as? NodeInput
                if (input != null) {
                    val texName = textures2.getOrPut(input) {
                        val linear = constEval(n.inputs!![2]) == true
                        // todo different color repeat modes in GLSL
                        Pair("tex2I${textures2.size}", linear)
                    }.first
                    "texture($texName,$uv)"
                } else "vec4(1.0,0.0,1.0,1.0)"
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
            else -> when (out.type) {
                "Texture" -> {
                    val input = out.others.firstOrNull() as? NodeInput
                    val tex = input?.getValue() as? Texture
                    if (tex != null) {
                        val tint = tex.color
                        val tintStr = if (tint != white4) "vec4(${tint.x},${tint.y},${tint.z},${tint.z})" else null
                        val tex1 = if (tex.tex != whiteTexture) {
                            val texName = textures2.getOrPut(input) { Pair("tex2I${textures2.size}", true) }.first
                            "texture($texName,uv)${if (tintStr != null) "*$tintStr" else ""}"
                        } else tintStr ?: "1.0"
                        val map = tex.mapping
                        val tex2 = if (map.isEmpty()) tex1 else "$tex1.$map"
                        val enc = tex.encoding
                        if (enc != null) "(${enc.dataToWork}($tex2))" else tex2
                    } else "((int(floor(uv.x*4.0)+floor(uv.y*4.0)) & 1) != 0 ? vec4(1,0,1,1) : vec4(0,0,0,1))"
                }
                else -> throw IllegalArgumentException("Unknown node type ${out.type} by ${n.javaClass.name}")
            }
        }
    }

    fun expr(c: NodeInput): String {
        if (c.type == "Flow") throw IllegalArgumentException("Cannot request value of flow type")
        val n0 = c.others.firstOrNull()
        if (n0 is NodeOutput) { // it is connected
            val aType0 = aType(n0, c)
            return convert(aType0, c.type, expr(n0, n0.node!!))
                ?: throw IllegalStateException("Cannot convert ${n0.type}->$aType0 to ${c.type}!")
        }
        val v = c.currValue
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
        return if (c.others.isEmpty()) c.currValue ?: c.defaultValue
        else null
    }

    abstract fun handleReturnNode(node: ReturnNode)

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
            is ReturnNode -> {
                handleReturnNode(n)
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
                    builder.append(getLocalVarName(n.key, n.type))
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

    fun filter(shader: Shader, name: String, tex: ITexture2D, linear: Boolean): ITexture2D {
        if (tex is Texture2D) filter(shader, name, tex, linear)
        return tex
    }

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

    fun defineLocalVars(builder: StringBuilder) {
        for ((k, v) in localVars) {
            val type = kotlinToGLSL(v.second)
            builder.append(type).append(' ').append(v.first)
                .append('=').append(type).append("(0);//").append(k).append("\n")
        }
        defineTextures()
        defineMovies()
    }

    fun defineTextures() {
        for ((file, data) in textures) {
            val (name, linear) = data
            typeValues[name] =
                TypeValueV2(GLSLType.S2D) {
                    val tex = ImageGPUCache[file, true]
                    if (tex != null) filter(currentShader, name, tex, linear)
                    else TextureLib.missingTexture
                }
        }
        for ((node, data) in textures2) {
            val (name, linear) = data
            typeValues[name] = TypeValueV2(GLSLType.S2D) {
                when (val tex = node.getValue()) {
                    is ITexture2D -> filter(currentShader, name, tex, linear)
                    is Texture -> tex.tex ?: TextureLib.missingTexture
                    else -> TextureLib.missingTexture
                }
            }
        }
    }

    fun defineMovies() {
        for ((node, data) in movies) {
            val (name, linear) = data
            typeValues[name] =
                TypeValueV2(GLSLType.S2D) {
                    val file = node.file
                    val meta = FFMPEGMetadata.getMeta(file, true)
                    if (meta != null && meta.hasVideo) {
                        val time1 = Engine.gameTime
                        if (time1 != g.lastInvalidation) {
                            g.invalidate()
                            g.lastInvalidation = time1
                        }
                        val time = node.getInput(2) as Float
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
                            filter(currentShader, name, tex2, linear)
                        } else TextureLib.blackTexture

                    } else TextureLib.blackTexture
                }
        }
    }

    fun defineBudget(builder: StringBuilder, budget: Int) {
        builder.append("int budget=").append(budget).append(";\n")
    }

    fun findExportSet(start: Node, layers: Array<DeferredLayerType>): BitSet {
        processedNodes.clear()
        val exportedLayers = BitSet(layers.size)
        fun traverse(node: Node?) {
            node ?: return
            if (!processedNodes.add(node)) {
                throw IllegalStateException("Illegal loop for ${node.javaClass.name}")
            }
            when (node) {
                is StartNode, is ActionNode -> traverse(node.getOutputNode(0))
                is ForNode -> {
                    traverse(node.getOutputNode(0))
                    traverse(node.getOutputNode(2))
                }
                is WhileNode, is DoWhileNode, is IfElseNode -> {
                    traverse(node.getOutputNode(0))
                    traverse(node.getOutputNode(1))
                }
                is MaterialReturnNode -> {
                    for (i in layers.indices) {
                        if (!exportedLayers[i] && shallExport(layers[i], node.inputs!![i + 1])) {
                            exportedLayers[i] = true
                        }
                    }
                }
            }
        }
        traverse(start)
        return exportedLayers
    }

}