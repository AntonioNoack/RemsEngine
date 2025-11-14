package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.dynamics.RigidBody

/**
 * TypedConstraint is the base class for Bullet constraints and vehicles.
 *
 * @author jezek2
 */
abstract class TypedConstraint(
    val rigidBodyA: RigidBody,
    val rigidBodyB: RigidBody,
) {

    constructor() : this(FIXED, FIXED)

    fun activate() {
        rigidBodyA.activate()
        rigidBodyB.activate()
    }

    abstract fun buildJacobian()
    abstract fun solveConstraint(timeStep: Float)

    abstract var breakingImpulse: Float

    var isBroken: Boolean
        get() = breakingImpulse < 0
        set(value) {
            if (value != isBroken) {
                breakingImpulse = -breakingImpulse
            }
        }

    companion object {
        private val FIXED = RigidBody(0f, SphereShape(0f))
    }
}
