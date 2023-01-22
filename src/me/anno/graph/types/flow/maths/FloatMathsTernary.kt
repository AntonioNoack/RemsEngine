package me.anno.graph.types.flow.maths

enum class FloatMathsTernary(
    val id: Int,
    val glsl: String,
    val float: (a: Float, b: Float, c: Float) -> Float,
    val double: (a: Double, b: Double, c: Double) -> Double
) {

    CLAMP(0, "clamp(a,b,c)",
        { v, min, max -> me.anno.maths.Maths.clamp(v, min, max) },
        { v, min, max -> me.anno.maths.Maths.clamp(v, min, max) }),
    MEDIAN(1, "max(min(a,b),min(max(a,b),c))",
        { a, b, c -> me.anno.maths.Maths.median(a, b, c) },
        { a, b, c -> me.anno.maths.Maths.median(a, b, c) }),

    MIX(2, "mix(a,b,c)",
        { a, b, c -> me.anno.maths.Maths.mix(a, b, c) },
        { a, b, c -> me.anno.maths.Maths.mix(a, b, c) }),
    UNMIX(
        3, "(a-b)/(c-b)",
        { a, b, c -> me.anno.maths.Maths.unmix(a, b, c) },
        { a, b, c -> me.anno.maths.Maths.unmix(a, b, c) }),
    MIX_CLAMPED(4, "mix(a,b,clamp(c,0.0,1.0))",
        { a, b, c -> me.anno.maths.Maths.mix(a, b, me.anno.maths.Maths.clamp(c)) },
        { a, b, c -> me.anno.maths.Maths.mix(a, b, me.anno.maths.Maths.clamp(c)) }),
    UNMIX_CLAMPED(
        5, "clamp((a-b)/(c-b),0.0,1.0)",
        { a, b, c -> me.anno.maths.Maths.clamp(me.anno.maths.Maths.unmix(a, b, c)) },
        { a, b, c -> me.anno.maths.Maths.clamp(me.anno.maths.Maths.unmix(a, b, c)) }),


    ADD3(10, "a+b+c", { a, b, c -> a + b + c }, { a, b, c -> a + b + c }),
    MUL3(12, "a*b*c", { a, b, c -> a * b * c }, { a, b, c -> a * b * c }),
    MUL_ADD(13, "a*b+c", { a, b, c -> a * b + c }, { a, b, c -> a * b * c }),

    ;

}