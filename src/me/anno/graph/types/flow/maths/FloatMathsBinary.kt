package me.anno.graph.types.flow.maths

import me.anno.maths.Maths
import kotlin.math.pow

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

    ATAN2(40, "atan(a,b)", { a, b -> kotlin.math.atan2(a, b) }, { a, b -> kotlin.math.atan2(a, b) }),

    ;

}