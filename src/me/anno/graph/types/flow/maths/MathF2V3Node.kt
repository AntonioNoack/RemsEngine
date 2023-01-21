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

class MathF2V3Node() : ValueNode("", inputs, outputs), EnumNode, GLSLExprNode {

    constructor(type: FloatMathsBinary) : this() {
        this.type = type
    }

    var type: FloatMathsBinary = FloatMathsBinary.ADD
        set(value) {
            field = value
            name = "Vector3f " + value.name.upperSnakeCaseToTitle()
        }

    override fun getShaderFuncName(outputIndex: Int): String = "f2v3$type"
    override fun defineShaderFunc(outputIndex: Int): String = "(vec3 a, vec3 b){return ${type.glsl};}"

    override fun listNodes() = values.map { MathF2V3Node(it) }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0]) as Vector3f
        val b = graph.getValue(inputs[1]) as Vector3f
        setOutput(Vector3f(type.float(a.x, b.x), type.float(a.y, b.y), type.float(a.z, b.z)), 0)
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
        if (name == "type") type = FloatMathsBinary.byId[value] ?: type
        else super.readInt(name, value)
    }

    override val className get() = "MathF2V3Node"

    companion object {

        val values = FloatMathsBinary.values
            .filter { "vec2" !in it.glsl }

        @JvmField
        val inputs = listOf("Vector3f", "A", "Vector3f", "B")

        @JvmField
        val outputs = listOf("Vector3f", "Result")
    }

}