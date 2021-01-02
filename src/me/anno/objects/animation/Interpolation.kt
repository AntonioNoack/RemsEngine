package me.anno.objects.animation

import me.anno.language.translation.Dict
import org.joml.Vector4d
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

enum class Interpolation(
    val code: Int, val symbol: String,
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
            f0: Keyframe<*>,
            f1: Keyframe<*>,
            f2: Keyframe<*>,
            f3: Keyframe<*>,
            t0: Double
        ): Vector4d {

            if (t0 <= 0.0) {
                return left
            }

            if (t0 >= 1.0) {
                return right
            }

            val x0 = f0.time
            val x1 = f1.time
            val x2 = f2.time
            val x3 = f3.time

            val g0 = 1.0 - t0
            val fg = t0 * g0

            val l = x1 - x0
            val r = x3 - x2
            val d = x2 - x1

            val maxGradient = 10.0

            val e0 = if (x1 == x0) 0.0 else fg * min(d / l, maxGradient)
            val e1 = if (x3 == x2) 0.0 else fg * min(d / r, maxGradient)

            val w0 = -e0 * g0
            val w1 = (1 + e0) * g0
            val w2 = (1 + e1) * t0
            val w3 = -e1 * t0

            return Vector4d(w0, w1, w2, w3)

        }

    },
    LINEAR_BOUNDED(
        1, "/",
        "Linear",
        "Straight curve segments, mix(a,b,clamp(t,0,1))", "linear"
    ) {

        override fun getWeights(
            f0: Keyframe<*>,
            f1: Keyframe<*>,
            f2: Keyframe<*>,
            f3: Keyframe<*>,
            t0: Double
        ): Vector4d {
            return when {
                t0 <= 0.0 -> left
                t0 >= 1.0 -> right
                else -> Vector4d(0.0, 1.0 - t0, t0, 0.0)
            }
        }

    },
    LINEAR_UNBOUNDED(
        2, "//",
        "Linear (unbounded)",
        "Straight curve segments, extending into infinity, mix(a,b,t)", "linearUnbounded"
    ) {

        override fun getWeights(
            f0: Keyframe<*>,
            f1: Keyframe<*>,
            f2: Keyframe<*>,
            f3: Keyframe<*>,
            t0: Double
        ): Vector4d {
            return Vector4d(0.0, 1.0 - t0, t0, 0.0)
        }

    },
    STEP(
        3, "L",
        "Step",
        "First half is the first value, second half is the second value, t > 0.5 ? a : b", "step"
    ) {

        override fun getWeights(
            f0: Keyframe<*>,
            f1: Keyframe<*>,
            f2: Keyframe<*>,
            f3: Keyframe<*>,
            t0: Double
        ): Vector4d {
            return if (t0 < 0.5) left else right
        }

    },
    SINE(
        4, "~",
        "Sine",
        "Uses a cosine function, mix(a, b, (1-cos(pi*t))/2)", "sine"
    ) {

        override fun getWeights(
            f0: Keyframe<*>,
            f1: Keyframe<*>,
            f2: Keyframe<*>,
            f3: Keyframe<*>,
            t0: Double
        ): Vector4d {
            val f = cos(t0 * PI) * 0.5 + 0.5
            val g = 1.0 - f
            return Vector4d(0.0, f, g, 0.0)
        }

    };

    val displayName get() = Dict[displayNameEn, "dict.$dictSubPath"]
    val description get() = Dict[descriptionEn, "dict.$dictSubPath.desc"]

    abstract fun getWeights(

        f0: Keyframe<*>,
        f1: Keyframe<*>,
        f2: Keyframe<*>,
        f3: Keyframe<*>,
        t0: Double

    ): Vector4d

    companion object {

        val left = Vector4d(0.0, 1.0, 0.0, 0.0)
        val right = Vector4d(0.0, 0.0, 1.0, 0.0)

        fun getType(code: Int) = values().firstOrNull { it.code == code } ?: SPLINE

    }

}