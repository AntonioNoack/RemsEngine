package com.bulletphysics.dynamics.vehicle

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
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
) : InterpolatedTransform(time1) {

    // set by raycaster
    val contactNormalWS = Vector3f()
    val contactPointWS = Vector3d() // raycast hit point

    var suspensionLength = config.suspensionRestLength

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
    val radius get() = config.radius
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

    /**
     * value is used for dampening
     * */
    var suspensionRelativeVelocity = 0f
    var suspensionForce = 0f

    var skidInfo
        get() = config.skidInfo
        set(value) {
            config.skidInfo = value
        }

    val forwardWS = Vector3f()
    val axle = Vector3f()
    var forwardImpulse = 0f
    var sideImpulse = 0f

}
