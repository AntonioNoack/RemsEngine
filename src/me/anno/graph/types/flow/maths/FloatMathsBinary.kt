package me.anno.graph.types.flow.maths

import me.anno.maths.Maths

enum class FloatMathsBinary(
    val glsl: String,
    val float: (a: Float, b: Float) -> Float,
    val double: (a: Double, b: Double) -> Double
) {

    ADD("a+b", { a, b -> a + b }, { a, b -> a + b }),
    SUB("a-b", { a, b -> a - b }, { a, b -> a - b }),
    MUL("a*b", { a, b -> a * b }, { a, b -> a * b }),
    DIV("a/b", { a, b -> a / b }, { a, b -> a / b }),
    MOD("a%b", { a, b -> a % b }, { a, b -> a % b }),
    POW("pow(a,b)", { a, b -> Maths.pow(a, b) }, { a, b -> StrictMath.pow(a, b) }),
    ROOT("pow(a,1/b)", { a, b -> Maths.pow(a, 1 / b) }, { a, b -> StrictMath.pow(a, 1 / b) }),
    LENGTH("length(vec2(a,b))",
        { a, b -> kotlin.math.sqrt(a * a + b * b) }, { a, b -> kotlin.math.sqrt(a * a + b * b) }),
    LENGTH_SQUARED("dot(vec2(a,b),vec2(a,b))", { a, b -> a * a + b * b }, { a, b -> a * a + b * b }),
    ABS_DELTA("abs(a-b)", { a, b -> kotlin.math.abs(a - b) }, { a, b -> kotlin.math.abs(a - b) }),
    NORM1("abs(a)+abs(b)",
        { a, b -> kotlin.math.abs(a) + kotlin.math.abs(b) }, { a, b -> kotlin.math.abs(a) + kotlin.math.abs(b) }),
    AVG("(a+b)*0.5", { a, b -> (a + b) * 0.5f }, { a, b -> (a + b) * 0.5 }),
    GEO_MEAN("sqrt(a*b)", { a, b -> kotlin.math.sqrt(a * b) }, { a, b -> kotlin.math.sqrt(a * b) }),
    MIN("min(a,b)", { a, b -> kotlin.math.min(a, b) }, { a, b -> kotlin.math.min(a, b) }),
    MAX("max(a,b)", { a, b -> kotlin.math.max(a, b) }, { a, b -> kotlin.math.max(a, b) }),

}