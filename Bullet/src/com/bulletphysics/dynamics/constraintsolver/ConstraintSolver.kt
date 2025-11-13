package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.collision.broadphase.Dispatcher
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.linearmath.IDebugDraw

/**
 * @author jezek2
 */
interface ConstraintSolver {
    /**
     * Solve a group of constraints.
     */
    fun solveGroup(
        bodies: List<CollisionObject>, numBodies: Int,
        manifold: List<PersistentManifold>, manifoldOffset: Int, numManifolds: Int,
        constraints: List<TypedConstraint>, constraintsOffset: Int, numConstraints: Int,
        info: ContactSolverInfo, debugDrawer: IDebugDraw?, dispatcher: Dispatcher
    )

    /**
     * Clear internal cached data and reset random seed.
     */
    fun reset()
}
