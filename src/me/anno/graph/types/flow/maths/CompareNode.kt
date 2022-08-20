package me.anno.graph.types.flow.maths

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ValueNode
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style

class CompareNode : ValueNode("Compare", inputs, outputs) {

    // todo long & double compare node for ease of use
    // todo long & double & bool value node as inputs for some nodes

    var type = Mode.LESS_THAN

    enum class Mode {
        LESS_THAN,
        LESS_OR_EQUALS,
        EQUALS,
        MORE_THAN,
        MORE_OR_EQUALS,
        NOT_EQUALS
    }

    fun apply(mode: Mode, delta: Int): Boolean {
        return when (mode) {
            Mode.LESS_THAN -> delta < 0
            Mode.LESS_OR_EQUALS -> delta <= 0
            Mode.EQUALS -> delta == 0
            Mode.MORE_THAN -> delta > 0
            Mode.MORE_OR_EQUALS -> delta >= 0
            Mode.NOT_EQUALS -> delta != 0
        }
    }

    fun compare(a: Any?, b: Any?): Int {
        if (a == b) return 0
        if (a is Comparable<*>) {
            try {
                a as Comparable<Any?>
                return a.compareTo(b)
            } catch (e: Exception) {
            }
        }
        val ha = System.identityHashCode(a)
        val hb = System.identityHashCode(b)
        if (ha == hb) return -1
        return ha.compareTo(hb)
    }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0])
        val b = graph.getValue(inputs[1])
        val c = apply(type, compare(a, b))
        setOutput(c, 0)
    }

    override fun createUI(list: PanelListY, style: Style) {
        super.createUI(list, style)
        list += EnumInput("Type", true, type.name, values2.map { NameDesc(it.name) }, style)
            .setChangeListener { _, index, _ ->
                type = values2[index]
            }
    }

    override val className = "CompareNode"

    companion object {
        val values2 = Mode.values()
        val inputs = listOf("?", "A", "?", "B")
        val outputs = listOf("Boolean", "Result")
    }

}