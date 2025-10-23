package org.recast4j.detour.extras.jumplink

import me.anno.maths.Maths.mix
import org.joml.Vector3f
import kotlin.math.sqrt

class JumpTrajectory(private val jumpHeight: Float) : Trajectory {
    override fun apply(start: Vector3f, end: Vector3f, u: Float): Vector3f {
        return Vector3f(
            mix(start.x, end.x, u),
            interpolateHeight(start.y, end.y, u),
            mix(start.z, end.z, u)
        )
    }

    private fun interpolateHeight(startY: Float, endY: Float, u: Float): Float {
        if (u == 0f) {
            return startY
        } else if (u == 1f) {
            return endY
        }
        val h1: Float
        val h2: Float
        if (startY >= endY) { // jump down
            h1 = jumpHeight
            h2 = jumpHeight + startY - endY
        } else { // jump up
            h1 = jumpHeight + startY - endY
            h2 = jumpHeight
        }
        val t = sqrt(h1) / (sqrt(h2) + sqrt(h1))
        return if (u <= t) {
            val v = 1f - u / t
            startY + h1 - h1 * v * v
        } else {
            val v = (u - t) / (1f - t)
            startY + h1 - h2 * v * v
        }
    }
}