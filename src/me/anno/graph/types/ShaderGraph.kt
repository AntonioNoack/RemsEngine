package me.anno.graph.types

import me.anno.gpu.GFX.flat01
import me.anno.gpu.shader.Shader
import me.anno.graph.types.flow.actions.ActionNode

object ShaderGraph {

    // todo convert flow-graph into shader graph as far as possible
    // todo compute static stuff on CPU, and uv dependent on GPU

    // todo this effectively is converting a flow graph into another flow graph,
    // todo just with custom shaders

    fun optimize(graph: FlowGraph): FlowGraph {
        val inputs = graph.inputs
        val outputs = graph.outputs // only first one???
        // todo find which nodes are required

        TODO()
    }

    class CustomShaderNode(val shader: Shader) : ActionNode("Shader") {

        override fun executeAction(graph: FlowGraph) {
            // todo we have inputs, and outputs
            // todo bind all inputs: textures & uniforms
            // todo define/get output buffer
            // todo draw
            shader.use()
            flat01.draw(shader)
        }

    }

}