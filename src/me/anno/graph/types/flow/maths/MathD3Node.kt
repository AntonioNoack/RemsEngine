package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ValueNode
import me.anno.graph.ui.GraphEditor
import me.anno.io.base.BaseWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelList
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import me.anno.utils.strings.StringHelper.upperSnakeCaseToTitle

class MathD3Node() : ValueNode("", inputs, outputs), EnumNode {

    constructor(type: FloatMathsTernary) : this() {
        this.type = type
    }

    var type: FloatMathsTernary = FloatMathsTernary.ADD
        set(value) {
            field = value
            name = "Float " + value.name.upperSnakeCaseToTitle()
        }

    override fun listNodes() = FloatMathsTernary.values.map { MathD3Node(it) }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0]) as Double
        val b = graph.getValue(inputs[1]) as Double
        val c = graph.getValue(inputs[2]) as Double
        setOutput(type.double(a, b, c), 0)
    }

    override fun createUI(g: GraphEditor, list: PanelList, style: Style) {
        super.createUI(g, list, style)
        list += EnumInput(
            "Type", true, type.name.upperSnakeCaseToTitle(),
            FloatMathsTernary.values.map { NameDesc(it.name.upperSnakeCaseToTitle(), it.glsl, "") }, style
        ).setChangeListener { _, index, _ ->
            type = FloatMathsTernary.values[index]
            g.onChange(false)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("type", type)
    }

    override fun readInt(name: String, value: Int) {
        if (name == "type") type = FloatMathsTernary.byId[value] ?: type
        else super.readInt(name, value)
    }

    override val className get() = "MathD3Node"

    companion object {
        val inputs = listOf("Double", "A", "Double", "B", "Double", "C")
        val outputs = listOf("Double", "Result")
    }

}