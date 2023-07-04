package me.anno.graph.render.compiler

import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType.Companion.UByteTargets
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.graph.render.DiscardNode
import me.anno.graph.render.scene.UVNode
import me.anno.graph.render.scene.UViNode
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.flow.ReturnNode
import me.anno.graph.types.flow.StartNode
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.graph.ui.GraphEditor
import me.anno.graph.ui.GraphPanel
import me.anno.maths.Maths.clamp
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelList
import me.anno.ui.style.Style

// todo own internal graph
// todo customizable inputs

// todo alternatives, which are CubeBufferNode, Buffer3DNode, BufferArrayNode, ...

class ShaderGraphNode : ActionNode(
    "Shader Graph",
    // num channels, num samples, todo target type, depth mode
    // todo blend mode/conditional clear
    // multi-sampled/single-sampled output
    listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Channels",
        "Int", "Samples",
    ) + startListArgs,
    listOf(
        "Texture", "Result", // resampled
    )
) {

    companion object {
        val library = NodeLibrary(
            NodeLibrary.flowNodes.nodes + listOf(
                { SGNReturn() },
                { DiscardNode() },
                { UVNode() },
                { UViNode() },
            )
        )
        val startListArgs = listOf(
            "Texture", "Data0",
            "Texture", "Data1",
            "Texture", "Data2",
            "Texture", "Data3",
            "Texture", "Data4",
            "Texture", "Data5",
            "Texture", "Data6",
            "Texture", "Data7",
        )
    }

    val constInputs = 6

    fun invalidate() {
        shader?.destroy()
        shader = null
    }

    override fun canAddInput(type: String, index: Int): Boolean {
        return index >= constInputs && when (type) {
            "Bool", "Boolean", "Int", "Float", "Vector2f", "Vector3f", "Vector4f",
            "Texture" -> true
            else -> false
        }
    }

    override fun canRemoveInput(type: String, index: Int) = index >= constInputs

    init {
        setInput(1, 256)
        setInput(2, 256)
        setInput(3, 4)
        setInput(4, 1)
    }

    var shader: Shader? = null
    var buffer: Framebuffer? = null

    val graph1 = FlowGraph()
    val startNode = StartNode(startListArgs)

    init {
        graph1.add(startNode)
        val endNode = SGNReturn()
        graph1.add(endNode)
        startNode.position.set(-200.0, 0.0, 0.0)
        endNode.position.set(200.0, 0.0, 0.0)
        startNode.connectTo(endNode)
        startNode.connectTo(1, endNode, 1)
    }

    var budget = 1000

    override fun createUI(g: GraphPanel, list: PanelList, style: Style) {
        val button = TextButton("Edit", false, style)
        // todo prevent the start node from being deletable
        if (g is GraphEditor) {
            button.addLeftClickListener {
                g.push(graph1, library)
            }
        } else button.isEnabled = false
        list += button
    }

    class SGNReturn : ReturnNode(listOf("Vector4f", "Result"))

    override fun executeAction() {
        val g = graph1

        // copy textures
        for (i in 0 until 8) {
            startNode.setOutput(getInput(i + 5), i + 1)
        }

        val shader = shader ?: object : GraphCompiler(g) {

            // not being used, as we only have an expression
            override fun handleReturnNode(node: ReturnNode) {
                when (node) {
                    is SGNReturn -> builder.append("result = ").append(expr(node.inputs!![1]))
                        .append("; return false;\n")
                    is DiscardNode -> builder.append("return true;\n")
                    else -> throw NotImplementedError(node.className)
                }
            }

            val shader: Shader

            init {
                val start = g.nodes.filterIsInstance<StartNode>().first()
                builder.append("bool calc(inout vec4 result){\n")
                defineBudget(builder, budget)
                if (createTree(start, 1)) {
                    // missing return statement
                    builder.append("return true;\n")
                }
                builder.append("}\n")
                val body = builder.toString()
                builder.clear()
                defineLocalVars(builder)
                val locals = builder.toString()
                val fragmentVariables = typeValues.map { (k, v) -> Variable(v.type, k) } + extraVariables +
                        listOf(Variable(GLSLType.V4F, "result1", VariableMode.OUT))
                shader = Shader(
                    name, coordsList, coordsVShader, uvList,
                    fragmentVariables,
                    extraFunctions.toString() +
                            locals + body +
                            "void main(){\n" +
                            "   vec4 result = vec4(0.0);\n" +
                            "   if(calc(result)) discard;\n" +
                            "   result1 = result;\n" +
                            "}\n"
                )
            }

            override val currentShader: Shader get() = shader


        }.shader
        this.shader = shader
        val w = getInput(1) as Int
        val h = getInput(2) as Int
        val channels = clamp(getInput(3) as Int, 1, 4)
        val samples = getInput(4) as Int
        var buffer = buffer
        if (buffer == null || buffer.width != w || buffer.height != h) {
            buffer?.destroy()
            val target = UByteTargets[channels - 1]
            buffer = Framebuffer(
                name, w, h, samples,
                arrayOf(target), DepthBufferType.NONE
            )
        }
        useFrame(buffer) {
            renderPurely {
                shader.use()
                flat01.draw(shader)
            }
        }
        setOutput(buffer.getTexture0(), 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        shader?.destroy()
    }
}