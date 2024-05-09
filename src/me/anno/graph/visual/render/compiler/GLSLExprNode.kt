package me.anno.graph.visual.render.compiler

import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput

interface GLSLExprNode {
    /**
     * creates expression code; returns true, if extra return is needed
     * */
    fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node)
}