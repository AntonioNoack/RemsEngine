package me.anno.graph.types.flow.maths

import me.anno.utils.types.Floats.toDegrees
import me.anno.utils.types.Floats.toRadians
import kotlin.math.*

enum class FloatMathsUnary(
    val id: Int, val glsl: String,
    val float: (a: Float) -> Float,
    val double: (a: Double) -> Double
) {

    ABS(0, "abs(a)", { abs(it) }, { abs(it) }),
    NEG(1, "-a", { -it }, { -it }),
    FLOOR(2, "floor(a)", { floor(it) }, { floor(it) }),
    ROUND(3, "round(a)", { round(it) }, { round(it) }),
    CEIL(4, "ceil(a)", { ceil(it) }, { ceil(it) }),
    FRACT(5, "fract(a)", { it - floor(it) }, { it - floor(it) }),
    TRUNC(5, "float(int(a))", { truncate(it) }, { truncate(it) }),

    LN(10, "log(a)", { ln(it) }, { ln(it) }),
    LN1P(11, "log(1.0+a)", { ln1p(it.toDouble()).toFloat() }, { ln1p(it) }),
    LOG2(12, "log2(a)", { log2(it) }, { log2(it) }),
    LOG10(13, "log10(a)", { log10(it) }, { log10(it) }),

    EXP(20, "exp(a)", { exp(it) }, { exp(it) }),
    EXPM1(21, "exp(a)-1.0", { expm1(it) }, { expm1(it) }),

    // todo unable to find compatible overloaded function "pow(float, vec3)"
    EXP2(22, "pow(2.0, a)", { 2f.pow(it) }, { 2.0.pow(it) }),
    EXP10(23, "pow(10.0, a)", { 10f.pow(it) }, { 10.0.pow(it) }),
    SQRT(26, "sqrt(a)", { sqrt(it) }, { sqrt(it) }),
    CBRT(27, "pow(a,1.0/3.0)", { cbrt(it) }, { cbrt(it) }), // cbrt is not available in glsl
    INV_SQRT(28, "inversesqrt(a)", { 1f / sqrt(it) }, { 1.0 / sqrt(it) }),

    ONE_MINUS(30, "1.0-a", { 1f - it }, { 1.0 - it }),
    INVERT(31, "1.0/a", { 1f / it }, { 1.0 / it }),

    SIN(40, "sin(a)", { sin(it) }, { sin(it) }),
    COS(41, "cos(a)", { cos(it) }, { cos(it) }),
    TAN(42, "tan(a)", { tan(it) }, { tan(it) }),
    ASIN(43, "asin(a)", { asin(it) }, { asin(it) }),
    ACOS(44, "acos(a)", { acos(it) }, { acos(it) }),
    ATAN(45, "atan(a)", { atan(it) }, { atan(it) }),
    SINH(46, "sinh(a)", { sinh(it) }, { sinh(it) }),
    COSH(47, "cosh(a)", { cosh(it) }, { cosh(it) }),
    TANH(48, "tanh(a)", { tanh(it) }, { tanh(it) }),
    ASINH(49, "asinh(a)", { asinh(it) }, { asinh(it) }),
    ACOSH(50, "acosh(a)", { acosh(it) }, { acosh(it) }),
    ATANH(51, "atanh(a)", { atanh(it) }, { atanh(it) }),

    RAD_TO_DEG(60, "a*${180.0 / PI}", { it.toDegrees() }, { it.toDegrees() }),
    DEG_TO_RAD(61, "a*${PI / 180.0}", { it.toRadians() }, { it.toRadians() }),

    ;

    companion object {
        val supportedUnaryVecTypes = entries.filter { it != EXP2 && it != EXP10 }
    }

}