package me.anno.graph.render.compiler

import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.simplestVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ReturnNode
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.maths.Maths.clamp
import org.joml.Vector4f

// this is a simple method for rendering, which does not allow loops
// todo alternatives, which are CubeBufferNode, Buffer3DNode, BufferArrayNode, ...

class ShaderExprNode : ActionNode(
    "Shader Expression",
    // num channels, num samples, todo target type, depth mode
    // todo blend mode/conditional clear
    // todo multi-sampled/single-sampled output
    listOf(
        "Vector4f", "Data",
        "Int", "Width",
        "Int", "Height",
        "Int", "Channels",
        "Int", "Samples",
    ),
    listOf(
        "Texture", "Result", // resampled
    )
) {

    init {
        setInput(1, Vector4f(1f, 0f, 1f, 1f))
        setInput(2, 256)
        setInput(3, 256)
        setInput(4, 4)
        setInput(5, 1)
    }

    var shader: Shader? = null
    var buffer: Framebuffer? = null
    override fun executeAction() {
        val shader = shader ?: object : GraphCompiler(graph as FlowGraph) {

            // not being used, as we only have an expression
            override fun handleReturnNode(node: ReturnNode) = throw NotImplementedError()

            val shader: Shader

            init {
                val expr = expr(inputs!![1])
                defineLocalVars(builder)
                shader = Shader(
                    name, coordsList, simplestVertexShader, uvList,
                    typeValues.map { (k, v) -> Variable(v.type, k) } +
                            listOf(Variable(GLSLType.V4F, "result", VariableMode.OUT)),
                    extraFunctions.toString() +
                            builder.toString() +
                            "void main(){\n" +
                            "   result = $expr;\n" +
                            "}\n"
                )
            }

            override val currentShader: Shader get() = shader


        }.shader
        this.shader = shader
        val w = getInput(2) as Int
        val h = getInput(3) as Int
        val channels = clamp(getInput(4) as Int, 1, 4)
        val samples = getInput(5) as Int
        var buffer = buffer
        if (buffer == null || buffer.w != w || buffer.h != h) {
            buffer?.destroy()
            buffer = Framebuffer(
                name, w, h, samples,
                arrayOf(
                    when (channels) {
                        1 -> TargetType.UByteTarget1
                        2 -> TargetType.UByteTarget2
                        3 -> TargetType.UByteTarget3
                        else -> TargetType.UByteTarget4
                    }
                ), DepthBufferType.NONE
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