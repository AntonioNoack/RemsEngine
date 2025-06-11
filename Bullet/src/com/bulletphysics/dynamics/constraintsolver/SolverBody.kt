package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.TransformUtil
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setCross
import com.bulletphysics.util.setScaleAdd

/**
 * SolverBody is an internal data structure for the constraint solver. Only necessary
 * data is packed to increase cache coherence/performance.
 *
 * @author jezek2
 */
class SolverBody {

	@JvmField
	val angularVelocity: Vector3d = Vector3d()
    @JvmField
	var angularFactor: Double = 0.0
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
        tmp.setCross(angularVelocity, relPos)
        velocity.setAdd(linearVelocity, tmp)
    }

    /**
     * Optimization for the iterative solver: avoid calculating constant terms involving inertia, normal, relative position.
     */
    fun internalApplyImpulse(linearComponent: Vector3d, angularComponent: Vector3d, impulseMagnitude: Double) {
        if (invMass != 0.0) {
            linearVelocity.setScaleAdd(impulseMagnitude, linearComponent, linearVelocity)
            angularVelocity.setScaleAdd(impulseMagnitude * angularFactor, angularComponent, angularVelocity)
        }
    }

    fun internalApplyPushImpulse(linearComponent: Vector3d, angularComponent: Vector3d, impulseMagnitude: Double) {
        if (invMass != 0.0) {
            pushVelocity.setScaleAdd(impulseMagnitude, linearComponent, pushVelocity)
            turnVelocity.setScaleAdd(impulseMagnitude * angularFactor, angularComponent, turnVelocity)
        }
    }

    fun writebackVelocity() {
        if (invMass != 0.0) {
            val originalBody = originalBody!!
            originalBody.setLinearVelocity(linearVelocity)
            originalBody.setAngularVelocity(angularVelocity)
            //m_originalBody->setCompanionId(-1);
        }
    }

    fun writebackVelocity(timeStep: Double) {
        if (invMass != 0.0) {
            val originalBody = originalBody!!
            originalBody.setLinearVelocity(linearVelocity)
            originalBody.setAngularVelocity(angularVelocity)

            // correct the position/orientation based on push/turn recovery
            val newTransform = Stack.newTrans()
            val curTrans = originalBody.getWorldTransform(Stack.newTrans())
            TransformUtil.integrateTransform(curTrans, pushVelocity, turnVelocity, timeStep, newTransform)
            originalBody.setWorldTransform(newTransform)

            Stack.subTrans(2)
            //m_originalBody->setCompanionId(-1);
        }
    }

    fun readVelocity() {
        if (invMass != 0.0) {
            val originalBody = originalBody!!
            originalBody.getLinearVelocity(linearVelocity)
            originalBody.getAngularVelocity(angularVelocity)
        }
    }
}
