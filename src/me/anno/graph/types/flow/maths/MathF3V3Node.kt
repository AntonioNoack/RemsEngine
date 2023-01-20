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

class MathF3V3Node() : ValueNode("FPV3 Math 3", inputs, outputs), EnumNode, GLSLExprNode {

    constructor(type: FloatMathsTernary) : this() {
        this.type = type
    }

    var type: FloatMathsTernary = FloatMathsTernary.ADD
        set(value) {
            field = value
            name = "Vector3f " + value.name
        }

    override fun getShaderFuncName(outputIndex: Int): String = "f3v3$type"
    override fun defineShaderFunc(outputIndex: Int): String = "(vec3 a, vec3 b, vec3 c){return ${type.glsl};}"

    override fun listNodes() = FloatMathsTernary.values.map { MathF3V3Node(it) }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0]) as Vector3f
        val b = graph.getValue(inputs[1]) as Vector3f
        val c = graph.getValue(inputs[2]) as Vector3f
        setOutput(Vector3f(type.float(a.x, b.x, c.x), type.float(a.y, b.y, c.y), type.float(a.z, b.z, c.z)), 0)
    }

    override fun createUI(g: GraphEditor, list: PanelList, style: Style) {
        super.createUI(g, list, style)
        list += EnumInput(
            "Type", true, type.name,
            FloatMathsTernary.values.map { NameDesc(it.name, it.glsl, "") }, style
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

    override val className get() = "MathF3V3Node"

    companion object {
        val inputs = listOf("Vector3f", "A", "Vector3f", "B", "Vector3f", "C")
        val outputs = listOf("Vector3f", "Result")
    }

}