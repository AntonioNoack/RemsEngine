package me.anno.graph.visual.local

import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLExprNode
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.io.base.BaseWriter

class GetLocalVariableNode(type: String = "?") :
    CalculationNode("", inputs, listOf(type, "Value")), GLSLExprNode {

    constructor(key: String, type: String) : this(type) {
        setInput(0, key)
    }

    var type: String = type
        set(value) {
            field = value
            outputs[0].type = value
            name = createName(type)
        }

    init {
        name = createName(type)
        outputs[0].isCustom = true // for type
    }

    private fun createName(type: String): String {
        return if (type == "?") "GetLocal"
        else "GetLocal $type"
    }

    val key get() = getInput(0) as String
    val value get() = (graph as FlowGraph).localVariables[key]

    override fun calculate(): Any? {
        val key = getInput(0)
        val graph = graph as FlowGraph
        return graph.localVariables[key]
    }

    override fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node) {
        g.builder.append(g.getLocalVarName(key, type))
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("type", type)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "type" -> {
                type = value as? String ?: return
                this.name = createName(type)
            }
            else -> super.setProperty(name, value)
        }
    }

    companion object {
        val inputs = listOf("String", "Name")
    }
}