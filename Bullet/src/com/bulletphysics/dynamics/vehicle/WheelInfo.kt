package com.bulletphysics.dynamics.vehicle

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setNegate
import com.bulletphysics.util.setSub

/**
 * WheelInfo contains information per wheel about friction and suspension.
 *
 * @author jezek2
 */
class WheelInfo(ci: WheelInfoConstructionInfo) {
    @JvmField
    val raycastInfo: RaycastInfo = RaycastInfo()

    @JvmField
    val worldTransform: Transform = Transform()

    @JvmField
    val chassisConnectionPointCS: Vector3d = Vector3d()

    @JvmField
    val wheelDirectionCS: Vector3d = Vector3d()

    @JvmField
    val wheelAxleCS: Vector3d = Vector3d() // const or modified by steering

    @JvmField
    var suspensionRestLength: Double = ci.suspensionRestLength

    @JvmField
    var maxSuspensionTravelCm: Double = ci.maxSuspensionTravelCm

    @JvmField
    var wheelRadius: Double = ci.wheelRadius

    @JvmField
    var suspensionStiffness: Double = ci.suspensionStiffness

    @JvmField
    var wheelDampingCompression: Double = ci.wheelsDampingCompression

    @JvmField
    var wheelDampingRelaxation: Double = ci.wheelsDampingRelaxation

    @JvmField
    var frictionSlip: Double = ci.frictionSlip

    @JvmField
    var steering: Double = 0.0

    @JvmField
    var rotation: Double = 0.0

    @JvmField
    var deltaRotation: Double = 0.0

    @JvmField
    var rollInfluence: Double = 0.1

    @JvmField
    var engineForce: Double = 0.0

    @JvmField
    var brake: Double = 0.0

    var bIsFrontWheel: Boolean

    var clientInfo: Any? = null // can be used to store pointer to sync transforms...

    @JvmField
    var clippedInvContactDotSuspension: Double = 0.0

    @JvmField
    var suspensionRelativeVelocity: Double = 0.0

    // calculated by suspension
    @JvmField
    var wheelsSuspensionForce: Double = 0.0

    @JvmField
    var skidInfo: Double = 0.0

    init {
        chassisConnectionPointCS.set(ci.chassisConnectionCS)
        wheelDirectionCS.set(ci.wheelDirectionCS)
        wheelAxleCS.set(ci.wheelAxleCS)
        bIsFrontWheel = ci.bIsFrontWheel
    }

    @Suppress("unused")
    fun updateWheel(chassis: RigidBody, raycastInfo: RaycastInfo) {
        if (raycastInfo.isInContact) {
            val project = raycastInfo.contactNormalWS.dot(raycastInfo.wheelDirectionWS)
            val chassisVelocityAtContactPoint = Stack.newVec()
            val relPos = Stack.newVec()
            relPos.setSub(raycastInfo.contactPointWS, chassis.getCenterOfMassPosition(Stack.newVec()))
            chassis.getVelocityInLocalPoint(relPos, chassisVelocityAtContactPoint)
            val projVel = raycastInfo.contactNormalWS.dot(chassisVelocityAtContactPoint)
            if (project >= -0.1) {
                suspensionRelativeVelocity = 0.0
                clippedInvContactDotSuspension = 1.0 / 0.1
            } else {
                val inv = -1.0 / project
                suspensionRelativeVelocity = projVel * inv
                clippedInvContactDotSuspension = inv
            }
        } else {
            // Not in contact : position wheel in a nice (rest length) position
            raycastInfo.suspensionLength = this.suspensionRestLength
            suspensionRelativeVelocity = 0.0
            raycastInfo.contactNormalWS.setNegate(raycastInfo.wheelDirectionWS)
            clippedInvContactDotSuspension = 1.0
        }
    }

    class RaycastInfo {
        // set by raycaster
        @JvmField
        val contactNormalWS: Vector3d = Vector3d() // contactnormal

        @JvmField
        val contactPointWS: Vector3d = Vector3d() // raycast hitpoint

        @JvmField
        var suspensionLength: Double = 0.0

        @JvmField
        val hardPointWS: Vector3d = Vector3d() // raycast starting point

        @JvmField
        val wheelDirectionWS: Vector3d = Vector3d() // direction in worldspace

        @JvmField
        val wheelAxleWS: Vector3d = Vector3d() // axle in worldspace

        @JvmField
        var isInContact: Boolean = false

        @JvmField
        var groundObject: RigidBody? = null
    }
}
