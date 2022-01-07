package me.anno.animation

import me.anno.language.translation.Dict
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.maths.Maths.mix
import me.anno.utils.maths.Maths.pow
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import org.joml.Vector4d
import kotlin.math.*

enum class Interpolation(
    val id: Int, val symbol: String,
    private val displayNameEn: String,
    private val descriptionEn: String,
    private val dictSubPath: String
) {

    SPLINE(
        0, "S",
        "Spline",
        "Smooth curve", "spline"
    ) {

        override fun getWeights(
            t0: Double,
            t1: Double,
            t2: Double,
            t3: Double,
            x: Double,
            dst: Vector4d
        ): Vector4d {

            if (x <= 0.0) return left
            if (x >= 1.0) return right

            val g0 = 1.0 - x
            val fg = x * g0

            val l = t1 - t0
            val r = t3 - t2
            val d = t2 - t1

            val maxGradient = 10.0

            val e0 = if (t1 == t0) 0.0 else fg * min(d / l, maxGradient)
            val e1 = if (t3 == t2) 0.0 else fg * min(d / r, maxGradient)

            val w0 = -e0 * g0
            val w1 = (1 + e0) * g0
            val w2 = (1 + e1) * x
            val w3 = -e1 * x

            return dst.set(w0, w1, w2, w3)

        }

    },
    LINEAR_BOUNDED(
        1, "/",
        "Linear",
        "Straight curve segments, mix(a,b,clamp(t,0.0,1.0))", "linear"
    ) {
        override fun getIn(x: Double) = clamp(x, 0.0, 1.0)
    },
    LINEAR_UNBOUNDED(
        2, "//",
        "Linear (unbounded)",
        "Straight curve segments, extending into infinity, mix(a,b,t)", "linearUnbounded"
    ) {
        override fun getIn(x: Double) = x
    },

    STEP(
        3, "L",
        "Step",
        "First half is the first value, second half is the second value, t > 0.5 ? a : b", "step"
    ) {
        override fun getIn(x: Double) = if (x < 0.5) 0.0 else 1.0
    },

    SINE(
        4, "~",
        "Sine",
        "Uses a cosine function, mix(a, b, (1-cos(pi*t))/2)", "sine"
    ) {
        override fun getIn(x: Double) = cos(x * PI) * 0.5 + 0.5
    },

    EASE_IN(5, ">", "Ease-In", "", "ease-in") {
        override fun getIn(x: Double): Double {
            val time = clamp(x, 0.0, 1.0)
            val expM2 = exp(-3.0)
            return (exp(-time * 3.0) - expM2) / (1.0 - expM2)
        }
    },

    EASE_OUT(6, "<", "Ease-Out", "", "ease-out") {
        override fun getIn(x: Double): Double {
            val time = 1.0 - clamp(x, 0.0, 1.0)
            val expM2 = exp(-3.0)
            return (exp(-time * 3.0) - expM2) / (1.0 - expM2)
        }
    },

    SWING(7, "#", "Swinging", "", "swing") {
        override fun getIn(x: Double): Double {
            val time = clamp(x, 0.0, 1.0)
            val expFactor = 7.0
            val expM2 = exp(-expFactor)
            return (exp(-time * expFactor) - expM2) / (1.0 - expM2) * mix(
                1.0,
                3.0 * cos(time * Math.PI * 5.0),
                clamp(2.0 * time, 0.0, 1.0)
            )
        }
    },

    // the following easing functions are implemented based on https://easings.net/#easeInQuart
    SINE_IN(10, "Si") {
        override fun getOut(x: Double) = sin(x * PI * 0.5)
    },
    SINE_OUT(11, "So") {
        override fun getIn(x: Double) = sin(x * PI * 0.5)
    },
    SINE_SYM(12, "Ss") {
        override fun getIn(x: Double) = 0.5 - 0.5 * cos(x * PI)
    },
    QUAD_IN(20, "2i") {
        override fun getIn(x: Double) = x * x
    },
    QUAD_OUT(21, "2o") {
        override fun getOut(x: Double) = x * x
    },
    QUAD_SYM(22, "2s") {
        override fun getIn(x: Double) = QUAD_IN.getInOut(x)
        override fun getOut(x: Double) = QUAD_IN.getInOut(x)
    },
    CUBIC_IN(30, "3i") {
        override fun getIn(x: Double) = x * x * x
    },
    CUBIC_OUT(31, "3o") {
        override fun getOut(x: Double) = x * x * x
    },
    CUBIC_SYM(32, "3s") {
        override fun getIn(x: Double) = CUBIC_IN.getInOut(x)
        override fun getOut(x: Double) = CIRCLE_IN.getInOut(x)
    },
    QUART_IN(40, "4i") {
        override fun getIn(x: Double): Double {
            val x2 = x * x
            return x2 * x2
        }
    },
    QUART_OUT(41, "4o") {
        override fun getIn(x: Double) = QUART_IN.getOut(x)
        override fun getOut(x: Double) = QUART_IN.getIn(x)
    },
    QUART_SYM(42, "4s") {
        override fun getIn(x: Double) = QUART_IN.getInOut(x)
        override fun getOut(x: Double) = QUART_IN.getInOut(x)
    },
    QUINT_IN(50, "5i") {
        override fun getIn(x: Double): Double {
            val x2 = x * x
            return x2 * x2 * x
        }
    },
    QUINT_OUT(51, "5o") {
        override fun getIn(x: Double) = QUINT_IN.getOut(x)
        override fun getOut(x: Double) = QUINT_IN.getIn(x)
    },
    QUINT_SYM(52, "5s") {
        override fun getIn(x: Double) = QUINT_IN.getInOut(x)
        override fun getOut(x: Double) = QUINT_IN.getInOut(x)
    },
    EXP_IN(60, "Xi") {
        override fun getIn(x: Double) = pow(2.0, 10.0 * x - 10.0)
    },
    EXP_OUT(61, "Xo") {
        override fun getIn(x: Double) = EXP_IN.getOut(x)
        override fun getOut(x: Double) = EXP_IN.getIn(x)
    },
    EXP_SYM(62, "Xs") {
        override fun getIn(x: Double) = EXP_IN.getInOut(x)
        override fun getOut(x: Double) = EXP_IN.getInOut(x)
    },
    CIRCLE_IN(70, "Ci") {
        override fun getIn(x: Double) = 1.0 - sqrt(max(1.0 - x * x, 0.0))
    },
    CIRCLE_OUT(71, "Co") {
        override fun getIn(x: Double) = CIRCLE_IN.getOut(x)
    },
    CIRCLE_SYM(72, "Cs") {
        override fun getIn(x: Double) = CIRCLE_IN.getInOut(x)
        override fun getOut(x: Double) = CIRCLE_IN.getInOut(x)
    },
    BACK_IN(80, "Bi") {
        override fun getIn(x: Double): Double {
            val c1 = 1.70158
            val c3 = c1 + 1.0
            return c3 * x * x * x - c1 * x * x
        }
    },
    BACK_OUT(81, "Bo") {
        override fun getIn(x: Double) = BACK_IN.getOut(x)
        override fun getOut(x: Double) = BACK_IN.getIn(x)
    },
    BACK_SYM(82, "Bs") {
        override fun getIn(x: Double) = BACK_IN.getInOut(x)
        override fun getOut(x: Double) = BACK_IN.getInOut(x)
    },
    ELASTIC_IN(90, "Ei") {
        private val magic = 2.0 * Math.PI / 3.0
        override fun getIn(x: Double): Double {
            return -pow(2.0, 10.0 * x - 10.0) * sin((x * 10.0 - 10.75) * magic)
        }
    },
    ELASTIC_OUT(91, "Eo") {
        override fun getIn(x: Double) = ELASTIC_IN.getOut(x)
        override fun getOut(x: Double) = ELASTIC_IN.getIn(x)
    },
    ELASTIC_SYM(92, "Es") {
        override fun getIn(x: Double) = ELASTIC_IN.getInOut(x)
        override fun getOut(x: Double) = ELASTIC_IN.getInOut(x)
    },
    BOUNCE_IN(100, "Bi") {
        private val c0 = 7.5625
        private val c1 = 2.75
        private val d0 = 1.0 / c0
        private val d1 = 2.0 / c1
        private val d2 = 2.5 / c1
        private val e0 = 1.5 / c1
        private val e1 = 2.25 / c1
        private val e2 = 2.625 * d1
        override fun getOut(x: Double): Double {
            return c0 * when {
                x < d0 -> x * x
                x < d1 -> {
                    val y = x - e0
                    y * y + 0.75
                }
                x < d2 -> {
                    val y = x - e1
                    y * y + 0.9375
                }
                else -> {
                    val y = x - e2
                    y * y + 0.984375
                }
            }
        }
    },
    BOUNCE_OUT(101, "Bo") {
        override fun getIn(x: Double) = BOUNCE_IN.getOut(x)
        override fun getOut(x: Double) = BOUNCE_IN.getIn(x)
        override fun getInOut(x: Double) = BOUNCE_IN.getInOut(x)
    },
    BOUNCE_SYM(102, "Bs") {
        override fun getIn(x: Double) = BOUNCE_IN.getInOut(x)
        override fun getOut(x: Double) = BOUNCE_IN.getInOut(x)
    };

    constructor() : this(-1)
    constructor(code: Int) : this(code, "")
    constructor(code: Int, symbol: String) : this(code, symbol, "", "", "")

    val displayName get() = Dict[displayNameEn.ifEmpty { name.camelCaseToTitle() }, "dict.$dictSubPath"]
    val description get() = Dict[descriptionEn, "dict.$dictSubPath.desc"]

    open fun getWeights(
        t0: Double,
        t1: Double,
        t2: Double,
        t3: Double,
        x: Double,
        dst: Vector4d = Vector4d()
    ): Vector4d {
        val w = getIn(x)
        return dst.set(0.0, 1.0 - w, w, 0.0)
    }

    open fun getIn(x: Double): Double {
        return 1.0 - getOut(1.0 - x)
    }

    open fun getOut(x: Double): Double {
        return 1.0 - getIn(1.0 - x)
    }

    open fun getInOut(x: Double): Double {
        return if (x < 0.5) {
            getIn(x * 2.0) * 0.5
        } else {
            getOut(x * 2.0 - 1.0) * 0.5 + 0.5
        }
    }

    open fun getInClamped(x: Double): Double {
        return getIn(clamp(x))
    }

    open fun getOutClamped(x: Double): Double {
        return getOut(clamp(x))
    }

    open fun getInOutClamped(x: Double): Double {
        return getInOut(clamp(x))
    }

    companion object {

        fun getWeights(

            f0: Keyframe<*>,
            f1: Keyframe<*>,
            f2: Keyframe<*>,
            f3: Keyframe<*>,
            t0: Double

        ): Vector4d {

            val interpolation = (if (t0 > 1.0) f2 else f1).interpolation
            return interpolation.getWeights(
                f0.time, f1.time, f2.time, f3.time, t0
            )

        }

        val left = Vector4d(0.0, 1.0, 0.0, 0.0)
        val right = Vector4d(0.0, 0.0, 1.0, 0.0)

        fun getType(code: Int) = values().firstOrNull { it.id == code } ?: SPLINE

    }

}