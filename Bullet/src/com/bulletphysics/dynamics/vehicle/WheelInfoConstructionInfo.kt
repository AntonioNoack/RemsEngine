package com.bulletphysics.dynamics.vehicle

import org.joml.Vector3f

/**
 *
 * @author jezek2
 */
class WheelInfoConstructionInfo {
    val chassisConnectionCS = Vector3f()
    val wheelDirectionCS = Vector3f()
    val wheelAxleCS = Vector3f()
    var suspensionRestLength = 0f
    var maxSuspensionTravel = 0f
    var wheelRadius = 0f

    var suspensionStiffness = 0f
    var wheelsDampingCompression = 0f
    var wheelsDampingRelaxation = 0f
    var frictionSlip = 0f
}
