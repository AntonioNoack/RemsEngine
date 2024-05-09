package me.anno.graph.visual.local

import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLExprNode
import me.anno.graph.visual.render.compiler.GLSLFlowNode
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.io.base.BaseWriter

class SetLocalVariableNode(type: String = "?") :
    ActionNode(
        "SetLocal",
        listOf("String", "Name", type, "New Value"),
        listOf(type, "Current Value")
    ), GLSLFlowNode, GLSLExprNode {

    var type: String = type
        set(value) {
            field = value
            inputs[2].type = value
            outputs[1].type = value
            name = if (value == "?") "SetLocal"
            else "SetLocal $value"
        }

    init {
        if (type != "?") name = "SetLocal $type"
        inputs[2].isCustom = true // for type
        outputs[1].isCustom = true
    }

    constructor(key: String, value: Any?) : this() {
        setInputs(listOf(null, key, value))
    }

    val key get() = getInput(1) as String
    val value get() = getInput(2)

    override fun executeAction() {
        val key = getInput(1) as String
        val value = getInput(2)
        val graph = graph as FlowGraph
        graph.localVariables[key] = value
        setOutput(1, value)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("type", type)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "type" -> type = value as? String ?: return
            else -> super.setProperty(name, value)
        }
    }

    override fun buildCode(g: GraphCompiler, depth: Int): Boolean {
        if (type != "?") {
            g.builder.append(g.getLocalVarName(key, type)).append("=")
            g.expr(inputs[2])
            g.builder.append(";\n")
        }
        // continue
        return g.buildCode(getOutputNode(0), depth)
    }

    override fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node) {
        g.builder.append(g.getLocalVarName(key, type))
    }

    companion object {
        val inputs = listOf("String", "Name", "?", "New Value")
        val outputs = listOf("?", "Current Value")
    }
}