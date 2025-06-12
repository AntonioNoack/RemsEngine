package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.narrowphase.ManifoldPoint
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setCross
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setSub
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Functions for resolving contacts.
 *
 * @author jezek2
 */
object ContactConstraint {
    @JvmField
    val resolveSingleCollision: ContactSolverFunc = ContactSolverFunc { a, b, c, d ->
        resolveSingleCollision(a, b, c, d)
    }

    @JvmField
    val resolveSingleFriction: ContactSolverFunc = ContactSolverFunc { a, b, c, d ->
        resolveSingleFriction(a, b, c)
    }

    @JvmField
    val resolveSingleCollisionCombined: ContactSolverFunc = ContactSolverFunc { a, b, c, d ->
        resolveSingleCollisionCombined(a, b, c, d)
    }

    /**
     * Bilateral constraint between two dynamic objects.
     */
    fun resolveSingleBilateral(
        body1: RigidBody, pos1: Vector3d,
        body2: RigidBody, pos2: Vector3d,
        normal: Vector3d, impulse: DoubleArray
    ) {
        val normalLenSqr = normal.lengthSquared()
        assert(abs(normalLenSqr) < 1.1)
        if (normalLenSqr > 1.1) {
            impulse[0] = 0.0
            return
        }

        val jacobiansPool = ObjectPool.Companion.get(JacobianEntry::class.java)
        val tmp = Stack.newVec()

        val relPos1 = Stack.newVec()
        relPos1.setSub(pos1, body1.getCenterOfMassPosition(tmp))

        val relPos2 = Stack.newVec()
        relPos2.setSub(pos2, body2.getCenterOfMassPosition(tmp))

        //this jacobian entry could be re-used for all iterations
        val vel1 = Stack.newVec()
        body1.getVelocityInLocalPoint(relPos1, vel1)

        val vel2 = Stack.newVec()
        body2.getVelocityInLocalPoint(relPos2, vel2)

        val vel = Stack.newVec()
        vel.setSub(vel1, vel2)

        val mat1 = body1.getCenterOfMassTransform(Stack.newTrans()).basis
        mat1.transpose()

        val mat2 = body2.getCenterOfMassTransform(Stack.newTrans()).basis
        mat2.transpose()

        val jac: JacobianEntry = jacobiansPool.get()
        jac.init(
            mat1, mat2,
            relPos1, relPos2, normal,
            body1.getInvInertiaDiagLocal(Stack.newVec()), body1.inverseMass,
            body2.getInvInertiaDiagLocal(Stack.newVec()), body2.inverseMass
        )

        val jacDiagAB = jac.diagonal
        val jacDiagABInv = 1.0 / jacDiagAB

        jacobiansPool.release(jac)

        val relVel = normal.dot(vel)

        // todo: move this into proper structure
        val contactDamping = 0.2

        //#ifdef ONLY_USE_LINEAR_MASS
        //	btScalar massTerm = btScalar(1.) / (body1.getInvMass() + body2.getInvMass());
        //	impulse = - contactDamping * rel_vel * massTerm;
        //#else
        val velocityImpulse = -contactDamping * relVel * jacDiagABInv
        impulse[0] = velocityImpulse
        //#endif
    }

    /**
     * Response between two dynamic objects with friction.
     */
    fun resolveSingleCollision(
        body1: RigidBody,
        body2: RigidBody,
        contactPoint: ManifoldPoint,
        solverInfo: ContactSolverInfo
    ): Double {
        val tmpVec = Stack.newVec()

        val pos1_ = contactPoint.getPositionWorldOnA(Stack.newVec())
        val pos2_ = contactPoint.getPositionWorldOnB(Stack.newVec())
        val normal = contactPoint.normalWorldOnB

        // constant over all iterations
        val rel_pos1 = Stack.newVec()
        rel_pos1.setSub(pos1_, body1.getCenterOfMassPosition(tmpVec))

        val rel_pos2 = Stack.newVec()
        rel_pos2.setSub(pos2_, body2.getCenterOfMassPosition(tmpVec))

        val vel1 = body1.getVelocityInLocalPoint(rel_pos1, Stack.newVec())
        val vel2 = body2.getVelocityInLocalPoint(rel_pos2, Stack.newVec())
        val vel = Stack.newVec()
        vel.setSub(vel1, vel2)

        val relVel = normal.dot(vel)

        val Kfps = 1.0 / solverInfo.timeStep

        // btScalar damping = solverInfo.m_damping ;
        val Kerp = solverInfo.erp
        val Kcor = Kerp * Kfps

        val cpd: ConstraintPersistentData? = checkNotNull(contactPoint.userPersistentData as ConstraintPersistentData?)
        val distance = cpd!!.penetration
        val positionalError = Kcor * -distance
        val velocityError = cpd.restitution - relVel // * damping;

        val penetrationImpulse = positionalError * cpd.jacDiagABInv

        val velocityImpulse = velocityError * cpd.jacDiagABInv

        var normalImpulse = penetrationImpulse + velocityImpulse

        // See Erin Catto's GDC 2006 paper: Clamp the accumulated impulse
        val oldNormalImpulse = cpd.appliedImpulse
        val sum = oldNormalImpulse + normalImpulse
        cpd.appliedImpulse = max(0.0, sum)

        normalImpulse = cpd.appliedImpulse - oldNormalImpulse

        //#ifdef USE_INTERNAL_APPLY_IMPULSE
        val tmp = Stack.newVec()
        if (body1.inverseMass != 0.0) {
            tmp.setScale(body1.inverseMass, contactPoint.normalWorldOnB)
            body1.internalApplyImpulse(tmp, cpd.angularComponentA, normalImpulse)
        }
        if (body2.inverseMass != 0.0) {
            tmp.setScale(body2.inverseMass, contactPoint.normalWorldOnB)
            body2.internalApplyImpulse(tmp, cpd.angularComponentB, -normalImpulse)
        }

        //#else //USE_INTERNAL_APPLY_IMPULSE
        //	body1.applyImpulse(normal*(normalImpulse), rel_pos1);
        //	body2.applyImpulse(-normal*(normalImpulse), rel_pos2);
        //#endif //USE_INTERNAL_APPLY_IMPULSE
        return normalImpulse
    }

