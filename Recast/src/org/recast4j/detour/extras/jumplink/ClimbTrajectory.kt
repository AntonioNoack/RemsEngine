package org.recast4j.detour.extras.jumplink

import org.joml.Vector3f
import org.recast4j.Vectors.lerp

object ClimbTrajectory : Trajectory {
    override fun apply(start: Vector3f, end: Vector3f, u: Float): Vector3f {
        return Vector3f(
            lerp(start.x, end.x, Math.min(2f * u, 1f)),
            lerp(start.y, end.y, Math.max(0f, 2f * u - 1f)),
            lerp(start.z, end.z, Math.min(2f * u, 1f))
        )
    }
}