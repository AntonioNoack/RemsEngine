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
import me.anno.utils.types.Floats.toDegrees
import me.anno.utils.types.Floats.toRadians
import kotlin.math.pow

class MathD1Node() : ValueNode("", inputs, outputs), EnumNode {

    constructor(type: FloatMathsUnary) : this() {
        this.type = type
    }

    var type: FloatMathsUnary = FloatMathsUnary.ABS
        set(value) {
            field = value
            name = "Float " + value.name.upperSnakeCaseToTitle()
        }

    override fun listNodes() = FloatMathsUnary.values.map { MathD1Node(it) }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0]) as Double
        setOutput(type.double(a), 0)
    }

    override fun createUI(g: GraphEditor, list: PanelList, style: Style) {
        super.createUI(g,list, style)
        list += EnumInput(
            "Type", true, type.name.upperSnakeCaseToTitle(),
            FloatMathsUnary.values.map { NameDesc(it.name.upperSnakeCaseToTitle(), it.glsl, "") }, style
        ).setChangeListener { _, index, _ ->
            type = FloatMathsUnary.values[index]
            g.onChange(false)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("type", type)
    }

    override fun readInt(name: String, value: Int) {
        if (name == "type") type = FloatMathsUnary.byId[value] ?: type
        else super.readInt(name, value)
    }

    override val className get() = "MathD1Node"

    companion object {
        val inputs = listOf("Double", "A")
        val outputs = listOf("Double", "Result")
    }

}