    fun resolveSingleFriction(
        body1: RigidBody,
        body2: RigidBody,
        contactPoint: ManifoldPoint
    ): Double {
        val tmpVec = Stack.newVec()

        val pos1 = contactPoint.getPositionWorldOnA(Stack.newVec())
        val pos2 = contactPoint.getPositionWorldOnB(Stack.newVec())

        val relPos1 = Stack.newVec()
        relPos1.setSub(pos1, body1.getCenterOfMassPosition(tmpVec))

        val relPos2 = Stack.newVec()
        relPos2.setSub(pos2, body2.getCenterOfMassPosition(tmpVec))

        val cpd: ConstraintPersistentData? = checkNotNull(contactPoint.userPersistentData as ConstraintPersistentData?)
        val combinedFriction = cpd!!.friction

        val limit = cpd.appliedImpulse * combinedFriction

        if (cpd.appliedImpulse > 0.0)  //friction
        {
            //apply friction in the 2 tangential directions

            // 1st tangent

            val vel1 = Stack.newVec()
            body1.getVelocityInLocalPoint(relPos1, vel1)

            val vel2 = Stack.newVec()
            body2.getVelocityInLocalPoint(relPos2, vel2)

            val vel = Stack.newVec()
            vel.setSub(vel1, vel2)

            var j1: Double
            var j2: Double

            run {
                val vrel = cpd.frictionWorldTangential0.dot(vel)
                // calculate j that moves us to zero relative velocity
                j1 = -vrel * cpd.jacDiagABInvTangent0
                val oldTangentImpulse = cpd.accumulatedTangentImpulse0
                cpd.accumulatedTangentImpulse0 = oldTangentImpulse + j1

                cpd.accumulatedTangentImpulse0 = min(cpd.accumulatedTangentImpulse0, limit)
                cpd.accumulatedTangentImpulse0 = max(cpd.accumulatedTangentImpulse0, -limit)
                j1 = cpd.accumulatedTangentImpulse0 - oldTangentImpulse
            }
            run {
                // 2nd tangent
                val vrel = cpd.frictionWorldTangential1.dot(vel)

                // calculate j that moves us to zero relative velocity
                j2 = -vrel * cpd.jacDiagABInvTangent1
                val oldTangentImpulse = cpd.accumulatedTangentImpulse1
                cpd.accumulatedTangentImpulse1 = oldTangentImpulse + j2

                cpd.accumulatedTangentImpulse1 = min(cpd.accumulatedTangentImpulse1, limit)
                cpd.accumulatedTangentImpulse1 = max(cpd.accumulatedTangentImpulse1, -limit)
                j2 = cpd.accumulatedTangentImpulse1 - oldTangentImpulse
            }

            //#ifdef USE_INTERNAL_APPLY_IMPULSE
            val tmp = Stack.newVec()

            if (body1.inverseMass != 0.0) {
                tmp.setScale(body1.inverseMass, cpd.frictionWorldTangential0)
                body1.internalApplyImpulse(tmp, cpd.frictionAngularComponent0A, j1)

                tmp.setScale(body1.inverseMass, cpd.frictionWorldTangential1)
                body1.internalApplyImpulse(tmp, cpd.frictionAngularComponent1A, j2)
            }
            if (body2.inverseMass != 0.0) {
                tmp.setScale(body2.inverseMass, cpd.frictionWorldTangential0)
                body2.internalApplyImpulse(tmp, cpd.frictionAngularComponent0B, -j1)

                tmp.setScale(body2.inverseMass, cpd.frictionWorldTangential1)
                body2.internalApplyImpulse(tmp, cpd.frictionAngularComponent1B, -j2)
            }
            //#else //USE_INTERNAL_APPLY_IMPULSE
            //	body1.applyImpulse((j1 * cpd->m_frictionWorldTangential0)+(j2 * cpd->m_frictionWorldTangential1), rel_pos1);
            //	body2.applyImpulse((j1 * -cpd->m_frictionWorldTangential0)+(j2 * -cpd->m_frictionWorldTangential1), rel_pos2);
            //#endif //USE_INTERNAL_APPLY_IMPULSE
        }
        return cpd.appliedImpulse
    }

