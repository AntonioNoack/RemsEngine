package com.bulletphysics.collision.narrowphase

import org.joml.Vector3d

/**
 * RayResult stores the closest result. Alternatively, add a callback method
 * to decide about closest/all results.
 */
class CastResult {
    @JvmField
    val normal: Vector3d = Vector3d()

    @JvmField
    val hitPoint: Vector3d = Vector3d()

    @JvmField
    var fraction: Double = 1e308 // input and output

    @JvmField
    var allowedPenetration: Double = 0.0

    fun debugDraw(fraction: Double) {
    }

    fun init() {
        fraction = 1e308
        allowedPenetration = 0.0
    }
}