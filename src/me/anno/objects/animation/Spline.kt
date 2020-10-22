package me.anno.objects.animation

import me.anno.utils.clamp
import org.joml.Vector4d
import kotlin.math.min

object Spline {

    fun getWeights(

        f0: Keyframe<*>,
        f1: Keyframe<*>,
        f2: Keyframe<*>,
        f3: Keyframe<*>,
        t0: Double

    ): Vector4d {

        // todo calculate the left and right control points...
        // todo first maybe an approximation by being completely linear

        return when (f1.interpolation) {
            Interpolation.SPLINE -> {

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

                Vector4d(w0, w1, w2, w3)

            }
            Interpolation.LINEAR_BOUNDED -> {
                val f = clamp(t0, 0.0, 1.0)
                Vector4d(0.0, 1.0 - f, f, 0.0)
            }
            Interpolation.LINEAR_UNBOUNDED -> {
                Vector4d(0.0, 1.0 - t0, t0, 0.0)
            }
            Interpolation.STEP -> {
                if(t0 >= 0.5){
                    Vector4d(0.0, 0.0, 1.0, 0.0)
                } else {
                    Vector4d(0.0, 1.0, 0.0, 0.0)
                }
            }
        }

    }

}
