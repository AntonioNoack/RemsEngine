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
import org.joml.Vector3f

class MathF1V3Node() : ValueNode("FPV3 Math 1", inputs, outputs), EnumNode, GLSLExprNode {

    constructor(type: FloatMathsUnary) : this() {
        this.type = type
    }

    var type: FloatMathsUnary = FloatMathsUnary.ABS
        set(value) {
            field = value
            name = "Vector3f " + value.name
        }

    override fun getShaderFuncName(outputIndex: Int): String = "f1v3$type"
    override fun defineShaderFunc(outputIndex: Int): String = "(vec3 a){return ${type.glsl};}"

    override fun listNodes() = FloatMathsUnary.values.map { MathF1V3Node(it) }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0]) as Vector3f
        setOutput(Vector3f(type.float(a.x), type.float(a.y), type.float(a.z)), 0)
    }

    override fun createUI(g: GraphEditor, list: PanelList, style: Style) {
        super.createUI(g, list, style)
        list += EnumInput(
            "Type", true, type.name,
            FloatMathsUnary.values.map { NameDesc(it.name, it.glsl, "") }, style
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

    override val className get() = "MathF1V3Node"

    companion object {
        val inputs = listOf("Vector3f", "A")
        val outputs = listOf("Vector3f", "Result")
    }

}