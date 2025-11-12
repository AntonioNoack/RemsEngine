package com.bulletphysics.collision.narrowphase

import org.joml.Vector3d
import org.joml.Vector3f

/**
 * RayResult stores the closest result. Alternatively, add a callback method
 * to decide about closest/all results.
 */
class CastResult {
    @JvmField
    val normal = Vector3f()

    @JvmField
    val hitPoint = Vector3d()

    @JvmField
    var fraction = 1e38f // input and output

    @JvmField
    var allowedPenetration = 0f

    fun init() {
        fraction = 1e38f
        allowedPenetration = 0f
    }
}