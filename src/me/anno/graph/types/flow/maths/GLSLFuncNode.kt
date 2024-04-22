package me.anno.graph.types.flow.maths

interface GLSLFuncNode {
    fun getShaderFuncName(outputIndex: Int): String
    fun defineShaderFunc(outputIndex: Int): String? = null
}