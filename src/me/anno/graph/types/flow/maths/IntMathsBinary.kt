package me.anno.graph.types.flow.maths

enum class IntMathsBinary(
    val id: Int,
    val glsl: String,
    val int: (a: Int, b: Int) -> Int,
    val long: (a: Long, b: Long) -> Long
) {

    ADD(10, "a+b", { a, b -> a + b }, { a, b -> a + b }),
    SUB(11, "a-b", { a, b -> a - b }, { a, b -> a - b }),
    MUL(12, "a*b", { a, b -> a * b }, { a, b -> a * b }),
    DIV(13, "a/b", { a, b -> if (b == 0) 0 else a / b }, { a, b -> if (b == 0L) 0 else a / b }),
    MOD(14, "a%b", { a, b -> if (b == 0) 0 else a % b }, { a, b -> if (b == 0L) 0 else a % b }),

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

}