package me.anno.graph.types.flow.maths

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ValueNode
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style

class MathL2Node() : ValueNode("Math", listOf("Long", "Long"), listOf("Long")) {

    constructor(type: IntMathType) : this() {
        this.type = type
    }

    var type: IntMathType = IntMathType.ADD

    override fun createUI(list: PanelListY, style: Style) {
        list += EnumInput("Type", true, type.name, IntMathType.values().map { NameDesc(it.name) }, style)
            .setChangeListener { _, index, _ ->
                type = IntMathType.values()[index]
            }
    }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0]) as Long
        val b = graph.getValue(inputs[1]) as Long
        val c = type.long(a, b)
        // LOGGER.info("$a $type $b = $c")
        setOutput(c, 0)
    }

}