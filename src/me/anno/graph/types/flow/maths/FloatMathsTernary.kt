package me.anno.graph.types.flow.maths

import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.median
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.unmix

enum class FloatMathsTernary(
    val id: Int,
    val glsl: String
) {

    CLAMP(0, "clamp(a,b,c)"),
    MEDIAN(1, "max(min(a,b),min(max(a,b),c))"),

    MIX(2, "mix(a,b,c)"),
    UNMIX(3, "(a-b)/(c-b)"),
    MIX_CLAMPED(4, "mix(a,b,clamp(c,0.0,1.0))"),
    UNMIX_CLAMPED(5, "clamp((a-b)/(c-b),0.0,1.0)"),


    ADD3(10, "a+b+c"),
    MUL3(12, "a*b*c"),
    MUL_ADD(13, "a*b+c"),

    ;

    fun double(a: Double, b: Double, c: Double): Double {
        return when (this) {
            CLAMP -> clamp(a, b, c)
            MEDIAN -> median(a, b, c)
            MIX -> mix(a, b, c)
            UNMIX -> unmix(a, b, c)
            MIX_CLAMPED -> mix(a, b, clamp(c))
            UNMIX_CLAMPED -> clamp(unmix(a, b, c))
            ADD3 -> a + b + c
            MUL3 -> a * b * c
            MUL_ADD -> a * b + c
        }
    }

    fun float(a: Float, b: Float, c: Float): Float {
        return double(a.toDouble(), b.toDouble(), c.toDouble()).toFloat()
    }
}