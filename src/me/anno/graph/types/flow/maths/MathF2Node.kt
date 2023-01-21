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

class MathF2Node() : ValueNode("", inputs, outputs), EnumNode, GLSLExprNode {

    constructor(type: FloatMathsBinary) : this() {
        this.type = type
    }

    var type: FloatMathsBinary = FloatMathsBinary.ADD
        set(value) {
            field = value
            name = "Float " + value.name.upperSnakeCaseToTitle()
        }

    override fun getShaderFuncName(outputIndex: Int): String = "f2$type"
    override fun defineShaderFunc(outputIndex: Int): String = "(float a, float b){return ${type.glsl};}"

    override fun listNodes() = FloatMathsBinary.values.map { MathF2Node(it) }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0]) as Float
        val b = graph.getValue(inputs[1]) as Float
        setOutput(type.float(a, b), 0)
    }

    override fun createUI(g: GraphEditor, list: PanelList, style: Style) {
        super.createUI(g, list, style)
        list += EnumInput(
            "Type", true, type.name.upperSnakeCaseToTitle(),
            FloatMathsBinary.values.map { NameDesc(it.name.upperSnakeCaseToTitle(), it.glsl, "") }, style
        ).setChangeListener { _, index, _ ->
            type = FloatMathsBinary.values[index]
            g.onChange(false)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("type", type)
    }

    override fun readInt(name: String, value: Int) {
        if (name == "type") type = FloatMathsBinary.byId[value] ?: type
        else super.readInt(name, value)
    }

    override val className get() = "MathF2Node"

    companion object {
        @JvmField
        val inputs = listOf("Float", "A", "Float", "B")

        @JvmField
        val outputs = listOf("Float", "Result")
    }

}