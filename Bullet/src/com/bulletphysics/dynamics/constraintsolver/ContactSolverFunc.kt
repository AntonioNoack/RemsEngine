package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.collision.narrowphase.ManifoldPoint
import com.bulletphysics.dynamics.RigidBody

/**
 * Contact solving function.
 *
 * @author jezek2
 */
fun interface ContactSolverFunc {
    fun resolveContact(
        body1: RigidBody,
        body2: RigidBody,
        contactPoint: ManifoldPoint,
        info: ContactSolverInfo
    ): Double
}
