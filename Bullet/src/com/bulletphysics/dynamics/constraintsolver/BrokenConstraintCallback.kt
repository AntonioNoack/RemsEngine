package com.bulletphysics.dynamics.constraintsolver

/**
 * Callback called for when constraints break.
 */
interface BrokenConstraintCallback {
    fun onBrokenConstraint(constraint: TypedConstraint)
}
