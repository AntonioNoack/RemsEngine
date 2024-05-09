package me.anno.graph.visual.render.compiler

interface GLSLFlowNode {
    /**
     * creates code; returns true, if extra return is needed
     * */
    fun buildCode(g: GraphCompiler, depth: Int): Boolean
}