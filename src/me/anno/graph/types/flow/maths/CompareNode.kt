package me.anno.graph.types.flow.maths

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ValueNode
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelList
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style

class CompareNode : ValueNode("Compare", inputs, outputs) {

    // todo long & double & bool value node as inputs for some nodes

    var type = Mode.LESS_THAN

    enum class Mode(val id: Int, val niceName: String) {
        LESS_THAN(0, "<"),
        LESS_OR_EQUALS(1, "<="),
        EQUALS(2, "=="),
        MORE_THAN(3, ">"),
        MORE_OR_EQUALS(4, ">="),
        NOT_EQUALS(5, "!="),
        IDENTICAL(6, "==="),
        NOT_IDENTICAL(7, "!==")
    }

    fun apply(delta: Int): Boolean {
        return when (type) {
            Mode.LESS_THAN -> delta < 0
            Mode.LESS_OR_EQUALS -> delta <= 0
            Mode.EQUALS -> delta == 0
            Mode.MORE_THAN -> delta > 0
            Mode.MORE_OR_EQUALS -> delta >= 0
            Mode.NOT_EQUALS -> delta != 0
            else -> false
        }
    }

    fun compare(a: Any?, b: Any?): Int {
        if (a == b) return 0
        if (a == null) return -compare(b, null)
        if (a is Comparable<*>) {
            try {
                a as Comparable<Any?>
                return a.compareTo(b)
            } catch (ignored: Exception) {
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
        val c = when (type) {
            Mode.IDENTICAL -> a === b
            Mode.NOT_IDENTICAL -> a !== b
            else -> apply(compare(a, b))
        }
        setOutput(c, 0)
    }

    override fun createUI(list: PanelList, style: Style) {
        super.createUI(list, style)
        list += EnumInput("Type", true, type.name, values2.map { NameDesc(it.niceName) }, style)
            .setChangeListener { _, index, _ ->
                type = values2[index]
            }
    }

    override val className get() = "CompareNode"

    companion object {
        val values2 = Mode.values()
        val inputs = listOf("?", "A", "?", "B")
        val outputs = listOf("Boolean", "Result")
    }

}