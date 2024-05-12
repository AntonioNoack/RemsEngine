package me.anno.graph.visual.scalar

import me.anno.maths.Maths.fract
import me.anno.utils.types.Floats.toDegrees
import me.anno.utils.types.Floats.toRadians
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atanh
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.expm1
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.ln1p
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh
import kotlin.math.truncate

enum class FloatMathUnary(
    val id: Int, val glsl: String
) {

    ABS(0, "abs(a)"),
    NEG(1, "-a"),
    FLOOR(2, "floor(a)"),
    ROUND(3, "round(a)"),
    CEIL(4, "ceil(a)"),
    FRACT(5, "fract(a)"),
    TRUNCATE(5, "float(int(a))"),

    LN(10, "log(a)"),
    LN1P(11, "log(1.0+a)"),
    LOG2(12, "log2(a)"),
    LOG10(13, "log2(a)/log2(10.0)"),

    EXP(20, "exp(a)"),
    EXPM1(21, "exp(a)-1.0"),

    EXP2(22, "pow(2.0, a)"),
    EXP10(23, "pow(10.0, a)"),
    SQRT(26, "sqrt(a)"),
    CBRT(27, "pow(a,1.0/3.0)"), // cbrt is not available in glsl
    INV_SQRT(28, "inversesqrt(a)"),

    ONE_MINUS(30, "1.0-a"),
    INVERT(31, "1.0/a"),
    SMOOTHSTEP(32, "a*a*(3.0-2.0*a)"),

    SIN(40, "sin(a)"),
    COS(41, "cos(a)"),
    TAN(42, "tan(a)"),
    ASIN(43, "asin(a)"),
    ACOS(44, "acos(a)"),
    ATAN(45, "atan(a)"),
    SINH(46, "sinh(a)"),
    COSH(47, "cosh(a)"),
    TANH(48, "tanh(a)"),
    ASINH(49, "asinh(a)"),
    ACOSH(50, "acosh(a)"),
    ATANH(51, "atanh(a)"),

    RAD_TO_DEG(60, "a*${180.0 / PI}"),
    DEG_TO_RAD(61, "a*${PI / 180.0}"),

    ;

    fun double(a: Double): Double {
        return when (this) {
            ABS -> abs(a)
            NEG -> -a
            FLOOR -> floor(a)
            CEIL -> ceil(a)
            ROUND -> round(a)
            FRACT -> fract(a)
            TRUNCATE -> truncate(a)
            LN -> ln(a)
            LN1P -> ln1p(a)
            LOG2 -> log2(a)
            LOG10 -> log10(a)
            EXP -> exp(a)
            EXPM1 -> expm1(a)
            EXP2 -> 2.0.pow(a)
            EXP10 -> 10.0.pow(a)
            SQRT -> sqrt(a)
            CBRT -> cbrt(a)
            INV_SQRT -> 1.0 / sqrt(a)
            ONE_MINUS -> 1.0 - a
            INVERT -> 1.0 / a
            SMOOTHSTEP -> a * a * (3.0 - 2.0 * a)
            SIN -> sin(a)
            COS -> cos(a)
            TAN -> tan(a)
            ASIN -> asin(a)
            ACOS -> acos(a)
            ATAN -> atan(a)
            SINH -> sinh(a)
            COSH -> cosh(a)
            TANH -> tanh(a)
            ASINH -> asinh(a)
            ACOSH -> acosh(a)
            ATANH -> atanh(a)
            RAD_TO_DEG -> a.toDegrees()
            DEG_TO_RAD -> a.toRadians()
        }
    }

    fun float(a: Float): Float {
        return double(a.toDouble()).toFloat()
    }

    fun int(a: Int): Int {
        return double(a.toDouble()).toInt()
    }

    companion object {
        val supportedUnaryVecTypes = entries.filter { it != EXP2 && it != EXP10 }
    }
}