    /**
     * velocity + friction<br></br>
     * response between two dynamic objects with friction
     */
    @JvmStatic
    fun resolveSingleCollisionCombined(
        body1: RigidBody,
        body2: RigidBody,
        contactPoint: ManifoldPoint,
        solverInfo: ContactSolverInfo
    ): Double {

        val pos1 = contactPoint.positionWorldOnA
        val pos2 = contactPoint.positionWorldOnB
        val normal = contactPoint.normalWorldOnB

        val tmpVec = Stack.newVec()
        val relPos1 = Stack.newVec()
        relPos1.setSub(pos1, body1.getCenterOfMassPosition(tmpVec))

        val relPos2 = Stack.newVec()
        relPos2.setSub(pos2, body2.getCenterOfMassPosition(tmpVec))

        val vel1 = body1.getVelocityInLocalPoint(relPos1, Stack.newVec())
        val vel2 = body2.getVelocityInLocalPoint(relPos2, Stack.newVec())
        val vel = Stack.newVec()
        vel.setSub(vel1, vel2)

        var relVel: Double
        relVel = normal.dot(vel)

        val Kfps = 1.0 / solverInfo.timeStep

        //btScalar damping = solverInfo.m_damping ;
        val Kerp = solverInfo.erp
        val Kcor = Kerp * Kfps

        val cpd: ConstraintPersistentData? = checkNotNull(contactPoint.userPersistentData as ConstraintPersistentData?)
        val distance = cpd!!.penetration
        val positionalError = Kcor * -distance
        val velocityError = cpd.restitution - relVel // * damping;

        val penetrationImpulse = positionalError * cpd.jacDiagABInv

        val velocityImpulse = velocityError * cpd.jacDiagABInv

        var normalImpulse = penetrationImpulse + velocityImpulse

        // See Erin Catto's GDC 2006 paper: Clamp the accumulated impulse
        val oldNormalImpulse = cpd.appliedImpulse
        val sum = oldNormalImpulse + normalImpulse
        cpd.appliedImpulse = max(0.0, sum)

        normalImpulse = cpd.appliedImpulse - oldNormalImpulse


        //#ifdef USE_INTERNAL_APPLY_IMPULSE
        val tmp = Stack.newVec()
        if (body1.inverseMass != 0.0) {
            tmp.setScale(body1.inverseMass, contactPoint.normalWorldOnB)
            body1.internalApplyImpulse(tmp, cpd.angularComponentA, normalImpulse)
        }
        if (body2.inverseMass != 0.0) {
            tmp.setScale(body2.inverseMass, contactPoint.normalWorldOnB)
            body2.internalApplyImpulse(tmp, cpd.angularComponentB, -normalImpulse)
        }

        //#else //USE_INTERNAL_APPLY_IMPULSE
        //	body1.applyImpulse(normal*(normalImpulse), rel_pos1);
        //	body2.applyImpulse(-normal*(normalImpulse), rel_pos2);
        //#endif //USE_INTERNAL_APPLY_IMPULSE
        run {
            //friction
            body1.getVelocityInLocalPoint(relPos1, vel1)
            body2.getVelocityInLocalPoint(relPos2, vel2)
            vel.setSub(vel1, vel2)

            relVel = normal.dot(vel)

            tmp.setScale(relVel, normal)
            val latVel = Stack.newVec()
            latVel.setSub(vel, tmp)
            val latRelVel = latVel.length()

            val combinedFriction = cpd.friction
            if (cpd.appliedImpulse > 0) {
                if (latRelVel > BulletGlobals.FLT_EPSILON) {
                    latVel.mul(1.0 / latRelVel)

                    val temp1 = Stack.newVec()
                    temp1.setCross(relPos1, latVel)
                    body1.invInertiaTensorWorld.transform(temp1)

                    val temp2 = Stack.newVec()
                    temp2.setCross(relPos2, latVel)
                    body2.invInertiaTensorWorld.transform(temp2)

                    val javaTmp1 = Stack.newVec()
                    javaTmp1.setCross(temp1, relPos1)

                    val javaTmp2 = Stack.newVec()
                    javaTmp2.setCross(temp2, relPos2)

                    tmp.setAdd(javaTmp1, javaTmp2)

                    var frictionImpulse = latRelVel /
                            (body1.inverseMass + body2.inverseMass + latVel.dot(tmp))
                    val normalImpulse1 = cpd.appliedImpulse * combinedFriction

                    frictionImpulse = min(frictionImpulse, normalImpulse1)
                    frictionImpulse = max(frictionImpulse, -normalImpulse1)

                    tmp.setScale(-frictionImpulse, latVel)
                    body1.applyImpulse(tmp, relPos1)

                    tmp.setScale(frictionImpulse, latVel)
                    body2.applyImpulse(tmp, relPos2)

                    Stack.subVec(4)
                }
            }

            Stack.subVec(1) // latVel
        }

        Stack.subVec(6)

        return normalImpulse
    }
}
