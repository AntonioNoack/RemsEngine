package me.anno.graph.types.flow.maths

enum class IntMathType(
    val int: (a: Int, b: Int) -> Int,
    val long: (a: Long, b: Long) -> Long
) {

    // unary
    ABS({ a, _ -> kotlin.math.abs(a) }, { a, _ -> kotlin.math.abs(a) }),
    NEG({ a, _ -> -a }, { a, _ -> -a }),

    // binary
    ADD({ a, b -> a + b }, { a, b -> a + b }),
    SUB({ a, b -> a - b }, { a, b -> a - b }),
    MUL({ a, b -> a * b }, { a, b -> a * b }),
    DIV({ a, b -> a / b }, { a, b -> a / b }),
    MOD({ a, b -> a % b }, { a, b -> a % b }),

    // POW({ a, b -> me.anno.utils.Maths.pow(a, b) }, { a, b -> StrictMath.pow(a, b) }),
    // ROOT({ a, b -> me.anno.utils.Maths.pow(a, 1 / b) }, { a, b -> StrictMath.pow(a, 1 / b) }),
    // LENGTH({ a, b -> kotlin.math.sqrt(a * a + b * b) }, { a, b -> kotlin.math.sqrt(a * a + b * b) }),
    LENGTH_SQUARED({ a, b -> a * a + b * b }, { a, b -> a * a + b * b }),
    ABS_DELTA({ a, b -> kotlin.math.abs(a - b) }, { a, b -> kotlin.math.abs(a - b) }),
    NORM1({ a, b -> kotlin.math.abs(a) + kotlin.math.abs(b) }, { a, b -> kotlin.math.abs(a) + kotlin.math.abs(b) }),
    AVG({ a, b -> (a + b) shr 1 }, { a, b -> (a + b) shr 1 }),

    // GEO_MEAN({ a, b -> kotlin.math.sqrt(a * b) }, { a, b -> kotlin.math.sqrt(a * b) }),
    MIN({ a, b -> kotlin.math.min(a, b) }, { a, b -> kotlin.math.min(a, b) }),
    MAX({ a, b -> kotlin.math.max(a, b) }, { a, b -> kotlin.math.max(a, b) })

}