package me.anno.graph.types.flow.maths

import me.anno.fonts.signeddistfields.algorithm.SDFMaths.median
import me.anno.utils.maths.Maths.clamp

enum class FloatMathsTernary(
    val glsl: String,
    val float: (a: Float, b: Float, c: Float) -> Float,
    val double: (a: Double, b: Double, c: Double) -> Double
) {

    CLAMP("clamp(a,b,c)", { v, min, max -> clamp(v, min, max) }, { v, min, max -> clamp(v, min, max) }),
    MEDIAN("max(min(a,b),min(max(a,b),c))", { a, b, c -> median(a, b, c) }, { a, b, c -> median(a, b, c) })

}