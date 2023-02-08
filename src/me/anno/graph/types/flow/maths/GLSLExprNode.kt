package me.anno.graph.types.flow.maths

interface GLSLExprNode {
    fun getShaderFuncName(outputIndex: Int): String
    fun defineShaderFunc(outputIndex: Int): String?
}