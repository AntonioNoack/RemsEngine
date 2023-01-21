package me.anno.graph.types.flow.maths

import me.anno.utils.types.Floats.toDegrees
import me.anno.utils.types.Floats.toRadians
import kotlin.math.pow

enum class FloatMathsUnary(
    val id: Int, val glsl: String,
    val float: (a: Float) -> Float,
    val double: (a: Double) -> Double
) {

    ABS(0, "abs(a)", { kotlin.math.abs(it) }, { kotlin.math.abs(it) }),
    NEG(1, "-a", { -it }, { -it }),
    FLOOR(2, "floor(a)", { kotlin.math.floor(it) }, { kotlin.math.floor(it) }),
    ROUND(3, "round(a)", { kotlin.math.round(it) }, { kotlin.math.round(it) }),
    CEIL(4, "ceil(a)", { kotlin.math.ceil(it) }, { kotlin.math.ceil(it) }),
    FRACT(5, "fract(a)", { it - kotlin.math.floor(it) }, { it - kotlin.math.floor(it) }),
    TRUNC(5, "float(int(a))", { kotlin.math.truncate(it) }, { kotlin.math.truncate(it) }),

    LN(10, "ln(a)", { kotlin.math.ln(it) }, { kotlin.math.ln(it) }),
    LN1P(11, "ln(1.0+a)", { kotlin.math.ln1p(it.toDouble()).toFloat() }, { kotlin.math.ln1p(it) }),
    LOG2(12, "log2(a)", { kotlin.math.log2(it) }, { kotlin.math.log2(it) }),
    LOG10(13, "log10(a)", { kotlin.math.log10(it) }, { kotlin.math.log10(it) }),

    EXP(20, "exp(a)", { kotlin.math.exp(it) }, { kotlin.math.exp(it) }),
    EXPM1(21, "exp(a)-1.0", { kotlin.math.expm1(it) }, { kotlin.math.expm1(it) }),
    EXP2(22, "pow(2.0, a)", { 2f.pow(it) }, { 2.0.pow(it) }),
    EXP10(23, "pow(10.0, a)", { 10f.pow(it) }, { 10.0.pow(it) }),

    SIN(40, "sin(a)", { kotlin.math.sin(it) }, { kotlin.math.sin(it) }),
    COS(41, "cos(a)", { kotlin.math.cos(it) }, { kotlin.math.cos(it) }),
    TAN(42, "tan(a)", { kotlin.math.tan(it) }, { kotlin.math.tan(it) }),
    ASIN(43, "asin(a)", { kotlin.math.asin(it) }, { kotlin.math.asin(it) }),
    ACOS(44, "acos(a)", { kotlin.math.acos(it) }, { kotlin.math.acos(it) }),
    ATAN(45, "atan(a)", { kotlin.math.atan(it) }, { kotlin.math.atan(it) }),
    SINH(46, "sinh(a)", { kotlin.math.sinh(it) }, { kotlin.math.sinh(it) }),
    COSH(47, "cosh(a)", { kotlin.math.cosh(it) }, { kotlin.math.cosh(it) }),
    TANH(48, "tanh(a)", { kotlin.math.tanh(it) }, { kotlin.math.tanh(it) }),
    ASINH(49, "asinh(a)", { kotlin.math.asinh(it) }, { kotlin.math.asinh(it) }),
    ACOSH(50, "acosh(a)", { kotlin.math.acosh(it) }, { kotlin.math.acosh(it) }),
    ATANH(51, "atanh(a)", { kotlin.math.atanh(it) }, { kotlin.math.atanh(it) }),

    RAD_TO_DEG(60, "a*${180.0 / Math.PI}", { it.toDegrees() }, { it.toDegrees() }),
    DEG_TO_RAD(61, "a*${Math.PI / 180.0}", { it.toRadians() }, { it.toRadians() }),

    ;

    companion object {
        val values = values()
        val byId = values.associateBy { it.id }
    }

}