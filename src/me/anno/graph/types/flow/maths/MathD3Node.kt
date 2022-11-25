package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ValueNode
import me.anno.io.base.BaseWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelList
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style

class MathD3Node() : ValueNode("FP Math 3", inputs, outputs), EnumNode {

    enum class FloatMathsTernary(
        val id: Int,
        val glsl: String,
        val float: (a: Float, b: Float, c: Float) -> Float,
        val double: (a: Double, b: Double, c: Double) -> Double
    ) {

        CLAMP(0, "clamp(a,b,c)",
            { v, min, max -> me.anno.maths.Maths.clamp(v, min, max) },
            { v, min, max -> me.anno.maths.Maths.clamp(v, min, max) }),
        MEDIAN(1, "max(min(a,b),min(max(a,b),c))",
            { a, b, c -> me.anno.maths.Maths.median(a, b, c) },
            { a, b, c -> me.anno.maths.Maths.median(a, b, c) }),

        MIX(2, "mix(a,b,c)",
            { a, b, c -> me.anno.maths.Maths.mix(a, b, c) },
            { a, b, c -> me.anno.maths.Maths.mix(a, b, c) }),
        UNMIX(
            3, "(a-b)/(c-b)",
            { a, b, c -> me.anno.maths.Maths.unmix(a, b, c) },
            { a, b, c -> me.anno.maths.Maths.unmix(a, b, c) }),
        MIX_CLAMPED(4, "mix(a,b,clamp(c,0.0,1.0))",
            { a, b, c -> me.anno.maths.Maths.mix(a, b, me.anno.maths.Maths.clamp(c)) },
            { a, b, c -> me.anno.maths.Maths.mix(a, b, me.anno.maths.Maths.clamp(c)) }),
        UNMIX_CLAMPED(
            5, "clamp((a-b)/(c-b),0.0,1.0)",
            { a, b, c -> me.anno.maths.Maths.clamp(me.anno.maths.Maths.unmix(a, b, c)) },
            { a, b, c -> me.anno.maths.Maths.clamp(me.anno.maths.Maths.unmix(a, b, c)) }),


        ADD(10, "a+b+c", { a, b, c -> a + b + c }, { a, b, c -> a + b + c }),
        MUL(12, "a*b*c", { a, b, c -> a * b * c }, { a, b, c -> a * b * c }),
        MUL_ADD(13, "a*b+c", { a, b, c -> a * b + c }, { a, b, c -> a * b * c }),

        ;

        companion object {
            @JvmStatic
            val values = values()
            @JvmStatic
            val byId = values.associateBy { it.id }
        }

    }

    var type: FloatMathsTernary = FloatMathsTernary.ADD

    constructor(type: FloatMathsTernary) : this() {
        this.type = type
        this.name = "Float " + type.name
    }

    override fun listNodes() = FloatMathsTernary.values.map { MathD3Node(it) }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0]) as Double
        val b = graph.getValue(inputs[1]) as Double
        val c = graph.getValue(inputs[2]) as Double
        setOutput(type.double(a, b, c), 0)
    }

    override fun createUI(list: PanelList, style: Style) {
        super.createUI(list, style)
        list += EnumInput(
            "Type", true, type.name,
            FloatMathsTernary.values.map { NameDesc(it.name, it.glsl, "") }, style
        ).setChangeListener { _, index, _ ->
            type = FloatMathsTernary.values[index]
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

    override val className = "MathD3Node"

    companion object {
        val inputs = listOf("Double", "A", "Double", "B", "Double", "C")
        val outputs = listOf("Double", "Result")
    }

}