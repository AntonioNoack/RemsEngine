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
            time0: Double,
            time1: Double,
            time2: Double,
            time3: Double,
            t0: Double
        ): Vector4d {

            if (t0 <= 0.0) {
                return left
            }

            if (t0 >= 1.0) {
                return right
            }

            val g0 = 1.0 - t0
            val fg = t0 * g0

            val l = time1 - time0
            val r = time3 - time2
            val d = time2 - time1

            val maxGradient = 10.0

            val e0 = if (time1 == time0) 0.0 else fg * min(d / l, maxGradient)
            val e1 = if (time3 == time2) 0.0 else fg * min(d / r, maxGradient)

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
            time0: Double,
            time1: Double,
            time2: Double,
            time3: Double,
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
            time0: Double,
            time1: Double,
            time2: Double,
            time3: Double,
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
            time0: Double,
            time1: Double,
            time2: Double,
            time3: Double,
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
            time0: Double,
            time1: Double,
            time2: Double,
            time3: Double,
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
        time0: Double,
        time1: Double,
        time2: Double,
        time3: Double,
        t0: Double
    ): Vector4d

    companion object {

        fun getWeights(

            f0: Keyframe<*>,
            f1: Keyframe<*>,
            f2: Keyframe<*>,
            f3: Keyframe<*>,
            t0: Double

        ): Vector4d {

            val interpolation = (if(t0 > 1.0) f2 else f1).interpolation
            return interpolation.getWeights(
                f0.time, f1.time, f2.time, f3.time, t0
            )

        }

        val left = Vector4d(0.0, 1.0, 0.0, 0.0)
        val right = Vector4d(0.0, 0.0, 1.0, 0.0)

        fun getType(code: Int) = values().firstOrNull { it.code == code } ?: SPLINE

    }

}