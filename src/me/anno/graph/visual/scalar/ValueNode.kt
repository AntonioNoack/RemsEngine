package me.anno.graph.visual.scalar

import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLExprNode
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.io.base.BaseWriter

class ValueNode private constructor(type: String, inputs: List<String>, outputs: List<String>) :
    CalculationNode(type, inputs, outputs), GLSLExprNode {

    private constructor(type: String, list: List<String>) :
            this(type, list, list)

    @Suppress("unused")
    constructor() : this("?") // for registry
    constructor(type: String) : this(type, listOf(type, "Value"))

    var type: String = type
        set(value) {
            field = value
            inputs[0].type = value
            outputs[0].type = value
            name = value
        }

    override fun calculate() = getInput(0)

    override fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node) {
        g.expr(n.inputs[0])
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
}