package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.dynamics.RigidBody
import org.joml.Vector3d

/**
 * TypedConstraint is the base class for Bullet constraints and vehicles.
 *
 * @author jezek2
 */
abstract class TypedConstraint @JvmOverloads constructor(
    @JvmField var rigidBodyA: RigidBody = FIXED,
    @JvmField var rigidBodyB: RigidBody = FIXED
) {

    @JvmField
    var appliedImpulse: Double = 0.0

    @JvmField
    var breakingImpulseThreshold: Double = 1e308

    init {
        FIXED.setMassProps(0.0, Vector3d(0.0, 0.0, 0.0))
    }

    abstract fun buildJacobian()
    abstract fun solveConstraint(timeStep: Double)

    var isBroken: Boolean
        get() = breakingImpulseThreshold < 0
        set(value) {
            if (value != isBroken) {
                breakingImpulseThreshold = -breakingImpulseThreshold
            }
        }

    companion object {
        private val FIXED = RigidBody(0.0, SphereShape(0.0))
    }
}
