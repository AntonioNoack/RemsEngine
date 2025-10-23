package org.recast4j.detour.extras.jumplink

import me.anno.maths.Maths.mix
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.min

object ClimbTrajectory : Trajectory {
    override fun apply(start: Vector3f, end: Vector3f, u: Float): Vector3f {
        val fxz = min(2f * u, 1f)
        val fy = max(0f, 2f * u - 1f)
        return Vector3f(
            mix(start.x, end.x, fxz),
            mix(start.y, end.y, fy),
            mix(start.z, end.z, fxz)
        )
    }
}