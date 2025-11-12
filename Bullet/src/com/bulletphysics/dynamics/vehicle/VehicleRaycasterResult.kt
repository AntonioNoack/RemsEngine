package com.bulletphysics.dynamics.vehicle

import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Vehicle raycaster result.
 *
 * @author jezek2
 */
class VehicleRaycasterResult {
    val hitPointInWorld = Vector3d()
    val hitNormalInWorld = Vector3f()
    var distFraction = -1f
}
