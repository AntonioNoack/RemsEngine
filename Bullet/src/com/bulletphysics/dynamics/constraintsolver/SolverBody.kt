package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.TransformUtil
import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * SolverBody is an internal data structure for the constraint solver. Only necessary
 * data is packed to increase cache coherence/performance.
 *
 * @author jezek2
 */
class SolverBody {

    @JvmField
    val angularVelocity = Vector3d()

    @JvmField
    val angularFactor = Vector3d()

    @JvmField
    var invMass: Double = 0.0

    @JvmField
    var friction: Double = 0.0

    @JvmField
    var originalBody: RigidBody? = null

    @JvmField
    val linearVelocity: Vector3d = Vector3d()

    @JvmField
    val centerOfMassPosition: Vector3d = Vector3d()

    @JvmField
    val pushVelocity: Vector3d = Vector3d()

    @JvmField
    val turnVelocity: Vector3d = Vector3d()

    fun getVelocityInLocalPoint(relPos: Vector3d, velocity: Vector3d) {
        val tmp = Stack.newVec()
        angularVelocity.cross(relPos, tmp)
        linearVelocity.add(tmp, velocity)
    }

    /**
     * Optimization for the iterative solver: avoid calculating constant terms involving inertia, normal, relative position.
     */
    fun internalApplyImpulse(linearComponent: Vector3d, angularComponent: Vector3d, impulseMagnitude: Double) {
        if (invMass != 0.0) {
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
        if (invMass != 0.0) {
            val originalBody = originalBody ?: return
            originalBody.setLinearVelocity(linearVelocity)
            originalBody.setAngularVelocity(angularVelocity)
            //m_originalBody->setCompanionId(-1);
        }
    }

    fun writebackVelocity(timeStep: Double) {
        if (invMass != 0.0) {
            val originalBody = originalBody ?: return
            originalBody.setLinearVelocity(linearVelocity)
            originalBody.setAngularVelocity(angularVelocity)

            // correct the position/orientation based on push/turn recovery
            val newTransform = Stack.newTrans()
            val curTrans = originalBody.worldTransform
            TransformUtil.integrateTransform(curTrans, pushVelocity, turnVelocity, timeStep, newTransform)
            originalBody.setWorldTransform(newTransform)
            Stack.subTrans(1)
            //m_originalBody->setCompanionId(-1);
        }
    }

    fun readVelocity() {
        if (invMass != 0.0) {
            val originalBody = originalBody ?: return
            linearVelocity.set(originalBody.linearVelocity)
            angularVelocity.set(originalBody.angularVelocity)
        }
    }
}
