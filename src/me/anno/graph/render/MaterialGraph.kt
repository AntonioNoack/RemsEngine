package me.anno.graph.render

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.Material
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.SceneView.Companion.testScene2
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2.Companion.glslTypes
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.graph.Node
import me.anno.graph.NodeInput
import me.anno.graph.NodeOutput
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.flow.ReturnNode
import me.anno.graph.types.flow.StartNode
import me.anno.graph.types.flow.actions.PrintNode
import me.anno.graph.types.flow.control.DoWhileNode
import me.anno.graph.types.flow.control.ForNode
import me.anno.graph.types.flow.control.IfElseNode
import me.anno.graph.types.flow.control.WhileNode
import me.anno.graph.types.flow.local.GetLocalVariableNode
import me.anno.graph.types.flow.local.SetLocalVariableNode
import me.anno.graph.types.flow.maths.*
import me.anno.graph.types.flow.vector.*
import me.anno.graph.ui.GraphEditor
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToLong
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*

// todo bug: <tab> in vector input not switching to next one

fun kotlinToGLSL(type: String): String {
    return when (type) {
        "Float" -> "float"
        "Vector2f" -> "vec2"
        "Vector3f" -> "vec3"
        "Vector4f" -> "vec4"
        else -> throw NotImplementedError(type)
    }
}

class MaterialReturnNode : ReturnNode(
    MaterialGraph.layers.map {
        listOf(MaterialGraph.types[it.dimensions - 1], it.name)
    }.flatten()
)

fun convert(srcType: String, dstType: String, expr: String): String? {
    if (srcType == dstType) return expr
    if (srcType == "Boolean") return convert("Bool", dstType, expr)
    else if (dstType == "Boolean") return convert(srcType, "Bool", expr)
    return when (srcType) {
        "Bool" -> when (dstType) {
            "Int" -> "$expr?1:0"
            "Float" -> "$expr?1.0:0.0"
            "Vector2f" -> "vec2($expr?1.0:0.0)"
            "Vector3f" -> "vec3($expr?1.0:0.0)"
            "Vector4f" -> "vec4($expr?1.0:0.0)"
            else -> null
        }
        "Float" -> when (dstType) {
            "Bool" -> "$expr!=0.0"
            "Int" -> "int($expr)"
            "Vector2f" -> "vec2($expr)"
            "Vector3f" -> "vec3($expr)"
            "Vector4f" -> "vec4($expr)"
            else -> null
        }
        "Int" -> when (dstType) {
            "Bool" -> "$expr!=0"
            "Float" -> "float($expr)"
            "Vector2f" -> "vec2($expr)"
            "Vector3f" -> "vec3($expr)"
            "Vector4f" -> "vec4($expr)"
            else -> null
        }
        "Vector2f" -> when (dstType) {
            "Bool" -> "$expr!=vec2(0)"
            "Float" -> "length($expr)"
            "Vector3f" -> "vec3($expr,0.0)"
            "Vector4f" -> "vec4($expr,0.0,0.0)"
            else -> null
        }
        "Vector3f" -> when (dstType) {
            "Bool" -> "$expr!=vec3(0)"
            "Float" -> "length($expr)"
            "Vector2f" -> "($expr).xy"
            "Vector4f" -> "vec4($expr,0.0)"
            else -> null
        }
        "Vector4f" -> when (dstType) {
            "Bool" -> "$expr!=vec4(0)"
            "Float" -> "length($expr)"
            "Vector2f" -> "($expr).xy"
            "Vector3f" -> "($expr).xyz"
            else -> null
        }
        else -> null
    }
}

class DiscardNode : ReturnNode("Discard")

object MaterialGraph {

    val layers = arrayOf(
        DeferredLayerType.COLOR,
        DeferredLayerType.EMISSIVE,
        DeferredLayerType.NORMAL,
        DeferredLayerType.TANGENT,
        DeferredLayerType.POSITION,
        DeferredLayerType.METALLIC,
        DeferredLayerType.ROUGHNESS,
        DeferredLayerType.OCCLUSION,
        // DeferredLayerType.TRANSLUCENCY,
        // DeferredLayerType.SHEEN,
        // DeferredLayerType.SHEEN_NORMAL,
        // DeferredLayerType.CLEAR_COAT,
        // DeferredLayerType.CLEAT_COAT_ROUGH_METALLIC,
        // DeferredLayerType.CLEAR_COAT_NORMAL,
        // DeferredLayerType.SUBSURFACE,
        // DeferredLayerType.ANISOTROPIC,
        // DeferredLayerType.INDEX_OF_REFRACTION,
    )

