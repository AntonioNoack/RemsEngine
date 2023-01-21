package me.anno.graph.types.flow.maths

enum class IntMathsTernary(
    val id: Int,
    val glsl: String,
    val int: (a: Int, b: Int, c: Int) -> Int,
    val long: (a: Long, b: Long, c: Long) -> Long
) {

    CLAMP(0, "clamp(a,b,c)",
        { v, min, max -> me.anno.maths.Maths.clamp(v, min, max) },
        { v, min, max -> me.anno.maths.Maths.clamp(v, min, max) }),
    MEDIAN(1, "max(min(a,b),min(max(a,b),c))",
        { a, b, c -> me.anno.maths.Maths.median(a, b, c) },
        { a, b, c -> me.anno.maths.Maths.median(a, b, c) }),
    ADD(10, "a+b+c", { a, b, c -> a + b + c }, { a, b, c -> a + b + c }),
    MUL(12, "a*b*c", { a, b, c -> a * b * c }, { a, b, c -> a * b * c }),
    MUL_ADD(13, "a*b+c", { a, b, c -> a * b + c }, { a, b, c -> a * b * c }),

    ;

}