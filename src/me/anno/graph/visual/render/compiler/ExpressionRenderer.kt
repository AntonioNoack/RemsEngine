package me.anno.graph.visual.render.compiler

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.Graph
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.node.NodeInput
import me.anno.graph.visual.render.Texture
import me.anno.maths.Maths
import me.anno.utils.assertions.assertTrue
import org.joml.Vector4f

interface ExpressionRenderer {

    var shader: Shader?
    val graph: Graph?
    val name: String
    val inputs: List<NodeInput>
    var typeValues: HashMap<String, TypeValue>?

    fun getInput(index: Int): Any?
    fun setInput(index: Int, value: Any?)

    fun init() {
        setInput(1, Vector4f(1f, 0f, 1f, 1f))
        setInput(2, 256)
        setInput(3, 256)
        setInput(4, 3)
        setInput(5, 1)
    }

    fun invalidate() {
        shader?.destroy()
        shader = null
    }

    private fun createShader(): Shader {
        val compiler = object : GraphCompiler(graph as FlowGraph) {

            // not being used, as we only have an expression
            override fun handleReturnNode(node: ReturnNode) = throw NotImplementedError()

            val shader: Shader

            init {
                assertTrue(builder.isEmpty())
                expr(inputs[1])
                val expr = builder.toString()
                builder.clear()
                defineLocalVars(builder)
                val variables = typeValues.map { (k, v) -> Variable(v.type, k) } +
                        listOf(Variable(GLSLType.V4F, "result", VariableMode.OUT)) +
                        DepthTransforms.depthVars
                shader = Shader(
                    "ExpressionRenderer", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader,
                    ShaderLib.uvList, variables, extraFunctions.toString() +
                            builder.toString() +
                            "void main(){\n" +
                            "   result = $expr;\n" +
                            "}\n"
                )
                shader.setTextureIndices(variables.filter { it.type.isSampler }.map { it.name })
                shader.ignoreNameWarnings("d_camRot,d_orthoMat")
            }

            override val currentShader: Shader get() = shader
        }
        typeValues = compiler.typeValues
        return compiler.shader
    }

    fun render(fp: Boolean): Texture {

        // trivial fast path:
        if (inputs[1].others.isEmpty()) {
            return Texture(Vector4f(1f, 0f, 1f, 1f))
        }

        // fast path:
        if (inputs[1].others.firstOrNull()?.type == "Texture") {
            val directReturn = getInput(1)
            if (directReturn is Texture) {
                return directReturn
            }
        }

        // slow path:
        val w = getInput(2) as Int
        val h = getInput(3) as Int
        val channels = Maths.clamp(getInput(4) as Int, 1, 4)
        val samples = Maths.clamp(getInput(5) as Int, 1, GFX.maxSamples)

        val shader = shader ?: createShader()
        this.shader = shader

        val buffer = FBStack["expr-renderer", w, h, channels, fp, samples, DepthBufferType.NONE]
        GFXState.useFrame(w, h, false, buffer) {
            GFXState.renderPurely {
                shader.use()
                DepthTransforms.bindDepthUniforms(shader)
                val tv = typeValues
                if (tv != null) {
                    for ((k, v) in tv) {
                        v.bind(shader, k)
                    }
                }
                SimpleBuffer.flat01.draw(shader)
            }
        }

        return Texture.texture(buffer, 0)
    }
}