    val types = arrayOf(
        "Float",
        "Vector2f",
        "Vector3f",
        "Vector4f"
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val g = object : FlowGraph() {
            override fun canConnectTypeToOtherType(srcType: String, dstType: String): Boolean {
                return convert(srcType, dstType, "") != null
            }
        }
        // todo create simple calculation
        val start = StartNode(
            listOf(
                "Vector3f", "Local Position",
                "Vector3f", "CamSpace Position",
                "Vector2f", "UVs",
                "Vector3f", "Normal",
                "Vector4f", "Tangent",
                "Vector4f", "Vertex Color",
            )
        )
        g.nodes.add(start)
        start.position.set(-200.0, 0.0, 0.0)
        // define return node
        val ret = MaterialReturnNode()
        for (i in layers.indices) {
            val layer = layers[i]
            val v = layer.defaultValueARGB
            val defaultValue: Any = when (layer.dimensions) {
                1 -> v.z
                2 -> Vector2f(v.y, v.z)
                3 -> Vector3f(v.x, v.y, v.z)
                else -> v
            }
            val con = ret.inputs!![i + 1]
            con.value = defaultValue
            con.defaultValue = defaultValue
        }
        g.nodes.add(ret)
        ret.position.set(200.0, 0.0, 0.0)
        start.connectTo(ret)
        val m = Material()

        fun compile() {
            m.shader = MatGraphCompiler(start, g).shader
        }
        compile()
        // show resulting material as preview
        testUI {
            val ui = CustomList(false, style)
            val ge = GraphEditor(g, style)
            ge.library = NodeLibrary(
                ge.library.nodes + listOf(
                    { DiscardNode() },
                    { MaterialReturnNode() })
            )
            ge.addChangeListener { _, isNodePositionChange ->
                if (!isNodePositionChange) {
                    compile()
                }
            }
            ui.add(ge, 1f)
            ui.add(testScene2(m))
        }
    }
}

