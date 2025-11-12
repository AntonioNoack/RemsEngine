package com.bulletphysics.dynamics.vehicle

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * WheelInfo contains information per wheel about friction and suspension.
 *
 * @author jezek2
 */
class WheelInfo(ci: WheelInfoConstructionInfo) {
    @JvmField
    val raycastInfo = RaycastInfo()

    @JvmField
    val worldTransform = Transform()

    @JvmField
    val chassisConnectionPointCS = Vector3f()

    @JvmField
    val wheelDirectionCS = Vector3f()

    @JvmField
    val wheelAxleCS = Vector3f() // const or modified by steering

    @JvmField
    var suspensionRestLength = ci.suspensionRestLength

    @JvmField
    var maxSuspensionTravel = ci.maxSuspensionTravel

    @JvmField
    var wheelRadius = ci.wheelRadius

    @JvmField
    var suspensionStiffness = ci.suspensionStiffness

    @JvmField
    var wheelDampingCompression = ci.wheelsDampingCompression

    @JvmField
    var wheelDampingRelaxation = ci.wheelsDampingRelaxation

    @JvmField
    var frictionSlip = ci.frictionSlip

    @JvmField
    var steering = 0f

    @JvmField
    var rotation = 0.0

    @JvmField
    var deltaRotation = 0f

    @JvmField
    var rollInfluence = 0.1f

    @JvmField
    var engineForce = 0f

    @JvmField
    var brake = 0f

    // set to me.anno.bullet.bodies.VehicleWheel
    var clientInfo: Any? = null // can be used to store pointer to sync transforms...

    @JvmField
    var clippedInvContactDotSuspension = 0f

    @JvmField
    var suspensionRelativeVelocity = 0f

    // calculated by suspension
    @JvmField
    var wheelsSuspensionForce = 0f

    @JvmField
    var skidInfo = 0f

    init {
        chassisConnectionPointCS.set(ci.chassisConnectionCS)
        wheelDirectionCS.set(ci.wheelDirectionCS)
        wheelAxleCS.set(ci.wheelAxleCS)
    }

    @Suppress("unused")
    fun updateWheel(chassis: RigidBody, raycastInfo: RaycastInfo) {
        if (raycastInfo.isInContact) {
            val project = raycastInfo.contactNormalWS.dot(raycastInfo.wheelDirectionWS)
            val chassisVelocityAtContactPoint = Stack.newVec3f()
            val relPos = Stack.newVec3f()
            raycastInfo.contactPointWS.sub(chassis.worldTransform.origin, relPos)
            chassis.getVelocityInLocalPoint(relPos, chassisVelocityAtContactPoint)
            val projVel = raycastInfo.contactNormalWS.dot(chassisVelocityAtContactPoint)
            if (project >= -0.1f) {
                suspensionRelativeVelocity = 0f
                clippedInvContactDotSuspension = 1f / 0.1f
            } else {
                val inv = -1f / project
                suspensionRelativeVelocity = projVel * inv
                clippedInvContactDotSuspension = inv
            }
            Stack.subVec3f(2)
        } else {
            // Not in contact : position wheel in a nice (rest length) position
            raycastInfo.suspensionLength = this.suspensionRestLength
            suspensionRelativeVelocity = 0f
            raycastInfo.wheelDirectionWS.negate(raycastInfo.contactNormalWS)
            clippedInvContactDotSuspension = 1f
        }
    }

    class RaycastInfo {
        // set by raycaster
        @JvmField
        val contactNormalWS = Vector3f()

        @JvmField
        val contactPointWS = Vector3d() // raycast hit point

        @JvmField
        var suspensionLength = 0f

        @JvmField
        val hardPointWS = Vector3d() // raycast starting point

        @JvmField
        val wheelDirectionWS = Vector3f() // direction in worldSpace

        @JvmField
        val wheelAxleWS = Vector3f() // axle in worldSpace

        @JvmField
        var isInContact = false

        @JvmField
        var groundObject: RigidBody? = null
    }
}
