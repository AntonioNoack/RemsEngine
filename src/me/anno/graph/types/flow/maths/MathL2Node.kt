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

class MathL2Node() : ValueNode("", inputs, outputs), EnumNode {

    enum class IntMathsBinary(
        val id: Int,
        val glsl: String,
        val int: (a: Int, b: Int) -> Int,
        val long: (a: Long, b: Long) -> Long
    ) {

        ADD(10, "a+b", { a, b -> a + b }, { a, b -> a + b }),
        SUB(11, "a-b", { a, b -> a - b }, { a, b -> a - b }),
        MUL(12, "a*b", { a, b -> a * b }, { a, b -> a * b }),
        DIV(13, "a/b", { a, b -> a / b }, { a, b -> a / b }),
        MOD(14, "a%b", { a, b -> a % b }, { a, b -> a % b }),

        LSL(20, "a<<b", { a, b -> a shl b }, { a, b -> a shl b.toInt() }),
        LSR(21, "a>>>b", { a, b -> a ushr b }, { a, b -> a ushr b.toInt() }),
        SHR(22, "a>>b", { a, b -> a shr b }, { a, b -> a shr b.toInt() }),

        AND(30, "a&b", { a, b -> a and b }, { a, b -> a and b }),
        OR(31, "a|b", { a, b -> a or b }, { a, b -> a or b }),
        XOR(32, "a^b", { a, b -> a xor b }, { a, b -> a xor b }),
        NOR(33, "~(a|b)", { a, b -> (a or b).inv() }, { a, b -> (a or b).inv() }),
        XNOR(34, "~(a^b)", { a, b -> (a xor b).inv() }, { a, b -> (a xor b).inv() }),
        NAND(35, "~(a&b)", { a, b -> (a and b).inv() }, { a, b -> (a and b).inv() }),

        LENGTH_SQUARED(40, "a*a+b*b", { a, b -> a * a + b * b }, { a, b -> a * a + b * b }),
        ABS_DELTA(41, "abs(a-b)", { a, b -> kotlin.math.abs(a - b) }, { a, b -> kotlin.math.abs(a - b) }),
        NORM1(
            42,
            "abs(a)+abs(b)",
            { a, b -> kotlin.math.abs(a) + kotlin.math.abs(b) },
            { a, b -> kotlin.math.abs(a) + kotlin.math.abs(b) }),
        AVG(43, "((a+b)>>1)", { a, b -> (a + b) shr 1 }, { a, b -> (a + b) shr 1 }),
        LENGTH(
            44, "int(sqrt(a*a+b*b))",
            { a, b -> kotlin.math.sqrt((a * a.toLong() + b * b.toLong()).toDouble()).toInt() },
            { a, b -> kotlin.math.sqrt((a * a + b * b).toDouble()).toLong() }),
        // POW(45,"int(pow(a,b))",{ a, b -> kotlin.math.pow(a.toDouble(), b.toDouble()).toInt() }, { a, b -> pow(a, b) }),
        // ROOT(46,"int(pow(a,1.0/b))",{ a, b -> me.anno.maths.Maths.pow(a.toDouble(), 1.0 / b) }, { a, b -> pow(a, 1 / b) }),

        // GEO_MEAN({ a, b -> kotlin.math.sqrt(a * b) }, { a, b -> kotlin.math.sqrt(a * b) }),
        MIN(50, "min(a,b)", { a, b -> kotlin.math.min(a, b) }, { a, b -> kotlin.math.min(a, b) }),
        MAX(51, "max(a,b)", { a, b -> kotlin.math.max(a, b) }, { a, b -> kotlin.math.max(a, b) }),

        // Kronecker delta
        EQUALS(60, "a==b?1:0", { a, b -> if (a == b) 1 else 0 }, { a, b -> if (a == b) 1 else 0 }),

        ;

        companion object {
            val values = values()
            val byId = values.associateBy { it.id }
        }

    }

    constructor(type: IntMathsBinary) : this() {
        this.type = type
    }

    override fun listNodes() = IntMathsBinary.values.map { MathL2Node(it) }

    var type: IntMathsBinary = IntMathsBinary.ADD
        set(value) {
            field = value
            name = "Int " + value.name.upperSnakeCaseToTitle()
        }

    override fun createUI(g: GraphEditor, list: PanelList, style: Style) {
        super.createUI(g, list, style)
        list += EnumInput(
            "Type", true, type.name.upperSnakeCaseToTitle(),
            IntMathsBinary.values.map { NameDesc(it.name.upperSnakeCaseToTitle(), it.glsl, "") }, style
        ).setChangeListener { _, index, _ ->
            type = IntMathsBinary.values[index]
            g.onChange(false)
        }
    }

    override fun compute(graph: FlowGraph) {
        val inputs = inputs!!
        val a = graph.getValue(inputs[0]) as Long
        val b = graph.getValue(inputs[1]) as Long
        setOutput(type.long(a, b), 0)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("type", type)
    }

    override fun readInt(name: String, value: Int) {
        if (name == "type") type = IntMathsBinary.byId[value] ?: type
        else super.readInt(name, value)
    }

    override val className get() = "MathL2Node"

    companion object {
        val inputs = listOf("Long", "A", "Long", "B")
        val outputs = listOf("Long", "Result")
    }

}