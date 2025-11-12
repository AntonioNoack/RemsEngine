package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.dynamics.RigidBody
import org.joml.Vector3d
import org.joml.Vector3f

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
    var appliedImpulse = 0f

    @JvmField
    var breakingImpulseThreshold = 1e38f

    init {
        FIXED.setMassProps(0f, Vector3f())
    }

    abstract fun buildJacobian()
    abstract fun solveConstraint(timeStep: Float)

    var isBroken: Boolean
        get() = breakingImpulseThreshold < 0
        set(value) {
            if (value != isBroken) {
                breakingImpulseThreshold = -breakingImpulseThreshold
            }
        }

    companion object {
        private val FIXED = RigidBody(0f, SphereShape(0f))
    }
}
