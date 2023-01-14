package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ValueNode
import me.anno.io.base.BaseWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelList
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style

class MathL3Node() : ValueNode("Integer Math 3", inputs, outputs), EnumNode {

    enum class IntMathsTernary(
        val id: Int,
        val glsl: String,
        val int: (a: Int, b: Int, c: Int) -> Int,
        val long: (a: Long, b: Long, c: Long) -> Long
    ) {

        CLAMP(0, "clamp(a,b,c)",
            { v, min, max -> me.anno.maths.Maths.clamp(v, min, max) },
            { v, min, max -> me.anno.maths.Maths.clamp(v, min, max) }),
        MEDIAN(1, "max(min(a,b),min(max(a,b),c))",
            { a, b, c -> me.anno.maths.Maths.median(a, b, c) },
            { a, b, c -> me.anno.maths.Maths.median(a, b, c) }),
        ADD(10, "a+b+c", { a, b, c -> a + b + c }, { a, b, c -> a + b + c }),
        MUL(12, "a*b*c", { a, b, c -> a * b * c }, { a, b, c -> a * b * c }),
        MUL_ADD(13, "a*b+c", { a, b, c -> a * b + c }, { a, b, c -> a * b * c }),

        ;

        companion object {
            val values = values()
            val byId = values.associateBy { it.id }
        }

    }

    constructor(type: IntMathsTernary) : this() {
        this.type = type
    }

    override fun listNodes() = IntMathsTernary.values.map { MathL3Node(it) }

    var type: IntMathsTernary = IntMathsTernary.ADD
        set(value) {
            field = value
            name = "Int " + value.name
        }

    override fun createUI(list: PanelList, style: Style) {
        super.createUI(list, style)
        list += EnumInput(
            "Type", true, type.name,
            IntMathsTernary.values.map { NameDesc(it.name, it.glsl, "") }, style
        ).setChangeListener { _, index, _ ->
            type = IntMathsTernary.values[index]
        }
    }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0]) as Long
        val b = graph.getValue(inputs[1]) as Long
        val c = graph.getValue(inputs[2]) as Long
        setOutput(type.long(a, b, c), 0)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("type", type)
    }

    override fun readInt(name: String, value: Int) {
        if (name == "type") type = IntMathsTernary.byId[value] ?: type
        else super.readInt(name, value)
    }

    override val className get() = "MathL3Node"

    companion object {
        val inputs = listOf("Long", "A", "Long", "B", "Long", "C")
        val outputs = listOf("Long", "Result")
    }

}