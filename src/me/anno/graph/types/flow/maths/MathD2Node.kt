package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ValueNode
import me.anno.io.base.BaseWriter
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths
import me.anno.ui.base.groups.PanelList
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import kotlin.math.pow

class MathD2Node() : ValueNode("FP Math 2", inputs, outputs), EnumNode {

    enum class FloatMathsBinary(
        val id: Int,
        val glsl: String,
        val float: (a: Float, b: Float) -> Float,
        val double: (a: Double, b: Double) -> Double
    ) {

        ADD(0, "a+b", { a, b -> a + b }, { a, b -> a + b }),
        SUB(1, "a-b", { a, b -> a - b }, { a, b -> a - b }),
        MUL(2, "a*b", { a, b -> a * b }, { a, b -> a * b }),
        DIV(3, "a/b", { a, b -> a / b }, { a, b -> a / b }),
        MOD(4, "a%b", { a, b -> a % b }, { a, b -> a % b }),
        POW(5, "pow(a,b)", { a, b -> Maths.pow(a, b) }, { a, b -> a.pow(b) }),
        ROOT(6, "pow(a,1.0/b)", { a, b -> Maths.pow(a, 1 / b) }, { a, b -> a.pow(1f / b) }),

        LENGTH(10, "length(vec2(a,b))",
            { a, b -> kotlin.math.hypot(a, b) }, { a, b -> kotlin.math.hypot(a, b) }),
        LENGTH_SQUARED(11, "dot(vec2(a,b),vec2(a,b))", { a, b -> a * a + b * b }, { a, b -> a * a + b * b }),
        ABS_DELTA(12, "abs(a-b)", { a, b -> kotlin.math.abs(a - b) }, { a, b -> kotlin.math.abs(a - b) }),
        NORM1(13, "abs(a)+abs(b)",
            { a, b -> kotlin.math.abs(a) + kotlin.math.abs(b) }, { a, b -> kotlin.math.abs(a) + kotlin.math.abs(b) }),

        AVG(20, "(a+b)*0.5", { a, b -> (a + b) * 0.5f }, { a, b -> (a + b) * 0.5 }),
        GEO_MEAN(21, "sqrt(a*b)", { a, b -> kotlin.math.sqrt(a * b) }, { a, b -> kotlin.math.sqrt(a * b) }),
        MIN(22, "min(a,b)", { a, b -> kotlin.math.min(a, b) }, { a, b -> kotlin.math.min(a, b) }),
        MAX(23, "max(a,b)", { a, b -> kotlin.math.max(a, b) }, { a, b -> kotlin.math.max(a, b) }),

        ATAN2(40, "atan2(a,b)", { a, b -> kotlin.math.atan2(a, b) }, { a, b -> kotlin.math.atan2(a, b) }),

        ;

        companion object {
            val values = values()
            val byId = values.associateBy { it.id }
        }

    }

    var type: FloatMathsBinary = FloatMathsBinary.ADD

    constructor(type: FloatMathsBinary) : this() {
        this.type = type
        this.name = "Float " + type.name
    }

    override fun listNodes() = FloatMathsBinary.values.map { MathD2Node(it) }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0]) as Double
        val b = graph.getValue(inputs[1]) as Double
        setOutput(type.double(a, b), 0)
    }

    override fun createUI(list: PanelList, style: Style) {
        super.createUI(list, style)
        list += EnumInput(
            "Type", true, type.name,
            FloatMathsBinary.values.map { NameDesc(it.name, it.glsl, "") }, style
        ).setChangeListener { _, index, _ ->
            type = FloatMathsBinary.values[index]
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

    override val className get() = "MathD2Node"

    companion object {
        @JvmField
        val inputs = listOf("Double", "A", "Double", "B")
        @JvmField
        val outputs = listOf("Double", "Result")
    }

}