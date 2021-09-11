package me.anno.graph.types.flow.maths

enum class FloatMathsUnary(
    val glsl: String,
    val float: (a: Float) -> Float,
    val double: (a: Double) -> Double
) {

    // unary
    ABS("abs(a)", { a -> kotlin.math.abs(a) }, { a -> kotlin.math.abs(a) }),
    FLOOR("floor(a)", { a -> kotlin.math.floor(a) }, { a -> kotlin.math.floor(a) }),
    ROUND("round(a)", { a -> kotlin.math.round(a) }, { a -> kotlin.math.round(a) }),
    CEIL("ceil(a)", { a -> kotlin.math.ceil(a) }, { a -> kotlin.math.ceil(a) }),
    FRACT("fract(a)", { a -> a - kotlin.math.floor(a) }, { a -> a - kotlin.math.floor(a) }),
    NEG("-a", { a -> -a }, { a -> -a }),

}