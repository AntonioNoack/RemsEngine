package me.anno.graph.visual.scalar

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

enum class FloatMathsBinary(
    val id: Int,
    val glsl: String,
) {

    ADD(0, "a+b"),
    SUB(1, "a-b"),
    MUL(2, "a*b"),
    DIV(3, "a/b"),
    MOD(4, "mod(a,b)"),
    POW(5, "pow(a,b)"),
    ROOT(6, "pow(a,1.0/b)"),

    LENGTH(10, "length(vec2(a,b))"),
    LENGTH_SQUARED(11, "dot(vec2(a,b),vec2(a,b))"),
    ABS_DELTA(12, "abs(a-b)"),
    NORM1(13, "abs(a)+abs(b)"),

    AVG(20, "(a+b)*0.5"),
    GEO_MEAN(21, "sqrt(a*b)"),
    MIN(22, "min(a,b)"),
    MAX(23, "max(a,b)"),

    ATAN2(40, "atan(a,b)"),

    ;

    val supportsVectors: Boolean
        get() = when (this) {
            LENGTH, LENGTH_SQUARED -> false
            else -> true
        }

    fun double(a: Double, b: Double): Double {
        return when (this) {
            ADD -> a + b
            SUB -> a - b
            MUL -> a * b
            DIV -> a / b
            MOD -> a % b
            POW -> a.pow(b)
            ROOT -> a.pow(1.0 / b)
            LENGTH -> hypot(a, b)
            LENGTH_SQUARED -> a * a + b * b
            ABS_DELTA -> abs(a - b)
            NORM1 -> abs(a) + abs(b)
            AVG -> (a + b) * 0.5
            GEO_MEAN -> sqrt(a * b)
            MIN -> min(a, b)
            MAX -> max(a, b)
            ATAN2 -> atan2(a, b)
        }
    }

    fun float(a: Float, b: Float): Float {
        return double(a.toDouble(), b.toDouble()).toFloat()
    }
}