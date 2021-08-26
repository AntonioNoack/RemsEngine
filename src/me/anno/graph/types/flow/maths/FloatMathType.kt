package me.anno.graph.types.flow.maths

import me.anno.utils.maths.Maths

enum class FloatMathType(
    val float: (a: Float, b: Float) -> Float,
    val double: (a: Double, b: Double) -> Double
) {

    // unary
    ABS({ a, _ -> kotlin.math.abs(a) }, { a, _ -> kotlin.math.abs(a) }),
    FLOOR({ a, _ -> kotlin.math.floor(a) }, { a, _ -> kotlin.math.floor(a) }),
    ROUND({ a, _ -> kotlin.math.round(a) }, { a, _ -> kotlin.math.round(a) }),
    CEIL({ a, _ -> kotlin.math.ceil(a) }, { a, _ -> kotlin.math.ceil(a) }),
    FRACT({ a, _ -> a - kotlin.math.floor(a) }, { a, _ -> a - kotlin.math.floor(a) }),
    NEG({ a, _ -> -a }, { a, _ -> -a }),

    // binary
    ADD({ a, b -> a + b }, { a, b -> a + b }),
    SUB({ a, b -> a - b }, { a, b -> a - b }),
    MUL({ a, b -> a * b }, { a, b -> a * b }),
    DIV({ a, b -> a / b }, { a, b -> a / b }),
    MOD({ a, b -> a % b }, { a, b -> a % b }),
    POW({ a, b -> Maths.pow(a, b) }, { a, b -> StrictMath.pow(a, b) }),
    ROOT({ a, b -> Maths.pow(a, 1 / b) }, { a, b -> StrictMath.pow(a, 1 / b) }),
    LENGTH({ a, b -> kotlin.math.sqrt(a * a + b * b) }, { a, b -> kotlin.math.sqrt(a * a + b * b) }),
    LENGTH_SQUARED({ a, b -> a * a + b * b }, { a, b -> a * a + b * b }),
    ABS_DELTA({ a, b -> kotlin.math.abs(a - b) }, { a, b -> kotlin.math.abs(a - b) }),
    NORM1({ a, b -> kotlin.math.abs(a) + kotlin.math.abs(b) }, { a, b -> kotlin.math.abs(a) + kotlin.math.abs(b) }),
    AVG({ a, b -> (a + b) * 0.5f }, { a, b -> (a + b) * 0.5 }),
    GEO_MEAN({ a, b -> kotlin.math.sqrt(a * b) }, { a, b -> kotlin.math.sqrt(a * b) }),
    MIN({ a, b -> kotlin.math.min(a, b) }, { a, b -> kotlin.math.min(a, b) }),
    MAX({ a, b -> kotlin.math.max(a, b) }, { a, b -> kotlin.math.max(a, b) }),

}