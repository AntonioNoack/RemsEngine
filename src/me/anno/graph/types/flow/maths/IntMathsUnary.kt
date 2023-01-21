package me.anno.graph.types.flow.maths

enum class IntMathsUnary(
    val id: Int,
    val glsl: String,
    val int: (a: Int) -> Int,
    val long: (a: Long) -> Long
) {

    ABS(0, "abs(a)", { a -> kotlin.math.abs(a) }, { a -> kotlin.math.abs(a) }),
    NEG(1, "-a", { a -> -a }, { a -> -a }),
    NOT(2, "~a", { a -> a.inv() }, { a -> a.inv() }), // = -x-1
    SQRT(
        3, "sqrt(a)",
        { a -> kotlin.math.sqrt(a.toDouble()).toInt() },
        { a -> kotlin.math.sqrt(a.toDouble()).toLong() }),

    ;

}