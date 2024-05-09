package me.anno.graph.visual.render

import me.anno.Time
import me.anno.ecs.components.mesh.material.utils.TypeValueV2
import me.anno.gpu.shader.GLSLType
import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLExprNode
import me.anno.graph.visual.render.compiler.GraphCompiler

@Suppress("unused")
class GameTime : CalculationNode("Game Time (%1h,FP32)", emptyList(), listOf("Float", "Game Time")), GLSLExprNode {

    // repeats once every 1h to ensure relatively consistent accuracy
    // if you need more accuracy, create your own nodes, but remember that OpenGL uses single precision!
    override fun calculate() = (Time.gameTime % 3600f).toFloat()

    override fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node) {
        val key = "uGameTime"
        g.typeValues.getOrPut(key) {
            TypeValueV2(GLSLType.V1F) { Time.gameTime.toFloat() }
        }
        g.builder.append(key)
    }
}