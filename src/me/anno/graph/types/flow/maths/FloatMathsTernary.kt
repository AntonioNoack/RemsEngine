package me.anno.graph.types.flow.maths

import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.median

enum class FloatMathsTernary(
    val id: Int,
    val glsl: String,
    val float: (a: Float, b: Float, c: Float) -> Float,
    val double: (a: Double, b: Double, c: Double) -> Double
) {

    CLAMP(0, "clamp(a,b,c)", { v, min, max -> clamp(v, min, max) }, { v, min, max -> clamp(v, min, max) }),
    MEDIAN(1, "max(min(a,b),min(max(a,b),c))", { a, b, c -> median(a, b, c) }, { a, b, c -> median(a, b, c) }),
    ADD(10, "a+b+c", { a, b, c -> a + b + c }, { a, b, c -> a + b + c }),
    MUL(12, "a*b*c", { a, b, c -> a * b * c }, { a, b, c -> a * b * c }),
}