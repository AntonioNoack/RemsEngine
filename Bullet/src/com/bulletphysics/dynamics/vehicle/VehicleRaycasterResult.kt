package com.bulletphysics.dynamics.vehicle

import org.joml.Vector3d

/**
 * Vehicle raycaster result.
 *
 * @author jezek2
 */
class VehicleRaycasterResult {
    val hitPointInWorld: Vector3d = Vector3d()
    val hitNormalInWorld: Vector3d = Vector3d()
    var distFraction: Double = -1.0
}
