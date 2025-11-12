package com.bulletphysics.dynamics.constraintsolver

import org.joml.Vector3f

/**
 * Stores some extra information to each contact point. It is not in the contact point,
 * because that want to keep the collision detection independent of the constraint solver.
 *
 * @author jezek2
 */
class ConstraintPersistentData {
    /**
     * total applied impulse during most recent frame
     */
    @JvmField
    var appliedImpulse = 0f
    @JvmField
    var prevAppliedImpulse = 0f
    @JvmField
    var accumulatedTangentImpulse0 = 0f
    @JvmField
    var accumulatedTangentImpulse1 = 0f

    @JvmField
    var jacDiagABInv = 0f
    @JvmField
    var jacDiagABInvTangent0 = 0f
    @JvmField
    var jacDiagABInvTangent1 = 0f
    @JvmField
    var persistentLifeTime = 0
    @JvmField
    var restitution = 0f
    @JvmField
    var friction = 0f
    @JvmField
    var penetration = 0f
    @JvmField
    val frictionWorldTangential0 = Vector3f()
    @JvmField
    val frictionWorldTangential1 = Vector3f()

    @JvmField
    val frictionAngularComponent0A = Vector3f()
    @JvmField
    val frictionAngularComponent0B = Vector3f()
    @JvmField
    val frictionAngularComponent1A = Vector3f()
    @JvmField
    val frictionAngularComponent1B = Vector3f()

    @JvmField
    val angularComponentA = Vector3f()
    @JvmField
    val angularComponentB = Vector3f()

    @JvmField
    var contactSolverFunc: ContactSolverFunc? = null
    @JvmField
    var frictionSolverFunc: ContactSolverFunc? = null

    fun reset() {
        appliedImpulse = 0f
        prevAppliedImpulse = 0f
        accumulatedTangentImpulse0 = 0f
        accumulatedTangentImpulse1 = 0f

        jacDiagABInv = 0f
        jacDiagABInvTangent0 = 0f
        jacDiagABInvTangent1 = 0f
        persistentLifeTime = 0
        restitution = 0f
        friction = 0f
        penetration = 0f

        frictionWorldTangential0.set(0.0)
        frictionWorldTangential1.set(0.0)

        frictionAngularComponent0A.set(0.0)
        frictionAngularComponent0B.set(0.0)
        frictionAngularComponent1A.set(0.0)
        frictionAngularComponent1B.set(0.0)

        angularComponentA.set(0.0)
        angularComponentB.set(0.0)

        contactSolverFunc = null
        frictionSolverFunc = null
    }
}
