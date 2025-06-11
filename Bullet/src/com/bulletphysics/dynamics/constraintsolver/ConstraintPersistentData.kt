package com.bulletphysics.dynamics.constraintsolver

import org.joml.Vector3d

/**
 * Stores some extra information to each contact point. It is not in the contact
 * point, because that want to keep the collision detection independent from the
 * constraint solver.
 *
 * @author jezek2
 */
class ConstraintPersistentData {
    /**
     * total applied impulse during most recent frame
     */
    @JvmField
    var appliedImpulse: Double = 0.0
    @JvmField
    var prevAppliedImpulse: Double = 0.0
    @JvmField
    var accumulatedTangentImpulse0: Double = 0.0
    @JvmField
    var accumulatedTangentImpulse1: Double = 0.0

    @JvmField
    var jacDiagABInv: Double = 0.0
    @JvmField
    var jacDiagABInvTangent0: Double = 0.0
    @JvmField
    var jacDiagABInvTangent1: Double = 0.0
    @JvmField
    var persistentLifeTime: Int = 0
    @JvmField
    var restitution: Double = 0.0
    @JvmField
    var friction: Double = 0.0
    @JvmField
    var penetration: Double = 0.0
    @JvmField
    val frictionWorldTangential0 = Vector3d()
    @JvmField
    val frictionWorldTangential1 = Vector3d()

    @JvmField
    val frictionAngularComponent0A = Vector3d()
    @JvmField
    val frictionAngularComponent0B = Vector3d()
    @JvmField
    val frictionAngularComponent1A = Vector3d()
    @JvmField
    val frictionAngularComponent1B = Vector3d()

    @JvmField
    val angularComponentA = Vector3d()
    @JvmField
    val angularComponentB = Vector3d()

    @JvmField
    var contactSolverFunc: ContactSolverFunc? = null
    @JvmField
    var frictionSolverFunc: ContactSolverFunc? = null

    fun reset() {
        appliedImpulse = 0.0
        prevAppliedImpulse = 0.0
        accumulatedTangentImpulse0 = 0.0
        accumulatedTangentImpulse1 = 0.0

        jacDiagABInv = 0.0
        jacDiagABInvTangent0 = 0.0
        jacDiagABInvTangent1 = 0.0
        persistentLifeTime = 0
        restitution = 0.0
        friction = 0.0
        penetration = 0.0

        frictionWorldTangential0.set(0.0, 0.0, 0.0)
        frictionWorldTangential1.set(0.0, 0.0, 0.0)

        frictionAngularComponent0A.set(0.0, 0.0, 0.0)
        frictionAngularComponent0B.set(0.0, 0.0, 0.0)
        frictionAngularComponent1A.set(0.0, 0.0, 0.0)
        frictionAngularComponent1B.set(0.0, 0.0, 0.0)

        angularComponentA.set(0.0, 0.0, 0.0)
        angularComponentB.set(0.0, 0.0, 0.0)

        contactSolverFunc = null
        frictionSolverFunc = null
    }
}
