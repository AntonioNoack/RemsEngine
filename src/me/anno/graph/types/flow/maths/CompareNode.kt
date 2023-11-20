package me.anno.graph.types.flow.maths

import me.anno.graph.types.flow.ComputeNode
import me.anno.graph.ui.GraphEditor
import me.anno.graph.ui.GraphPanel
import me.anno.io.base.BaseWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.EnumInput
import me.anno.ui.Style

class CompareNode(type: String = "?") :
    ComputeNode("Compare", listOf(type, "A", type, "B"), outputs) {

    // todo long & double & bool value node as inputs for some nodes

    var compType = Mode.LESS_THAN

    var type: String = type
        set(value) {
            field = value
            inputs!![0].type = value
            inputs!![1].type = value
            name = if (value == "?") "Compare"
            else "Compare $value"
        }

    init {
        if (type != "?") name = "Compare $type"
    }

    enum class Mode(val id: Int, val niceName: String, val glslName: String) {
        LESS_THAN(0, "<", "<"),
        LESS_OR_EQUALS(1, "<=", "<="),
        EQUALS(2, "==", "=="),
        MORE_THAN(3, ">", ">"),
        MORE_OR_EQUALS(4, ">=", ">="),
        NOT_EQUALS(5, "!=", "!="),
        IDENTICAL(6, "===", "=="),
        NOT_IDENTICAL(7, "!==", "!=")
    }

    fun apply(delta: Int): Boolean {
        return when (compType) {
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

    override fun compute() {
        val inputs = inputs!!
        val a = inputs[0].getValue()
        val b = inputs[1].getValue()
        val c = when (compType) {
            Mode.IDENTICAL -> a === b
            Mode.NOT_IDENTICAL -> a !== b
            else -> apply(compare(a, b))
        }
        setOutput(0, c)
    }

    override fun createUI(g: GraphPanel, list: PanelList, style: Style) {
        if (g is GraphEditor) {
            list += EnumInput(
                "Type", true, compType.name,
                values2.map { NameDesc(it.niceName) }, style
            ).setChangeListener { _, index, _ ->
                compType = values2[index]
                g.onChange(false)
            }
        } else list.add(TextPanel("Type: ${compType.name}", style))
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("type", type)
        writer.writeEnum("type2", compType)
    }

    override fun readString(name: String, value: String) {
        if (name == "type") type = value
        else super.readString(name, value)
    }

    override fun readInt(name: String, value: Int) {
        if (name == "type2") compType = values2.getOrNull(value) ?: compType
        else super.readInt(name, value)
    }

    override val className: String get() = "CompareNode"

    companion object {
        val values2 = Mode.values()
        val outputs = listOf("Boolean", "Result")
    }
}