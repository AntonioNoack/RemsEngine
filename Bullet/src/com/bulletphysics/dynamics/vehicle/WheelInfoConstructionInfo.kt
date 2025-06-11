package com.bulletphysics.dynamics.vehicle

import org.joml.Vector3d

/**
 *
 * @author jezek2
 */
class WheelInfoConstructionInfo {
    val chassisConnectionCS: Vector3d = Vector3d()
    val wheelDirectionCS: Vector3d = Vector3d()
    val wheelAxleCS: Vector3d = Vector3d()
    var suspensionRestLength: Double = 0.0
    var maxSuspensionTravelCm: Double = 0.0
    var wheelRadius: Double = 0.0

    var suspensionStiffness: Double = 0.0
    var wheelsDampingCompression: Double = 0.0
    var wheelsDampingRelaxation: Double = 0.0
    var frictionSlip: Double = 0.0
    var bIsFrontWheel: Boolean = false
}
