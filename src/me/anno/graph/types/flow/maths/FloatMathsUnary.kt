package me.anno.graph.types.flow.maths

import kotlin.math.pow

enum class FloatMathsUnary(
    val id: Int,
    val glsl: String,
    val float: (a: Float) -> Float,
    val double: (a: Double) -> Double
) {

    ABS(0, "abs(a)", { a -> kotlin.math.abs(a) }, { a -> kotlin.math.abs(a) }),
    FLOOR(1, "floor(a)", { a -> kotlin.math.floor(a) }, { a -> kotlin.math.floor(a) }),
    ROUND(2, "round(a)", { a -> kotlin.math.round(a) }, { a -> kotlin.math.round(a) }),
    CEIL(3, "ceil(a)", { a -> kotlin.math.ceil(a) }, { a -> kotlin.math.ceil(a) }),
    FRACT(4, "fract(a)", { a -> a - kotlin.math.floor(a) }, { a -> a - kotlin.math.floor(a) }),
    NEG(5, "-a", { a -> -a }, { a -> -a }),

    LN(10, "ln(a)", { a -> kotlin.math.ln(a) }, { a -> kotlin.math.ln(a) }),
    LN1P(11, "ln(1.0+a)", { a -> kotlin.math.ln1p(a.toDouble()).toFloat() }, { a -> kotlin.math.ln1p(a) }),
    LOG2(12, "log2(a)", { a -> kotlin.math.log2(a) }, { a -> kotlin.math.log2(a) }),
    LOG10(13, "log10(a)", { a -> kotlin.math.log10(a) }, { a -> kotlin.math.log10(a) }),

    EXP(20, "exp(a)", { a -> kotlin.math.exp(a) }, { a -> kotlin.math.exp(a) }),
    EXPM1(21, "exp(a)-1.0", { a -> kotlin.math.expm1(a) }, { a -> kotlin.math.expm1(a) }),
    EXP2(22, "pow(2.0, a)", { a -> 2f.pow(a) }, { a -> 2.0.pow(a) }),
    EXP10(23, "pow(10.0, a)", { a -> 10f.pow(a) }, { a -> 10.0.pow(a) }),

}