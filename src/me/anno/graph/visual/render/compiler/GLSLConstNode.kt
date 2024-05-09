package me.anno.graph.visual.render.compiler

import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput

interface GLSLConstNode : GLSLExprNode {

    fun getGLSLName(outputIndex: Int): String

    override fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node) {
        g.builder.append(getGLSLName(n.outputs.indexOf(out)))
    }
}