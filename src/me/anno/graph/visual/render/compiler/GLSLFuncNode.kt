package me.anno.graph.visual.render.compiler

import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.MaterialGraph.kotlinToGLSL

interface GLSLFuncNode : GLSLExprNode {

    fun getShaderFuncName(outputIndex: Int): String
    fun defineShaderFunc(outputIndex: Int): String? = null

    override fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node) {
        val c = n.outputs.indexOf(out)
        val name = getShaderFuncName(c)
        g.typeToFunc.getOrPut(name) {
            var shaderFunc = defineShaderFunc(c)
            if (!shaderFunc.isNullOrEmpty() && shaderFunc[0] != '(') { // short form
                shaderFunc = "(${shaderFuncPrefix(n)}){return $shaderFunc;}"
            }
            g.defineFunc(name, kotlinToGLSL(out.type), shaderFunc)
        }
        g.builder.append(name).append('(')
        // function arguments
        val inputs = n.inputs
        for (ni in inputs.indices) {
            if (ni > 0) g.builder.append(',')
            g.expr(inputs[ni])
        }
        g.builder.append(')')
    }

    companion object {
        fun shaderFuncPrefix(n: Node): String {
            return (n.inputs.indices step 2).joinToString(",") { i ->
                kotlinToGLSL(n.inputs[i].type) + " " + ('a' + i)
            }
        }
    }
}