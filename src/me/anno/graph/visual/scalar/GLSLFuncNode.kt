package me.anno.graph.visual.scalar

interface GLSLFuncNode {
    fun getShaderFuncName(outputIndex: Int): String
    fun defineShaderFunc(outputIndex: Int): String? = null
}