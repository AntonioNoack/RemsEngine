package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.TransformUtil
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * SolverBody is an internal data structure for the constraint solver. Only necessary
 * data is packed to increase cache coherence/performance.
 *
 * @author jezek2
 */
class SolverBody {

    @JvmField
    val angularVelocity = Vector3f()

    @JvmField
    val angularFactor = Vector3f()

    @JvmField
    var invMass = 0f

    @JvmField
    var friction = 0f

    @JvmField
    var originalBody: RigidBody? = null

    @JvmField
    val linearVelocity = Vector3f()

    @JvmField
    val centerOfMassPosition = Vector3d()

    @JvmField
    val pushVelocity = Vector3f()

    @JvmField
    val turnVelocity = Vector3f()

    /**
     * Optimization for the iterative solver: avoid calculating constant terms involving inertia, normal, relative position.
     */
    fun internalApplyImpulse(linearComponent: Vector3f, angularComponent: Vector3f, impulseMagnitude: Float) {
        if (invMass != 0f) {
            linearVelocity.fma(impulseMagnitude, linearComponent)
            val angularFactor = angularFactor
            angularVelocity.add(
                impulseMagnitude * angularFactor.x * angularComponent.x,
                impulseMagnitude * angularFactor.y * angularComponent.y,
                impulseMagnitude * angularFactor.z * angularComponent.z,
            )
        }
    }

    fun writebackVelocity() {
        if (invMass != 0f) {
            val originalBody = originalBody ?: return
            originalBody.linearVelocity.set(linearVelocity)
            originalBody.angularVelocity.set(angularVelocity)
        }
    }

    fun writebackVelocity(timeStep: Float) {
        if (invMass != 0f) {
            val originalBody = originalBody ?: return
            originalBody.linearVelocity.set(linearVelocity)
            originalBody.angularVelocity.set(angularVelocity)

            // correct the position/orientation based on push/turn recovery
            val newTransform = Stack.newTrans()
            val curTrans = originalBody.worldTransform
            TransformUtil.integrateTransform(curTrans, pushVelocity, turnVelocity, timeStep, newTransform)
            originalBody.setWorldTransform(newTransform)
            Stack.subTrans(1)
        }
    }

    fun readVelocity() {
        if (invMass != 0f) {
            val originalBody = originalBody ?: return
            linearVelocity.set(originalBody.linearVelocity)
            angularVelocity.set(originalBody.angularVelocity)
        }
    }
}
