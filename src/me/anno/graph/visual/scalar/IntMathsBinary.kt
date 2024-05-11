package me.anno.graph.visual.scalar

import me.anno.maths.Maths.length
import me.anno.maths.Maths.min
import me.anno.utils.types.Booleans.toLong
import kotlin.math.abs
import kotlin.math.max

enum class IntMathsBinary(
    val id: Int,
    val glsl: String
) {

    ADD(10, "a+b"),
    SUB(11, "a-b"),
    MUL(12, "a*b"),
    DIV(13, "a/b"),
    MOD(14, "mod(a,b)"), // correct?

    LSL(20, "a<<b"),
    LSR(21, "a>>>b"),
    SHR(22, "a>>b"),

    AND(30, "a&b"),
    OR(31, "a|b"),
    XOR(32, "a^b"),
    NOR(33, "~(a|b)"),
    XNOR(34, "~(a^b)"),
    NAND(35, "~(a&b)"),

    LENGTH_SQUARED(40, "a*a+b*b"),
    ABS_DELTA(41, "abs(a-b)"),
    NORM1(42, "abs(a)+abs(b)"),
    AVG(43, "((a+b)>>1)"),
    LENGTH(44, "int(sqrt(a*a+b*b))"),
    // POW(45,"int(pow(a,b))",{ a, b -> kotlin.math.pow(a.toDouble(), b.toDouble()).toInt() }, { a, b -> pow(a, b) }),
    // ROOT(46,"int(pow(a,1.0/b))",{ a, b -> me.anno.maths.Maths.pow(a.toDouble(), 1.0 / b) }, { a, b -> pow(a, 1 / b) }),

    // GEO_MEAN({ a, b -> kotlin.math.sqrt(a * b) }, { a, b -> kotlin.math.sqrt(a * b) }),
    MIN(50, "min(a,b)"),
    MAX(51, "max(a,b)"),

    // Kronecker delta
    EQUALS(60, "a==b?1:0"),

    ;

    fun calculate(a: Long, b: Long): Long {
        return when (this) {
            ADD -> a + b
            SUB -> a - b
            MUL -> a * b
            DIV -> if (b == 0L) 0L else a / b
            MOD -> if (b == 0L) 0L else a % b
            LSL -> a shl b.toInt()
            LSR -> a ushr b.toInt()
            SHR -> a shr b.toInt()
            AND -> a and b
            OR -> a or b
            XOR -> a xor b
            NOR -> (a or b).inv()
            XNOR -> (a xor b).inv()
            NAND -> (a and b).inv()
            LENGTH_SQUARED -> a * a + b * b
            ABS_DELTA -> abs(a - b)
            NORM1 -> abs(a) + abs(b)
            AVG -> (a + b) shr 1
            LENGTH -> length(a.toDouble(), b.toDouble()).toLong()
            MIN -> min(a, b)
            MAX -> max(a, b)
            EQUALS -> (a == b).toLong()
        }
    }
}