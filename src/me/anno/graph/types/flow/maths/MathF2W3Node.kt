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
import org.joml.Vector3f

class MathF2W3Node() : ValueNode("", inputs, outputs), EnumNode, GLSLExprNode {

    constructor(type: VectorMathsBinary) : this() {
        this.type = type
    }

    var type: VectorMathsBinary = VectorMathsBinary.CROSS
        set(value) {
            field = value
            name = "Vector3f " + value.name.upperSnakeCaseToTitle()
        }

    override fun getShaderFuncName(outputIndex: Int): String = "f2w3$type"
    override fun defineShaderFunc(outputIndex: Int): String = "(vec3 a, vec3 b){return ${type.glsl};}"

    override fun listNodes() = values.map { MathF2W3Node(it) }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0]) as Vector3f
        val b = graph.getValue(inputs[1]) as Vector3f
        setOutput(type.float(a, b, Vector3f()), 0)
    }

    override fun createUI(g: GraphEditor, list: PanelList, style: Style) {
        super.createUI(g, list, style)
        list += EnumInput(
            "Type", true, type.name.upperSnakeCaseToTitle(),
            values.map { NameDesc(it.name.upperSnakeCaseToTitle(), it.glsl, "") }, style
        ).setChangeListener { _, index, _ ->
            type = values[index]
            g.onChange(false)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("type", type)
    }

    override fun readInt(name: String, value: Int) {
        if (name == "type") type = VectorMathsBinary.byId[value] ?: type
        else super.readInt(name, value)
    }

    override val className get() = "MathF2W3Node"

    companion object {

        val values = VectorMathsBinary.values

        @JvmField
        val inputs = listOf("Vector3f", "A", "Vector3f", "B")

        @JvmField
        val outputs = listOf("Vector3f", "Result")
    }

}