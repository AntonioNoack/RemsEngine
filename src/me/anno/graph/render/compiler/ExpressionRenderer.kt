package me.anno.graph.render.compiler

import me.anno.ecs.components.mesh.TypeValue
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.graph.Graph
import me.anno.graph.NodeInput
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ReturnNode
import me.anno.maths.Maths
import org.joml.Vector4f

interface ExpressionRenderer {

    var shader: Shader?
    var buffer: Framebuffer?
    val graph: Graph?
    val name: String
    val inputs: Array<NodeInput>?
    var typeValues: HashMap<String, TypeValue>?

    fun getInput(index: Int): Any?
    fun setInput(index: Int, value: Any?)

    fun init() {
        setInput(1, Vector4f(1f, 0f, 1f, 1f))
        setInput(2, 256)
        setInput(3, 256)
        setInput(4, 4)
        setInput(5, 1)
    }

    fun invalidate() {
        shader?.destroy()
        shader = null
    }

    fun render(fp: Boolean): ITexture2D {
        val shader = shader ?: kotlin.run {
            val compiler = object : GraphCompiler(graph as FlowGraph) {

                // not being used, as we only have an expression
                override fun handleReturnNode(node: ReturnNode) = throw NotImplementedError()

                val shader: Shader

                init {
                    val expr = expr(inputs!![1])
                    defineLocalVars(builder)
                    val variables = typeValues.map { (k, v) -> Variable(v.type, k) } +
                            listOf(Variable(GLSLType.V4F, "result", VariableMode.OUT))
                    shader = Shader(
                        "ExpressionRenderer", ShaderLib.coordsList, ShaderLib.coordsVShader,
                        ShaderLib.uvList, variables, extraFunctions.toString() +
                                builder.toString() +
                                "void main(){\n" +
                                "   result = $expr;\n" +
                                "}\n"
                    )
                    shader.setTextureIndices(variables.filter {
                        when (it.type) {
                            GLSLType.S2D, GLSLType.S2DI, GLSLType.S2DU, GLSLType.S2DA, GLSLType.S3D, GLSLType.S2DMS,
                            GLSLType.SCube -> true
                            else -> false
                        }
                    }.map { it.name })
                }

                override val currentShader: Shader get() = shader


            }
            typeValues = compiler.typeValues
            compiler.shader
        }
        this.shader = shader

        val w = getInput(2) as Int
        val h = getInput(3) as Int
        val channels = Maths.clamp(getInput(4) as Int, 1, 4)
        val samples = Maths.clamp(getInput(5) as Int, 1, GFX.maxSamples)
        var buffer = buffer
        if (buffer == null || buffer.samples != samples || buffer.targets[0].channels != channels) {
            buffer?.destroy()
            buffer = Framebuffer(
                name, w, h, samples,
                arrayOf(
                    if (fp) {
                        when (channels) {
                            1 -> TargetType.FloatTarget1
                            2 -> TargetType.FloatTarget2
                            3 -> TargetType.FloatTarget3
                            else -> TargetType.FloatTarget4
                        }
                    } else {
                        when (channels) {
                            1 -> TargetType.UByteTarget1
                            2 -> TargetType.UByteTarget2
                            3 -> TargetType.UByteTarget3
                            else -> TargetType.UByteTarget4
                        }
                    }
                ), DepthBufferType.NONE
            )
            this.buffer = buffer
        }

        GFXState.useFrame(w, h, true, buffer) {
            GFXState.renderPurely {
                shader.use()
                val tv = typeValues
                if (tv != null) {
                    for ((k, v) in tv) {
                        v.bind(shader, k)
                    }
                }
                GFX.flat01.draw(shader)
            }
        }

        return buffer.getTexture0()
    }
}