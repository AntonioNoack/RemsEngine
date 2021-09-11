package me.anno.graph.types.flow.maths

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ValueNode
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style

class MathD2Node() : ValueNode(listOf("Double", "Double"), listOf("Double")) {

    constructor(type: FloatMathsBinary) : this() {
        this.type = type
    }

    var type: FloatMathsBinary = FloatMathsBinary.ADD

    override fun createUI(list: PanelListY, style: Style) {
        list += EnumInput("Type", true, type.name, FloatMathsBinary.values().map { NameDesc(it.name) }, style)
            .setChangeListener { _, index, _ ->
                type = FloatMathsBinary.values()[index]
            }
    }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0]) as Double
        val b = graph.getValue(inputs[1]) as Double
        val c = type.double(a, b)
        // logger.info("$a $type $b = $c")
        setOutput(c, 0)
    }

}