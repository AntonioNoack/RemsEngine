package me.anno.maths

import me.anno.maths.Maths.mix
import me.anno.maths.Maths.unmix

object Smoothstep {

    @JvmStatic
    fun smoothstepFactor(x: Float): Float {
        return when {
            x <= 0f -> 0f
            x < 1f -> x * x * (3f - 2f * x)
            else -> 1f
        }
    }

    @JvmStatic
    fun smoothstepFactor(x: Double): Double {
        return when {
            x <= 0.0 -> 0.0
            x < 1.0 -> x * x * (3.0 - 2.0 * x)
            else -> 1.0
        }
    }

    @JvmStatic
    fun smoothstepFactorUnsafe(x: Float): Float {
        return x * x * (3f - 2f * x)
    }

    @JvmStatic
    fun smoothstepFactorUnsafe(x: Double): Double {
        return x * x * (3.0 - 2.0 * x)
    }

    @JvmStatic
    fun smoothstepMixGradientUnsafe(x: Float): Float {
        val k = x - 0.5f
        return 1.5f - k * k * 6f
    }

    @JvmStatic
    fun smoothstepMixGradientUnsafe(x: Double): Double {
        val k = x - 0.5
        return 1.5 - k * k * 6.0
    }

    @JvmStatic
    fun smoothstepMix(a: Float, b: Float, x: Float): Float {
        return when {
            x <= 0f -> a
            x < 1f -> mix(a, b, smoothstepFactorUnsafe(x))
            else -> b
        }
    }

    @JvmStatic
    fun smoothstepMixUnsafe(a: Float, b: Float, x: Float): Float {
        return mix(a, b, smoothstepFactorUnsafe(x))
    }

    @JvmStatic
    fun smoothstep(edgeA: Float, edgeB: Float, x: Float): Float {
        return if (edgeA < edgeB) {
            val f = unmix(edgeA, edgeB, x)
             smoothstepFactor(f)
        } else 0f
    }

    @JvmStatic
    fun smoothstep(edgeA: Double, edgeB: Double, x: Double): Double {
        return if (edgeA < edgeB) {
            val f = unmix(edgeA, edgeB, x)
            smoothstepFactor(f)
        } else 0.0
    }
}