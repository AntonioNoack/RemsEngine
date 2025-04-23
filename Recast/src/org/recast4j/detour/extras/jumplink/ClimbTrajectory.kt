package org.recast4j.detour.extras.jumplink

import org.joml.Vector3f
import org.recast4j.Vectors.lerp
import kotlin.math.max
import kotlin.math.min

object ClimbTrajectory : Trajectory {
    override fun apply(start: Vector3f, end: Vector3f, u: Float): Vector3f {
        return Vector3f(
            lerp(start.x, end.x, min(2f * u, 1f)),
            lerp(start.y, end.y, max(0f, 2f * u - 1f)),
            lerp(start.z, end.z, min(2f * u, 1f))
        )
    }
}