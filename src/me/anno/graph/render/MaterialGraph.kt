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
import me.anno.graph.Graph
import me.anno.graph.Node
import me.anno.graph.NodeInput
import me.anno.graph.NodeOutput
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ReturnNode
import me.anno.graph.types.flow.StartNode
import me.anno.graph.types.flow.control.ForNode
import me.anno.graph.types.flow.control.IfElseNode
import me.anno.graph.types.flow.control.WhileNode
import me.anno.graph.types.flow.maths.*
import me.anno.graph.types.flow.vector.CombineVector3fNode
import me.anno.graph.types.flow.vector.SeparateVector3fNode
import me.anno.graph.ui.GraphEditor
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToLong
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*

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

    class MaterialReturnNode : ReturnNode(
        layers.map {
            listOf(types[it.dimensions - 1], it.name)
        }.flatten()
    )

    // todo add discard node to library
    class DiscardNode : ReturnNode()

    @JvmStatic
    fun main(args: Array<String>) {
        val g = object : FlowGraph() {
            override fun canConnectTypeToOtherType(srcType: String, dstType: String): Boolean {
                return srcType == dstType
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
            ge.library
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
    g: Graph,
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

    fun shallExport(l: DeferredLayerType, c: NodeInput): Boolean {
        return l == DeferredLayerType.COLOR || c.others.isNotEmpty() || c.value != c.defaultValue
    }

    val funcBuilder = StringBuilder()
    val typeToFunc = HashMap<Any, String>() // key -> name
    fun defineFunc(name: String, prefix: String, suffix: String): String {
        funcBuilder.append(prefix).append(' ').append(name).append(suffix).append('\n')
        return name
    }

    // todo if types are not matching, convert them :)
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
            is CombineVector3fNode -> {
                val a = expr(n.inputs!![0])
                val b = expr(n.inputs!![1])
                val c = expr(n.inputs!![2])
                "vec3($a,$b,$c)"
            }
            is SeparateVector3fNode -> {
                val c = n.outputs!!.indexOf(out)
                val a = expr(n.inputs!![0])
                "($a).${"xyz"[c]}"
            }
            else -> throw IllegalArgumentException("Unknown node ${n.javaClass.name}")
        }
    }

    fun expr(c: NodeInput): String {
        val n0 = c.others.firstOrNull()
        if (n0 is NodeOutput) return expr(n0, n0.node!!)
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
            else -> throw IllegalArgumentException("Unknown type ${c.type}")
        }).toString()
    }

    fun createTree(node: Node?, depth: Int) {
        node ?: return
        if (!processedNodes.add(node)) {
            throw IllegalStateException("Illegal loop for ${node.javaClass.name}")
        }
        // todo define all local variables
        // todo define all outputs with their default values
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
            is MaterialGraph.MaterialReturnNode -> {
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
            is MaterialGraph.DiscardNode -> {
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
                is IfElseNode -> {
                    traverse(node.getOutputNode(0))
                    traverse(node.getOutputNode(1))
                }
                is ForNode -> {
                    traverse(node.getOutputNode(0))
                    traverse(node.getOutputNode(2))
                }
                is WhileNode -> {
                    traverse(node.getOutputNode(0))
                    traverse(node.getOutputNode(1))
                }
                is MaterialGraph.MaterialReturnNode -> {
                    for (i in MaterialGraph.layers.indices) {
                        if (!exportedLayers[i] && shallExport(MaterialGraph.layers[i], node.inputs!![i + 1])) {
                            exportedLayers[i] = true
                        }
                    }
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

        createTree(start, 1)
        builder.append("}\n")

        val functions = funcBuilder.toString() + builder.toString()

        builder.clear()
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
                            funcCall +
                            // todo only if clear coat is defined
                            // v0 + clearCoatCalculation +
                            reflectionPlaneCalculation +
                            (if (motionVectors) finalMotionCalculation else "")
                ).add(functions)
            }
        }
    }

    fun kotlinToGLSL(type: String): String {
        return when (type) {
            "Float" -> "float"
            "Vector2f" -> "vec2"
            "Vector3f" -> "vec3"
            "Vector4f" -> "vec4"
            else -> throw NotImplementedError()
        }
    }

}