package me.anno.objects.animation

import org.joml.Vector4d
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

object Spline {

    fun getWeights(

        f0: Keyframe<*>,
        f1: Keyframe<*>,
        f2: Keyframe<*>,
        f3: Keyframe<*>,
        t0: Double

    ): Vector4d {
        val interpolation = (if(t0 > 1.0) f2 else f1).interpolation
        return interpolation.getWeights(
            f0, f1, f2, f3, t0
        )
    }

}
