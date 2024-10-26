package me.anno.animation

import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.pow
import org.joml.Vector4d
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * these are the easing functions from https://easings.net/
 * */
@Suppress("unused")
enum class Interpolation(val id: Int, val nameDesc: NameDesc) {

    SPLINE(0, "Spline", "Smooth curve") {
        // if only two points are known, a spline is a linear interpolator
        override fun getIn(x: Double): Double = x
        override fun getWeights(
            t0: Double,
            t1: Double,
            t2: Double,
            t3: Double,
            x: Double,
            dst: Vector4d
        ): Vector4d {

            if (x <= 0.0) return dst.set(0.0, 1.0, 0.0, 0.0)
            if (x >= 1.0) return dst.set(0.0, 0.0, 1.0, 0.0)

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
    LINEAR_BOUNDED(1, "Linear", "Straight curve segments, mix(a,b,clamp(t,0.0,1.0))") {
        override fun getIn(x: Double): Double = clamp(x, 0.0, 1.0)
    },
    LINEAR_UNBOUNDED(
        2,
        NameDesc(
            "Linear (unbounded)",
            "Straight curve segments, extending into infinity, mix(a,b,t)",
            "linearUnbounded"
        )
    ) {
        override fun getIn(x: Double): Double = x
    },

    STEP(3, "Step", "First half is the first value, second half is the second value, t > 0.5 ? a : b") {
        override fun getIn(x: Double): Double = if (x < 0.5) 0.0 else 1.0
    },

    SINE(4, "Sine", "Uses a cosine function, mix(a, b, (1-cos(pi*t))/2)") {
        override fun getIn(x: Double): Double = 0.5 - cos(x * PI) * 0.5
    },

    EASE_IN(5, "Ease-In") {
        override fun getReversedType(): Interpolation = EASE_OUT
        override fun getIn(x: Double): Double {
            val time = 1.0 - clamp(x, 0.0, 1.0)
            val expM2 = exp(-3.0)
            return (exp(-time * 3.0) - expM2) / (1.0 - expM2)
        }
    },

    EASE_OUT(6, "Ease-Out") {
        override fun getReversedType(): Interpolation = EASE_IN
        override fun getOut(x: Double): Double = EASE_IN.getIn(x)
        override fun getIn(x: Double): Double = EASE_IN.getOut(x)
    },

    SWING(7, "Swinging") {
        override fun getReversedType(): Interpolation = SWING_REV
        override fun getIn(x: Double): Double {
            val time = 1.0 - clamp(x, 0.0, 1.0)
            val expFactor = 7.0
            val expM2 = exp(-expFactor)
            return (exp(-time * expFactor) - expM2) / (1.0 - expM2) * mix(
                1.0,
                3.0 * cos(time * PI * 5.0),
                clamp(2.0 * time, 0.0, 1.0)
            )
        }
    },
    SWING_REV(8, "SwingingReverse") {
        override fun getIn(x: Double): Double = SWING.getOut(x)
        override fun getOut(x: Double): Double = SWING.getIn(x)
        override fun getReversedType(): Interpolation = SWING
    },

    // the following easing functions are implemented based on https://easings.net/#easeInQuart
    SINE_IN(10, "Sine In") {
        override fun getReversedType(): Interpolation = SINE_OUT
        override fun getOut(x: Double): Double = sin(x * PI * 0.5)
    },
    SINE_OUT(11, "Sine Out") {
        override fun getReversedType(): Interpolation = SINE_IN
        override fun getIn(x: Double): Double = sin(x * PI * 0.5)
    },
    SINE_SYM(12, "Sine Symmetric") {
        override fun getIn(x: Double): Double = 0.5 - 0.5 * cos(x * PI)
    },
    QUAD_IN(20, "Quadratic In") {
        override fun getReversedType(): Interpolation = QUAD_OUT
        override fun getIn(x: Double): Double = x * x
    },
    QUAD_OUT(21, "Quadratic Out") {
        override fun getReversedType(): Interpolation = QUAD_IN
        override fun getOut(x: Double): Double = x * x
    },
    QUAD_SYM(22, "Quadratic Symmetric") {
        override fun getIn(x: Double): Double = QUAD_IN.getInOut(x)
        override fun getOut(x: Double): Double = QUAD_IN.getInOut(x)
    },
    CUBIC_IN(30, "Cubic In") {
        override fun getReversedType(): Interpolation = CUBIC_OUT
        override fun getIn(x: Double): Double = x * x * x
    },
    CUBIC_OUT(31, "Cubic Out") {
        override fun getReversedType(): Interpolation = CUBIC_IN
        override fun getOut(x: Double): Double = x * x * x
    },
    CUBIC_SYM(32, "Cubic Symmetric") {
        override fun getIn(x: Double): Double = CUBIC_IN.getInOut(x)
        override fun getOut(x: Double): Double = CUBIC_IN.getInOut(x)
    },
    QUART_IN(40, "Quart In") {
        override fun getReversedType(): Interpolation = QUART_OUT
        override fun getIn(x: Double): Double {
            val x2 = x * x
            return x2 * x2
        }
    },
    QUART_OUT(41, "Quart Out") {
        override fun getReversedType(): Interpolation = QUART_IN
        override fun getIn(x: Double): Double = QUART_IN.getOut(x)
        override fun getOut(x: Double): Double = QUART_IN.getIn(x)
    },
    QUART_SYM(42, "Quart Symmetric") {
        override fun getIn(x: Double): Double = QUART_IN.getInOut(x)
        override fun getOut(x: Double): Double = QUART_IN.getInOut(x)
    },
    QUINT_IN(50, "Quint In") {
        override fun getReversedType(): Interpolation = QUINT_OUT
        override fun getIn(x: Double): Double {
            val x2 = x * x
            return x2 * x2 * x
        }
    },
    QUINT_OUT(51, "Quint Out") {
        override fun getIn(x: Double): Double = QUINT_IN.getOut(x)
        override fun getOut(x: Double): Double = QUINT_IN.getIn(x)
        override fun getReversedType(): Interpolation = QUINT_IN
    },
    QUINT_SYM(52, "Quint Symmetric") {
        override fun getIn(x: Double): Double = QUINT_IN.getInOut(x)
        override fun getOut(x: Double): Double = QUINT_IN.getInOut(x)
    },
    EXP_IN(60, "Exponential In") {
        val zero = pow(2.0, -10.0)
        val scale = 1.0 / (1.0 - zero)
        override fun getIn(x: Double): Double = (pow(2.0, 10.0 * x - 10.0) - zero) * scale
        override fun getReversedType(): Interpolation = EXP_OUT
    },
    EXP_OUT(61, "Exponential Out") {
        override fun getIn(x: Double): Double = EXP_IN.getOut(x)
        override fun getOut(x: Double): Double = EXP_IN.getIn(x)
        override fun getReversedType(): Interpolation = EXP_IN
    },
    EXP_SYM(62, "Exponential Symmetric") {
        override fun getIn(x: Double): Double = EXP_IN.getInOut(x)
        override fun getOut(x: Double): Double = EXP_IN.getInOut(x)
    },
    CIRCLE_IN(70, "Circle In") {
        override fun getIn(x: Double): Double = 1.0 - sqrt(max(1.0 - x * x, 0.0))
        override fun getReversedType(): Interpolation = CIRCLE_OUT
    },
    CIRCLE_OUT(71, "Circle Out") {
        override fun getIn(x: Double): Double = CIRCLE_IN.getOut(x)
        override fun getOut(x: Double): Double = CIRCLE_IN.getIn(x)
        override fun getReversedType(): Interpolation = CIRCLE_IN
    },
    CIRCLE_SYM(72, "Circle Symmetric") {
        override fun getIn(x: Double): Double = CIRCLE_IN.getInOut(x)
        override fun getOut(x: Double): Double = CIRCLE_IN.getInOut(x)
    },
    BACK_IN(80, "Back In") {
        override fun getReversedType(): Interpolation = BACK_OUT
        override fun getIn(x: Double): Double {
            val c1 = 1.70158
            val c3 = c1 + 1.0
            return c3 * x * x * x - c1 * x * x
        }
    },
    BACK_OUT(81, "Back Out") {
        override fun getIn(x: Double): Double = BACK_IN.getOut(x)
        override fun getOut(x: Double): Double = BACK_IN.getIn(x)
        override fun getReversedType(): Interpolation = BACK_IN
    },
    BACK_SYM(82, "Back Symmetric") {
        override fun getIn(x: Double): Double = BACK_IN.getInOut(x)
        override fun getOut(x: Double): Double = BACK_IN.getInOut(x)
    },
    ELASTIC_IN(90, "Elastic In") {
        private val magic = 2.0 * PI / 3.0
        override fun getReversedType(): Interpolation = ELASTIC_OUT
        override fun getIn(x: Double): Double {
            return -EXP_IN.getIn(x) * sin((x * 10.0 - 10.75) * magic)
        }
    },
    ELASTIC_OUT(91, "Elastic Out") {
        override fun getIn(x: Double): Double = ELASTIC_IN.getOut(x)
        override fun getOut(x: Double): Double = ELASTIC_IN.getIn(x)
        override fun getReversedType(): Interpolation = ELASTIC_IN
    },
    ELASTIC_SYM(92, "Elastic Symmetric") {
        override fun getIn(x: Double): Double = ELASTIC_IN.getInOut(x)
        override fun getOut(x: Double): Double = ELASTIC_IN.getInOut(x)
    },
    BOUNCE_IN(100, "Bounce In") {
        private val n1 = 7.5625
        private val d1 = 2.75
        private val invD1 = 1.0 / d1
        private val invD1x2 = 2.0 / d1
        private val invD1x15 = 1.5 / d1
        private val invD1x25 = 2.5 / d1
        private val invD1x225 = 2.25 / d1
        private val invD1x265 = 2.625 / d1
        override fun getReversedType(): Interpolation = BOUNCE_OUT
        override fun getOut(x: Double): Double {
            var offsetIn = 0.0
            var offsetOut = 0.0
            when {
                x < invD1 -> {}
                x < invD1x2 -> {
                    offsetIn = invD1x15
                    offsetOut = 0.75
                }
                x < invD1x25 -> {
                    offsetIn = invD1x225
                    offsetOut = 0.9375
                }
                else -> {
                    offsetIn = invD1x265
                    offsetOut = 0.984375
                }
            }
            val y = x - offsetIn
            return n1 * y * y + offsetOut
        }
    },
    BOUNCE_OUT(101, "Bounce Out") {
        override fun getIn(x: Double): Double = BOUNCE_IN.getOut(x)
        override fun getOut(x: Double): Double = BOUNCE_IN.getIn(x)
        override fun getReversedType(): Interpolation = BOUNCE_IN
    },
    BOUNCE_SYM(102, "Bounce Symmetric") {
        override fun getIn(x: Double): Double = BOUNCE_IN.getInOut(x)
        override fun getOut(x: Double): Double = BOUNCE_IN.getInOut(x)
    };

    constructor(id: Int, name: String) : this(id, name, "")
    constructor(id: Int, name: String, desc: String) :
            this(id, NameDesc(name, desc, "interpolation.${name.lowercase()}"))

    val displayName get() = nameDesc.name
    val description get() = nameDesc.desc

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

    open fun getReversedType(): Interpolation = this

    open fun getIn(x: Double): Double {
        return 1.0 - getOut(1.0 - x)
    }

    open fun getIn(x: Float): Float {
        return getIn(x.toDouble()).toFloat()
    }

    open fun getOut(x: Double): Double {
        return 1.0 - getIn(1.0 - x)
    }

    open fun getOut(x: Float): Float {
        return getOut(x.toDouble()).toFloat()
    }

    open fun getInOut(x: Double): Double {
        // return mix(getIn(x), getOut(x), x)
        return if (x < 0.5) {
            getIn(x * 2.0) * 0.5
        } else {
            getOut(x * 2.0 - 1.0) * 0.5 + 0.5
        }
    }

    open fun getInOut(x: Float): Float {
        return getInOut(x.toDouble()).toFloat()
    }

    open fun getInClamped(x: Double): Double {
        return getIn(clamp(x))
    }

    open fun getInClamped(x: Float): Float {
        return getIn(clamp(x).toDouble()).toFloat()
    }

    open fun getOutClamped(x: Double): Double {
        return getOut(clamp(x))
    }

    open fun getOutClamped(x: Float): Float {
        return getOut(clamp(x).toDouble()).toFloat()
    }

    open fun getInOutClamped(x: Double): Double {
        return getInOut(clamp(x))
    }

    open fun getInOutClamped(x: Float): Float {
        return getInOut(clamp(x).toDouble()).toFloat()
    }

    companion object {
        @JvmStatic
        fun getType(code: Int): Interpolation = entries.firstOrNull { it.id == code } ?: SPLINE
    }
}