class MatGraphCompiler(
    start: StartNode,
    val g: FlowGraph,
) {

    // todo uniform input block? maybe :)

    // todo compile material into shader
    //  - compile all ActionNodes into a block of instructions
    //  - if a graph ends, discard the pixel? idk...

    val builder = StringBuilder()
    val processedNodes = HashSet<Node>(g.nodes.size)

    val prefix = "tmp_"
    val conDefines = HashMap<NodeOutput, String>()

    val shader: ECSMeshShader

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
    val typeToFunc = HashMap<Any, String>() // key -> name
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
                typeToFunc.getOrPut(n) { defineFunc(name, kotlinToGLSL(out.type), n.defineShaderFunc(c)) }
                when (n.inputs?.size ?: 0) {
                    0 -> "$name()"
                    1 -> "$name(${expr(n.inputs!![0])})"
                    2 -> "$name(${expr(n.inputs!![0])},${expr(n.inputs!![1])})"
                    3 -> "$name(${expr(n.inputs!![0])},${expr(n.inputs!![1])},${expr(n.inputs!![2])})"
                    4 -> "$name(${expr(n.inputs!![0])},${expr(n.inputs!![1])},${expr(n.inputs!![2])},${expr(n.inputs!![3])})"
                    else -> throw NotImplementedError()
                }
            }
            is DotProductF2, is DotProductF3, is DotProductF4 -> {
                val a = expr(n.inputs!![0])
                val b = expr(n.inputs!![1])
                "dot($a,$b)"
            }
            is CrossProductF3 -> {
                val a = expr(n.inputs!![0])
                val b = expr(n.inputs!![1])
                "cross($a,$b)"
            }
            is CombineVector2f -> {
                val a = expr(n.inputs!![0])
                val b = expr(n.inputs!![1])
                "vec2($a,$b)"
            }
            is CombineVector3f -> {
                val a = expr(n.inputs!![0])
                val b = expr(n.inputs!![1])
                val c = expr(n.inputs!![2])
                "vec3($a,$b,$c)"
            }
            is CombineVector4f -> {
                val a = expr(n.inputs!![0])
                val b = expr(n.inputs!![1])
                val c = expr(n.inputs!![2])
                val d = expr(n.inputs!![3])
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
                val an = n.inputs!![0]
                val bn = n.inputs!![1]
                val a = expr(an)
                val b = convert(bn.type, an.type, expr(bn))!!
                val symbol = n.compType.glslName
                "($a)$symbol($b)"
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
            "Boolean" -> {
                if (v !is Boolean) return "false"
                v.toString()
            }
            else -> throw IllegalArgumentException("Unknown type ${c.type}")
        }).toString()
    }

    fun createTree(node: Node?, depth: Int) {
        node ?: return
        if (!processedNodes.add(node)) {
            throw IllegalStateException("Illegal loop for ${node.javaClass.name}")
        }
        when (node) {
            is StartNode -> createTree(node.getOutputNode(0), depth)
            is ForNode -> {
                val ki = k++
                val body = node.getOutputNode(0)
                if (body != null) {
                    val startValue = expr(node.inputs!![1])
                    val endValue = expr(node.inputs!![2])
                    val step = expr(node.inputs!![3])
                    val desc = expr(node.inputs!![4])
                    builder.append(
                        "" +
                                "bool d$ki=$desc;\n" +
                                "for(int i$ki=$startValue-(d$ki?1:0);d$ki?i$ki>=$endValue:i$ki<$endValue;i$ki+=$step"
                    )
                    builder.append("){\n")
                    createTree(body, depth + 1)
                    builder.append("}\n")
                }
                createTree(node.getOutputNode(2), depth)
            }
            is MaterialReturnNode -> {
                // define all values :)
                for (i in MaterialGraph.layers.indices) {
                    val l = MaterialGraph.layers[i]
                    val c = node.inputs!![i + 1]
                    if (shallExport(l, c)) {
                        // set value :)
                        val x = expr(c)
                        builder.append(l.glslName).append("=")
                        builder.append(x)
                        builder.append(";\n")
                    } // else skip this
                }
                // jump to return
                builder.append("return false;\n")
            }
            is DiscardNode -> {
                builder.append("return true;\n")
            }
            is WhileNode -> {
                val body = node.getOutputNode(0)
                if (body != null) {
                    val cond = expr(node.inputs!![1])
                    builder.append("while(")
                    builder.append(cond)
                    builder.append("){\n")
                    createTree(body, depth + 1)
                    builder.append("}\n")
                }
                createTree(node.getOutputNode(1), depth)
            }
            is DoWhileNode -> {
                val body = node.getOutputNode(0)
                if (body != null) {
                    builder.append("do {")
                    createTree(body, depth + 1)
                    val cond = expr(node.inputs!![1])
                    builder.append("} while(\n")
                    builder.append(cond)
                    builder.append(");\n")
                }
                createTree(node.getOutputNode(1), depth)
            }
            is IfElseNode -> {
                val tr = node.getOutputNode(0)
                val fs = node.getOutputNode(1)
                val cond = expr(node.inputs!![1])
                if (tr != null && fs != null) {
                    builder.append("if(")
                    builder.append(cond)
                    builder.append("){\n")
                    createTree(tr, depth)
                    builder.append("} else {\n")
                    createTree(fs, depth)
                    builder.append("}\n")
                } else if (tr != null || fs != null) {
                    builder.append(if (tr != null) "if((" else "if(!(")
                    builder.append(cond)
                    builder.append(")){\n")
                    createTree(tr ?: fs, depth)
                    builder.append("}\n")
                }// else nothing
            }
            // we could use it later for debugging :)
            is PrintNode -> createTree(node.getOutputNode(0), depth)
            is SetLocalVariableNode -> {
                if (node.type != "?") {
                    val value = expr(node.inputs!![2])
                    builder.append(getLocalVarName(node.getKey(g), node.type))
                        .append("=").append(value).append(";\n")
                }
                // continue
                createTree(node.getOutputNode(0), depth)
            }
            else -> throw NotImplementedError("Unsupported node type ${node.javaClass.name}")
        }
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
                is PrintNode -> traverse(node.getOutputNode(0))
                is GetLocalVariableNode -> {
                    if (node.type != "?") getLocalVarName(node.getInput(g, 0).toString(), node.type)
                }
                is SetLocalVariableNode -> {
                    if (node.type != "?") getLocalVarName(node.getInput(g, 1).toString(), node.type)
                    traverse(node.getOutputNode(0))
                }
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
                    .append(glslTypes[l.dimensions - 1])
                    .append(" ").append(l.glslName)
                first = false
            }
        }
        builder.append("){\n")
        val funcHeader = builder.toString()
        builder.clear()
        createTree(start, 1)
        builder.append("return false;\n")
        builder.append("}\n")

        val funcBody = builder.toString()
        builder.clear()

        for ((k, v) in localVars) {
            val type = kotlinToGLSL(v.second)
            builder.append(type).append(' ').append(v.first)
                .append('=').append(type).append("(0);//").append(k).append("\n")
        }
        val funcVars = builder.toString()
        builder.clear()

        val functions = funcBuilder.toString() + funcHeader + funcVars + funcBody

        // call method with all required parameters
        builder.append("if(calc(")
        first = true
        // all inputs
        val usedVars = ArrayList<Variable>()
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
                        6 -> {
                            usedVars += Variable(GLSLType.V4F, "vertexColor")
                            "vertexColor"
                        }
                        else -> throw NotImplementedError()
                    }
                )
                first = false
            }
        }
        // all outputs
        for (i in MaterialGraph.layers.indices) {
            if (exportedLayers[i]) {
                if (!first) builder.append(", ")
                builder.append(MaterialGraph.layers[i].glslName)
                first = false
            }
        }
        builder.append(")) discard;\n")

        val funcCall = builder.toString()
        println(functions)
        println(funcCall)

        shader = object : ECSMeshShader(g.name) {
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