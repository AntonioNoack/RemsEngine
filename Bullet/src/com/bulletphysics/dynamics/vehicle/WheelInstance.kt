package com.bulletphysics.dynamics.vehicle

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.bullet.bodies.VehicleWheel
import me.anno.ecs.components.physics.InterpolatedTransform
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Original @author jezek2 (WheelInfo)
 */
class WheelInstance(
    val config: VehicleWheel,
    // cs = chassis space
    val connectionPointCS: Vector3f,
    val directionCS: Vector3f,
    val axleCS: Vector3f,
    time1: Long
): InterpolatedTransform(time1) {

    // set by raycaster
    val contactNormalWS = Vector3f()
    val contactPointWS = Vector3d() // raycast hit point

    var suspensionLength = 0f

    val connectionPointWS = Vector3d() // raycast starting point

    val directionWS = Vector3f() // direction in worldSpace
    val axleWS = Vector3f() // axle in worldSpace

    var isInContact = false
    var groundObject: RigidBody? = null

    val worldTransform = Transform()

    val suspensionRestLength get() = config.suspensionRestLength
    val suspensionStiffness get() = config.suspensionStiffness
    val suspensionDampingCompression get() = config.suspensionDampingCompression
    val suspensionDampingRelaxation get() = config.suspensionDampingRelaxation
    val maxSuspensionTravel get() = config.maxSuspensionTravel
    val wheelRadius get() = config.radius
    val frictionSlip get() = config.frictionSlip
    val steering get() = config.steering

    var rotation: Double
        get() = config.rotation
        set(value) {
            config.rotation = value
        }

    var deltaRotation = 0f

    val rollInfluence get() = config.rollInfluence
    val engineForce get() = config.engineForce
    val brakeForce get() = config.brakeForce

    var clippedInvContactDotSuspension = 0f

    var suspensionRelativeVelocity = 0f
    var wheelsSuspensionForce = 0f

    var skidInfo = 0f

    val forwardWS = Vector3f()
    val axle = Vector3f()
    var forwardImpulse = 0f
    var sideImpulse = 0f

    @Suppress("unused")
    fun updateWheel(chassis: RigidBody) {
        if (isInContact) {
            val project = contactNormalWS.dot(directionWS)
            val chassisVelocityAtContactPoint = Stack.newVec3f()
            val relPos = Stack.newVec3f()
            contactPointWS.sub(chassis.worldTransform.origin, relPos)
            chassis.getVelocityInLocalPoint(relPos, chassisVelocityAtContactPoint)
            val projVel = contactNormalWS.dot(chassisVelocityAtContactPoint)
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
            suspensionLength = this.suspensionRestLength
            suspensionRelativeVelocity = 0f
            directionWS.negate(contactNormalWS)
            clippedInvContactDotSuspension = 1f
        }
    }
}
