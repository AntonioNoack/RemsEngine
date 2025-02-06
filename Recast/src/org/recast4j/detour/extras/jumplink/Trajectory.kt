package org.recast4j.detour.extras.jumplink

import org.joml.Vector3f

interface Trajectory {
    fun apply(start: Vector3f, end: Vector3f, u: Float): Vector3f
}