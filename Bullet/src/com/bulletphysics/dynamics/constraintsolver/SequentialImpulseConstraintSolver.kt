package com.bulletphysics.dynamics.constraintsolver

import com.bulletphysics.BulletGlobals
import com.bulletphysics.BulletStats
import com.bulletphysics.BulletStats.popProfile
import com.bulletphysics.BulletStats.pushProfile
import com.bulletphysics.ContactDestroyedCallback
import com.bulletphysics.collision.broadphase.Dispatcher
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.narrowphase.ManifoldPoint
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.ContactConstraint.resolveSingleCollisionCombined
import com.bulletphysics.linearmath.IDebugDraw
import com.bulletphysics.linearmath.MiscUtil.resize
import com.bulletphysics.linearmath.TransformUtil.planeSpace1
import com.bulletphysics.util.IntArrayList
import com.bulletphysics.util.ObjectPool
import com.bulletphysics.util.Packing.pack
import com.bulletphysics.util.Packing.unpackHigh
import com.bulletphysics.util.Packing.unpackLow
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setCross
import com.bulletphysics.util.setNegate
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setSub
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * SequentialImpulseConstraintSolver uses a Propagation Method and Sequentially applies impulses.
 * The approach is the 3D version of Erin Catto's GDC 2006 tutorial. See [GPhysics.com](http://www.gphysics.com)
 *
 * Although Sequential Impulse is more intuitive, it is mathematically equivalent to Projected
 * Successive Overrelaxation (iterative LCP).
 *
 * Applies impulses for combined restitution and penetration recovery and to simulate friction.
 *
 * @author jezek2
 */
class SequentialImpulseConstraintSolver : ConstraintSolver {

    private val gOrder = LongArray(SEQUENTIAL_IMPULSE_MAX_SOLVER_POINTS)

    /** ///////////////////////////////////////////////////////////////////////// */
    private val bodiesPool = ObjectPool.Companion.get(SolverBody::class.java)
    private val constraintsPool = ObjectPool.Companion.get(SolverConstraint::class.java)
    private val jacobiansPool = ObjectPool.Companion.get(JacobianEntry::class.java)

    private val tmpSolverBodyPool = ArrayList<SolverBody>()
    private val tmpSolverConstraintPool = ArrayList<SolverConstraint>()
    private val tmpSolverFrictionConstraintPool = ArrayList<SolverConstraint>()
    private val orderTmpConstraintPool = IntArrayList()
    private val orderFrictionConstraintPool = IntArrayList()

    val contactDispatch = Array(MAX_CONTACT_SOLVER_TYPES) {
        Array(MAX_CONTACT_SOLVER_TYPES) { ContactConstraint.resolveSingleCollision }
    }

    val frictionDispatch = Array(MAX_CONTACT_SOLVER_TYPES) {
        Array(MAX_CONTACT_SOLVER_TYPES) { ContactConstraint.resolveSingleFriction }
    }

    // btSeed2 is used for re-arranging the constraint rows. improves convergence/quality of friction
    var randSeed: Long = 0L

    init {
        BulletGlobals.contactDestroyedCallback = ContactDestroyedCallback { userPersistentData: Any? ->
            checkNotNull(userPersistentData)
            true
        }
    }

    /**
     * See ODE: adam's all-int straightforward(?) dRandInt (0..n-1)
     */
    fun randInt2(n: Int): Int {
        // seems good; xor-fold and modulus

        this.randSeed = (1664525L * this.randSeed + 1013904223L)
        var r = this.randSeed

        // note: probably more aggressive than it needs to be -- might be
        //       able to get away without one or two of the innermost branches.
        if (n <= 0x10000) {
            r = r xor (r ushr 16)
            if (n <= 0x100) {
                r = r xor (r ushr 8)
                if (n <= 0x10) {
                    r = r xor (r ushr 4)
                    if (n <= 0x4) {
                        r = r xor (r ushr 2)
                        if (n <= 0x2) {
                            r = r xor (r ushr 1)
                        }
                    }
                }
            }
        }

        // TODO: check modulo C vs Java mismatch
        return abs(r % n).toInt()
    }

    private fun initSolverBody(solverBody: SolverBody, collisionObject: CollisionObject) {
        val rb = collisionObject as? RigidBody
        if (rb != null) {
            rb.getAngularVelocity(solverBody.angularVelocity)
            solverBody.centerOfMassPosition.set(collisionObject.getWorldTransform(Stack.newTrans()).origin)
            solverBody.friction = collisionObject.friction
            solverBody.invMass = rb.inverseMass
            rb.getLinearVelocity(solverBody.linearVelocity)
            solverBody.originalBody = rb
            solverBody.angularFactor = rb.angularFactor
        } else {
            solverBody.angularVelocity.set(0.0, 0.0, 0.0)
            solverBody.centerOfMassPosition.set(collisionObject.getWorldTransform(Stack.newTrans()).origin)
            solverBody.friction = collisionObject.friction
            solverBody.invMass = 0.0
            solverBody.linearVelocity.set(0.0, 0.0, 0.0)
            solverBody.originalBody = null
            solverBody.angularFactor = 1.0
        }

        solverBody.pushVelocity.set(0.0, 0.0, 0.0)
        solverBody.turnVelocity.set(0.0, 0.0, 0.0)
    }

    private fun restitutionCurve(relVel: Double, restitution: Double): Double {
        return -restitution * relVel
    }

    private fun resolveSplitPenetrationImpulseCacheFriendly(
        body1: SolverBody,
        body2: SolverBody,
        contactConstraint: SolverConstraint,
        solverInfo: ContactSolverInfo
    ) {
        if (contactConstraint.penetration < solverInfo.splitImpulsePenetrationThreshold) {
            BulletStats.numSplitImpulseRecoveries++
            var normalImpulse: Double

            //  Optimized version of projected relative velocity, use precomputed cross products with normal
            //      body1.getVelocityInLocalPoint(contactConstraint.m_rel_posA,vel1);
            //      body2.getVelocityInLocalPoint(contactConstraint.m_rel_posB,vel2);
            //      btVector3 vel = vel1 - vel2;
            //      btScalar  rel_vel = contactConstraint.m_contactNormal.dot(vel);

            val vel1DotN = contactConstraint.contactNormal.dot(body1.pushVelocity) +
                    contactConstraint.relPos1CrossNormal.dot(body1.turnVelocity)
            val vel2DotN = contactConstraint.contactNormal.dot(body2.pushVelocity) +
                    contactConstraint.relPos2CrossNormal.dot(body2.turnVelocity)

            val relVel = vel1DotN - vel2DotN

            val positionalError = -contactConstraint.penetration * solverInfo.erp2 / solverInfo.timeStep

            //      btScalar positionalError = contactConstraint.m_penetration;
            val velocityError = contactConstraint.restitution - relVel // * damping;

            val penetrationImpulse = positionalError * contactConstraint.jacDiagABInv
            val velocityImpulse = velocityError * contactConstraint.jacDiagABInv
            normalImpulse = penetrationImpulse + velocityImpulse

            // See Erin Catto's GDC 2006 paper: Clamp the accumulated impulse
            val oldNormalImpulse = contactConstraint.appliedPushImpulse
            val sum = oldNormalImpulse + normalImpulse
            contactConstraint.appliedPushImpulse = max(0.0, sum)

            normalImpulse = contactConstraint.appliedPushImpulse - oldNormalImpulse

            val tmp = Stack.newVec()
            tmp.setScale(body1.invMass, contactConstraint.contactNormal)
            body1.internalApplyPushImpulse(tmp, contactConstraint.angularComponentA, normalImpulse)

            tmp.setScale(body2.invMass, contactConstraint.contactNormal)
            body2.internalApplyPushImpulse(tmp, contactConstraint.angularComponentB, -normalImpulse)
            Stack.subVec(1)
        }
    }

    /**
     * velocity + friction
     * response  between two dynamic objects with friction
     */
    private fun resolveSingleCollisionCombinedCacheFriendly(
        body1: SolverBody,
        body2: SolverBody,
        contactConstraint: SolverConstraint,
        solverInfo: ContactSolverInfo
    ) {
        // Optimized version of projected relative velocity, use precomputed cross products with normal
        // body1.getVelocityInLocalPoint(contactConstraint.m_rel_posA,vel1);
        // body2.getVelocityInLocalPoint(contactConstraint.m_rel_posB,vel2);
        // btVector3 vel = vel1 - vel2;
        // btScalar  rel_vel = contactConstraint.m_contactNormal.dot(vel);

        val vel1Dotn =
            contactConstraint.contactNormal.dot(body1.linearVelocity) + contactConstraint.relPos1CrossNormal.dot(body1.angularVelocity)
        val vel2Dotn =
            contactConstraint.contactNormal.dot(body2.linearVelocity) + contactConstraint.relPos2CrossNormal.dot(body2.angularVelocity)

        val relVel = vel1Dotn - vel2Dotn

        var normalImpulse: Double = getNormalImpulse(contactConstraint, solverInfo, relVel)

        // See Erin Catto's GDC 2006 paper: Clamp the accumulated impulse
        val oldNormalImpulse = contactConstraint.appliedImpulse
        val sum = oldNormalImpulse + normalImpulse
        contactConstraint.appliedImpulse = max(0.0, sum)

        normalImpulse = contactConstraint.appliedImpulse - oldNormalImpulse

        val tmp = Stack.newVec()
        tmp.setScale(body1.invMass, contactConstraint.contactNormal)
        body1.internalApplyImpulse(tmp, contactConstraint.angularComponentA, normalImpulse)

        tmp.setScale(body2.invMass, contactConstraint.contactNormal)
        body2.internalApplyImpulse(tmp, contactConstraint.angularComponentB, -normalImpulse)
        Stack.subVec(1)
    }

    private fun resolveSingleFrictionCacheFriendly(
        body1: SolverBody, body2: SolverBody, contactConstraint: SolverConstraint,
        appliedNormalImpulse: Double
    ) {
        val combinedFriction = contactConstraint.friction

        val limit = appliedNormalImpulse * combinedFriction

        if (appliedNormalImpulse > 0.0) { //friction

            val vel1DotN = contactConstraint.contactNormal.dot(body1.linearVelocity) +
                    contactConstraint.relPos1CrossNormal.dot(body1.angularVelocity)
            val vel2DotN = contactConstraint.contactNormal.dot(body2.linearVelocity) +
                    contactConstraint.relPos2CrossNormal.dot(body2.angularVelocity)
            val relVel = vel1DotN - vel2DotN

            // calculate j that moves us to zero relative velocity
            var j1 = -relVel * contactConstraint.jacDiagABInv
            val oldTangentImpulse = contactConstraint.appliedImpulse
            contactConstraint.appliedImpulse = oldTangentImpulse + j1

            if (limit < contactConstraint.appliedImpulse) {
                contactConstraint.appliedImpulse = limit
            } else {
                if (contactConstraint.appliedImpulse < -limit) {
                    contactConstraint.appliedImpulse = -limit
                }
            }
            j1 = contactConstraint.appliedImpulse - oldTangentImpulse


            val tmp = Stack.newVec()
            tmp.setScale(body1.invMass, contactConstraint.contactNormal)
            body1.internalApplyImpulse(tmp, contactConstraint.angularComponentA, j1)

            tmp.setScale(body2.invMass, contactConstraint.contactNormal)
            body2.internalApplyImpulse(tmp, contactConstraint.angularComponentB, -j1)
            Stack.subVec(1)
        }
    }

    fun addFrictionConstraint(
        normalAxis: Vector3d, solverBodyIdA: Int, solverBodyIdB: Int, frictionIndex: Int,
        cp: ManifoldPoint, relPos1: Vector3d, relPos2: Vector3d,
        colObj0: CollisionObject?, colObj1: CollisionObject?, relaxation: Double
    ) {
        val body0 = colObj0 as? RigidBody
        val body1 = colObj1 as? RigidBody

        val solverConstraint = constraintsPool.get()
        tmpSolverFrictionConstraintPool.add(solverConstraint)

        solverConstraint.contactNormal.set(normalAxis)

        solverConstraint.solverBodyIdA = solverBodyIdA
        solverConstraint.solverBodyIdB = solverBodyIdB
        solverConstraint.constraintType = SolverConstraintType.SOLVER_FRICTION_1D
        solverConstraint.frictionIndex = frictionIndex

        solverConstraint.friction = cp.combinedFriction
        solverConstraint.originalContactPoint = null

        solverConstraint.appliedImpulse = 0.0
        solverConstraint.appliedPushImpulse = 0.0
        solverConstraint.penetration = 0.0

        val ftorqueAxis1 = Stack.newVec()
        val tmpMat = Stack.newMat()


        ftorqueAxis1.setCross(relPos1, solverConstraint.contactNormal)
        solverConstraint.relPos1CrossNormal.set(ftorqueAxis1)
        if (body0 != null) {
            solverConstraint.angularComponentA.set(ftorqueAxis1)
            body0.getInvInertiaTensorWorld(tmpMat).transform(solverConstraint.angularComponentA)
        } else {
            solverConstraint.angularComponentA.set(0.0, 0.0, 0.0)
        }


        ftorqueAxis1.setCross(relPos2, solverConstraint.contactNormal)
        solverConstraint.relPos2CrossNormal.set(ftorqueAxis1)
        if (body1 != null) {
            solverConstraint.angularComponentB.set(ftorqueAxis1)
            body1.getInvInertiaTensorWorld(tmpMat).transform(solverConstraint.angularComponentB)
        } else {
            solverConstraint.angularComponentB.set(0.0, 0.0, 0.0)
        }


        val vec = Stack.newVec()
        var denom0 = 0.0
        var denom1 = 0.0
        if (body0 != null) {
            vec.setCross(solverConstraint.angularComponentA, relPos1)
            denom0 = body0.inverseMass + normalAxis.dot(vec)
        }
        if (body1 != null) {
            vec.setCross(solverConstraint.angularComponentB, relPos2)
            denom1 = body1.inverseMass + normalAxis.dot(vec)
        }

        solverConstraint.jacDiagABInv = relaxation / (denom0 + denom1)

        Stack.subVec(2)
        Stack.subMat(1)
    }

    @Suppress("SameReturnValue")
    fun solveGroupCacheFriendlySetup(
        manifoldPtr: List<PersistentManifold>, manifoldOffset: Int, numManifolds: Int,
        constraints: List<TypedConstraint>?, constraintsOffset: Int,
        numConstraints: Int, infoGlobal: ContactSolverInfo
    ): Double {
        pushProfile("solveGroupCacheFriendlySetup")
        try {
            if ((numConstraints + numManifolds) == 0) {
                return 0.0
            }
            var manifold: PersistentManifold
            var colObj0: CollisionObject
            var colObj1: CollisionObject

            val tmpTrans = Stack.newTrans()

            val relPos1 = Stack.newVec()
            val relPos2 = Stack.newVec()

            val pos1 = Stack.newVec()
            val pos2 = Stack.newVec()
            val vel = Stack.newVec()
            val torqueAxis0 = Stack.newVec()
            val torqueAxis1 = Stack.newVec()
            val vel1 = Stack.newVec()
            val vel2 = Stack.newVec()
            val vec = Stack.newVec()
            val tmp = Stack.newVec()

            val tmpMat = Stack.newMat()

            for (i in 0 until numManifolds) {
                manifold = manifoldPtr[manifoldOffset + i]
                colObj0 = manifold.getBody0() as CollisionObject
                colObj1 = manifold.getBody1() as CollisionObject

                var solverBodyIdA = -1
                var solverBodyIdB = -1

                if (manifold.numContacts != 0) {
                    if (colObj0.islandTag >= 0) {
                        if (colObj0.companionId >= 0) {
                            // body has already been converted
                            solverBodyIdA = colObj0.companionId
                        } else {
                            solverBodyIdA = tmpSolverBodyPool.size
                            val solverBody = bodiesPool.get()
                            tmpSolverBodyPool.add(solverBody)
                            initSolverBody(solverBody, colObj0)
                            colObj0.companionId = solverBodyIdA
                        }
                    } else {
                        // create a static body
                        solverBodyIdA = tmpSolverBodyPool.size
                        val solverBody = bodiesPool.get()
                        tmpSolverBodyPool.add(solverBody)
                        initSolverBody(solverBody, colObj0)
                    }

                    if (colObj1.islandTag >= 0) {
                        if (colObj1.companionId >= 0) {
                            solverBodyIdB = colObj1.companionId
                        } else {
                            solverBodyIdB = tmpSolverBodyPool.size
                            val solverBody = bodiesPool.get()
                            tmpSolverBodyPool.add(solverBody)
                            initSolverBody(solverBody, colObj1)
                            colObj1.companionId = solverBodyIdB
                        }
                    } else {
                        // create a static body
                        solverBodyIdB = tmpSolverBodyPool.size
                        val solverBody = bodiesPool.get()
                        tmpSolverBodyPool.add(solverBody)
                        initSolverBody(solverBody, colObj1)
                    }
                }

                var relaxation: Double

                for (j in 0 until manifold.numContacts) {
                    val cp = manifold.getContactPoint(j)

                    if (cp.distance <= 0.0) {
                        cp.getPositionWorldOnA(pos1)
                        cp.getPositionWorldOnB(pos2)

                        relPos1.setSub(pos1, colObj0.getWorldTransform(tmpTrans).origin)
                        relPos2.setSub(pos2, colObj1.getWorldTransform(tmpTrans).origin)

                        relaxation = 1.0

                        val frictionIndex = tmpSolverConstraintPool.size

                        run {
                            val solverConstraint = constraintsPool.get()
                            tmpSolverConstraintPool.add(solverConstraint)
                            val rb0 = RigidBody.upcast(colObj0)
                            val rb1 = RigidBody.upcast(colObj1)

                            solverConstraint.solverBodyIdA = solverBodyIdA
                            solverConstraint.solverBodyIdB = solverBodyIdB
                            solverConstraint.constraintType = SolverConstraintType.SOLVER_CONTACT_1D

                            solverConstraint.originalContactPoint = cp

                            torqueAxis0.setCross(relPos1, cp.normalWorldOnB)

                            if (rb0 != null) {
                                solverConstraint.angularComponentA.set(torqueAxis0)
                                rb0.getInvInertiaTensorWorld(tmpMat).transform(solverConstraint.angularComponentA)
                            } else {
                                solverConstraint.angularComponentA.set(0.0, 0.0, 0.0)
                            }

                            torqueAxis1.setCross(relPos2, cp.normalWorldOnB)

                            if (rb1 != null) {
                                solverConstraint.angularComponentB.set(torqueAxis1)
                                rb1.getInvInertiaTensorWorld(tmpMat).transform(solverConstraint.angularComponentB)
                            } else {
                                solverConstraint.angularComponentB.set(0.0, 0.0, 0.0)
                            }

                            run {
                                //#ifdef COMPUTE_IMPULSE_DENOM
                                //btScalar denom0 = rb0->computeImpulseDenominator(pos1,cp.m_normalWorldOnB);
                                //btScalar denom1 = rb1->computeImpulseDenominator(pos2,cp.m_normalWorldOnB);
                                //#else
                                var denom0 = 0.0
                                var denom1 = 0.0
                                if (rb0 != null) {
                                    vec.setCross(solverConstraint.angularComponentA, relPos1)
                                    denom0 = rb0.inverseMass + cp.normalWorldOnB.dot(vec)
                                }
                                if (rb1 != null) {
                                    vec.setCross(solverConstraint.angularComponentB, relPos2)
                                    denom1 = rb1.inverseMass + cp.normalWorldOnB.dot(vec)
                                }

                                //#endif //COMPUTE_IMPULSE_DENOM
                                solverConstraint.jacDiagABInv = relaxation / (denom0 + denom1)
                            }

                            solverConstraint.contactNormal.set(cp.normalWorldOnB)
                            solverConstraint.relPos1CrossNormal.setCross(relPos1, cp.normalWorldOnB)
                            solverConstraint.relPos2CrossNormal.setCross(relPos2, cp.normalWorldOnB)

                            if (rb0 != null) {
                                rb0.getVelocityInLocalPoint(relPos1, vel1)
                            } else {
                                vel1.set(0.0, 0.0, 0.0)
                            }

                            if (rb1 != null) {
                                rb1.getVelocityInLocalPoint(relPos2, vel2)
                            } else {
                                vel2.set(0.0, 0.0, 0.0)
                            }

                            vel.setSub(vel1, vel2)

                            val relVel = cp.normalWorldOnB.dot(vel)

                            solverConstraint.penetration = min(cp.distance + infoGlobal.linearSlop, 0.0)

                            //solverConstraint.m_penetration = cp.getDistance();
                            solverConstraint.friction = cp.combinedFriction
                            solverConstraint.restitution = restitutionCurve(relVel, cp.combinedRestitution)
                            if (solverConstraint.restitution <= 0.0) {
                                solverConstraint.restitution = 0.0
                            }

                            val penVel = -solverConstraint.penetration / infoGlobal.timeStep

                            if (solverConstraint.restitution > penVel) {
                                solverConstraint.penetration = 0.0
                            }

                            // warm starting (or zero if disabled)
                            if ((infoGlobal.solverMode and SolverMode.SOLVER_USE_WARMSTARTING) != 0) {
                                solverConstraint.appliedImpulse = cp.appliedImpulse * infoGlobal.warmstartingFactor
                                if (rb0 != null) {
                                    tmp.setScale(rb0.inverseMass, solverConstraint.contactNormal)
                                    tmpSolverBodyPool[solverConstraint.solverBodyIdA]
                                        .internalApplyImpulse(
                                            tmp,
                                            solverConstraint.angularComponentA,
                                            solverConstraint.appliedImpulse
                                        )
                                }
                                if (rb1 != null) {
                                    tmp.setScale(rb1.inverseMass, solverConstraint.contactNormal)
                                    tmpSolverBodyPool[solverConstraint.solverBodyIdB]
                                        .internalApplyImpulse(
                                            tmp,
                                            solverConstraint.angularComponentB,
                                            -solverConstraint.appliedImpulse
                                        )
                                }
                            } else {
                                solverConstraint.appliedImpulse = 0.0
                            }

                            solverConstraint.appliedPushImpulse = 0.0

                            solverConstraint.frictionIndex = tmpSolverFrictionConstraintPool.size
                            if (!cp.lateralFrictionInitialized) {
                                cp.lateralFrictionDir1.setScale(relVel, cp.normalWorldOnB)
                                cp.lateralFrictionDir1.setSub(vel, cp.lateralFrictionDir1)

                                val latRelVel = cp.lateralFrictionDir1.lengthSquared()
                                if (latRelVel > BulletGlobals.FLT_EPSILON)  //0.0)
                                {
                                    cp.lateralFrictionDir1.mul(1.0 / sqrt(latRelVel))
                                    addFrictionConstraint(
                                        cp.lateralFrictionDir1,
                                        solverBodyIdA,
                                        solverBodyIdB,
                                        frictionIndex,
                                        cp,
                                        relPos1,
                                        relPos2,
                                        colObj0,
                                        colObj1,
                                        relaxation
                                    )
                                    cp.lateralFrictionDir2.setCross(cp.lateralFrictionDir1, cp.normalWorldOnB)
                                    cp.lateralFrictionDir2.normalize() //??
                                    addFrictionConstraint(
                                        cp.lateralFrictionDir2,
                                        solverBodyIdA,
                                        solverBodyIdB,
                                        frictionIndex,
                                        cp,
                                        relPos1,
                                        relPos2,
                                        colObj0,
                                        colObj1,
                                        relaxation
                                    )
                                } else {
                                    // re-calculate friction direction every frame, todo: check if this is really needed

                                    planeSpace1(cp.normalWorldOnB, cp.lateralFrictionDir1, cp.lateralFrictionDir2)
                                    addFrictionConstraint(
                                        cp.lateralFrictionDir1,
                                        solverBodyIdA,
                                        solverBodyIdB,
                                        frictionIndex,
                                        cp,
                                        relPos1,
                                        relPos2,
                                        colObj0,
                                        colObj1,
                                        relaxation
                                    )
                                    addFrictionConstraint(
                                        cp.lateralFrictionDir2,
                                        solverBodyIdA,
                                        solverBodyIdB,
                                        frictionIndex,
                                        cp,
                                        relPos1,
                                        relPos2,
                                        colObj0,
                                        colObj1,
                                        relaxation
                                    )
                                }
                                cp.lateralFrictionInitialized = true
                            } else {
                                addFrictionConstraint(
                                    cp.lateralFrictionDir1,
                                    solverBodyIdA,
                                    solverBodyIdB,
                                    frictionIndex,
                                    cp,
                                    relPos1,
                                    relPos2,
                                    colObj0,
                                    colObj1,
                                    relaxation
                                )
                                addFrictionConstraint(
                                    cp.lateralFrictionDir2,
                                    solverBodyIdA,
                                    solverBodyIdB,
                                    frictionIndex,
                                    cp,
                                    relPos1,
                                    relPos2,
                                    colObj0,
                                    colObj1,
                                    relaxation
                                )
                            }

                            run {
                                val frictionConstraint1 =
                                    tmpSolverFrictionConstraintPool[solverConstraint.frictionIndex]
                                if ((infoGlobal.solverMode and SolverMode.SOLVER_USE_WARMSTARTING) != 0) {
                                    frictionConstraint1.appliedImpulse =
                                        cp.appliedImpulseLateral1 * infoGlobal.warmstartingFactor
                                    if (rb0 != null) {
                                        tmp.setScale(rb0.inverseMass, frictionConstraint1.contactNormal)
                                        tmpSolverBodyPool[solverConstraint.solverBodyIdA]
                                            .internalApplyImpulse(
                                                tmp,
                                                frictionConstraint1.angularComponentA,
                                                frictionConstraint1.appliedImpulse
                                            )
                                    }
                                    if (rb1 != null) {
                                        tmp.setScale(rb1.inverseMass, frictionConstraint1.contactNormal)
                                        tmpSolverBodyPool[solverConstraint.solverBodyIdB]
                                            .internalApplyImpulse(
                                                tmp,
                                                frictionConstraint1.angularComponentB,
                                                -frictionConstraint1.appliedImpulse
                                            )
                                    }
                                } else {
                                    frictionConstraint1.appliedImpulse = 0.0
                                }
                            }
                            run {
                                val frictionConstraint2 =
                                    tmpSolverFrictionConstraintPool[solverConstraint.frictionIndex + 1]
                                if ((infoGlobal.solverMode and SolverMode.SOLVER_USE_WARMSTARTING) != 0) {
                                    frictionConstraint2.appliedImpulse =
                                        cp.appliedImpulseLateral2 * infoGlobal.warmstartingFactor
                                    if (rb0 != null) {
                                        tmp.setScale(rb0.inverseMass, frictionConstraint2.contactNormal)
                                        tmpSolverBodyPool[solverConstraint.solverBodyIdA]
                                            .internalApplyImpulse(
                                                tmp,
                                                frictionConstraint2.angularComponentA,
                                                frictionConstraint2.appliedImpulse
                                            )
                                    }
                                    if (rb1 != null) {
                                        tmp.setScale(rb1.inverseMass, frictionConstraint2.contactNormal)
                                        tmpSolverBodyPool[solverConstraint.solverBodyIdB]
                                            .internalApplyImpulse(
                                                tmp,
                                                frictionConstraint2.angularComponentB,
                                                -frictionConstraint2.appliedImpulse
                                            )
                                    }
                                } else {
                                    frictionConstraint2.appliedImpulse = 0.0
                                }
                            }
                        }
                    }
                }
            }

            if (constraints != null) {
                for (j in 0 until numConstraints) {
                    constraints[constraintsOffset + j].buildJacobian()
                }
            }

            val numConstraintPool = tmpSolverConstraintPool.size
            val numFrictionPool = tmpSolverFrictionConstraintPool.size

            resize(orderTmpConstraintPool, numConstraintPool, 0)
            resize(orderFrictionConstraintPool, numFrictionPool, 0)

            for (j in 0 until numConstraintPool) {
                orderTmpConstraintPool.set(j, j)
            }
            for (j in 0 until numFrictionPool) {
                orderFrictionConstraintPool.set(j, j)
            }

            Stack.subTrans(1)
            Stack.subMat(1)
            Stack.subVec(11)

            return 0.0
        } finally {
            popProfile()
        }
    }

    fun solveGroupCacheFriendlyIterations(
        constraints: List<TypedConstraint>?, constraintsOffset: Int, numConstraints: Int,
        infoGlobal: ContactSolverInfo
    ): Double {
        pushProfile("solveGroupCacheFriendlyIterations")
        try {
            val numConstraintPool = tmpSolverConstraintPool.size
            val numFrictionPool = tmpSolverFrictionConstraintPool.size

            // should traverse the contacts random order...
            var iteration: Int
            var stackPos: IntArray? = null
            iteration = 0
            while (iteration < infoGlobal.numIterations) {
                var j: Int
                if ((infoGlobal.solverMode and SolverMode.SOLVER_RANDOMIZE_ORDER) != 0) {
                    if ((iteration and 7) == 0) {
                        j = 0
                        while (j < numConstraintPool) {
                            val tmp = orderTmpConstraintPool.get(j)
                            val swapi = randInt2(j + 1)
                            orderTmpConstraintPool.set(j, orderTmpConstraintPool.get(swapi))
                            orderTmpConstraintPool.set(swapi, tmp)
                            ++j
                        }

                        j = 0
                        while (j < numFrictionPool) {
                            val tmp = orderFrictionConstraintPool.get(j)
                            val swapi = randInt2(j + 1)
                            orderFrictionConstraintPool.set(j, orderFrictionConstraintPool.get(swapi))
                            orderFrictionConstraintPool.set(swapi, tmp)
                            ++j
                        }
                    }
                }

                if (constraints != null) {
                    for (k in 0 until numConstraints) {
                        stackPos = Stack.getPosition(stackPos)
                        val constraint = constraints[constraintsOffset + k]

                        // todo: use solver bodies, so we don't need to copy from/to btRigidBody
                        if ((constraint.rigidBodyA.islandTag >= 0) && (constraint.rigidBodyA.companionId >= 0)) {
                            tmpSolverBodyPool[constraint.rigidBodyA.companionId].writebackVelocity()
                        }
                        if ((constraint.rigidBodyB.islandTag >= 0) && (constraint.rigidBodyB.companionId >= 0)) {
                            tmpSolverBodyPool[constraint.rigidBodyB.companionId].writebackVelocity()
                        }

                        constraint.solveConstraint(infoGlobal.timeStep)

                        if ((constraint.rigidBodyA.islandTag >= 0) && (constraint.rigidBodyA.companionId >= 0)) {
                            tmpSolverBodyPool[constraint.rigidBodyA.companionId].readVelocity()
                        }
                        if ((constraint.rigidBodyB.islandTag >= 0) && (constraint.rigidBodyB.companionId >= 0)) {
                            tmpSolverBodyPool[constraint.rigidBodyB.companionId].readVelocity()
                        }
                        Stack.reset(stackPos)
                    }
                }


                val numPoolConstraints = tmpSolverConstraintPool.size
                for (k in 0 until numPoolConstraints) {
                    stackPos = Stack.getPosition(stackPos)
                    val solveManifold = tmpSolverConstraintPool[orderTmpConstraintPool.get(k)]
                    resolveSingleCollisionCombinedCacheFriendly(
                        tmpSolverBodyPool[solveManifold.solverBodyIdA],
                        tmpSolverBodyPool[solveManifold.solverBodyIdB], solveManifold, infoGlobal
                    )
                    Stack.reset(stackPos)
                }

                val numFrictionPoolConstraints = tmpSolverFrictionConstraintPool.size
                for (k in 0 until numFrictionPoolConstraints) {
                    stackPos = Stack.getPosition(stackPos)
                    val solveManifold = tmpSolverFrictionConstraintPool[orderFrictionConstraintPool.get(k)]

                    val totalImpulse = tmpSolverConstraintPool[solveManifold.frictionIndex].appliedImpulse +
                            tmpSolverConstraintPool[solveManifold.frictionIndex].appliedPushImpulse

                    resolveSingleFrictionCacheFriendly(
                        tmpSolverBodyPool[solveManifold.solverBodyIdA],
                        tmpSolverBodyPool[solveManifold.solverBodyIdB], solveManifold,
                        totalImpulse
                    )
                    Stack.reset(stackPos)
                }
                iteration++
            }

            if (infoGlobal.splitImpulse) {
                iteration = 0
                while (iteration < infoGlobal.numIterations) {
                    val numPoolConstraints = tmpSolverConstraintPool.size
                    for (j in 0 until numPoolConstraints) {
                        stackPos = Stack.getPosition(stackPos)
                        val solveManifold = tmpSolverConstraintPool[orderTmpConstraintPool.get(j)]
                        resolveSplitPenetrationImpulseCacheFriendly(
                            tmpSolverBodyPool[solveManifold.solverBodyIdA],
                            tmpSolverBodyPool[solveManifold.solverBodyIdB], solveManifold, infoGlobal
                        )
                        Stack.reset(stackPos)
                    }
                    iteration++
                }
            }

            return 0.0
        } finally {
            popProfile()
        }
    }

    fun solveGroupCacheFriendly(
        manifoldPtr: List<PersistentManifold>, manifoldOffset: Int, numManifolds: Int,
        constraints: List<TypedConstraint>?, constraintsOffset: Int, numConstraints: Int,
        infoGlobal: ContactSolverInfo
    ): Double {
        solveGroupCacheFriendlySetup(
            manifoldPtr, manifoldOffset, numManifolds,
            constraints, constraintsOffset, numConstraints,
            infoGlobal
        )
        solveGroupCacheFriendlyIterations(constraints, constraintsOffset, numConstraints, infoGlobal)

        val numPoolConstraints = tmpSolverConstraintPool.size
        for (j in 0 until numPoolConstraints) {
            val solveManifold = tmpSolverConstraintPool[j]
            val pt = checkNotNull(solveManifold.originalContactPoint as ManifoldPoint?)
            pt.appliedImpulse = solveManifold.appliedImpulse
            pt.appliedImpulseLateral1 =
                tmpSolverFrictionConstraintPool[solveManifold.frictionIndex].appliedImpulse
            pt.appliedImpulseLateral2 =
                tmpSolverFrictionConstraintPool[solveManifold.frictionIndex + 1].appliedImpulse

            // do a callback here?
        }

        if (infoGlobal.splitImpulse) {
            for (i in 0 until tmpSolverBodyPool.size) {
                tmpSolverBodyPool[i].writebackVelocity(infoGlobal.timeStep)
                bodiesPool.release(tmpSolverBodyPool[i])
            }
        } else {
            for (i in 0 until tmpSolverBodyPool.size) {
                tmpSolverBodyPool[i].writebackVelocity()
                bodiesPool.release(tmpSolverBodyPool[i])
            }
        }

        tmpSolverBodyPool.clear()
        for (i in 0 until tmpSolverConstraintPool.size) {
            constraintsPool.release(tmpSolverConstraintPool[i])
        }
        tmpSolverConstraintPool.clear()

        for (i in 0 until tmpSolverFrictionConstraintPool.size) {
            constraintsPool.release(tmpSolverFrictionConstraintPool[i])
        }
        tmpSolverFrictionConstraintPool.clear()

        return 0.0
    }

    /**
     * Sequentially applies impulses.
     */
    override fun solveGroup(
        bodies: List<CollisionObject>, numBodies: Int,
        manifold: List<PersistentManifold>, manifoldOffset: Int, numManifolds: Int,
        constraints: List<TypedConstraint>?, constraintsOffset: Int, numConstraints: Int,
        info: ContactSolverInfo, debugDrawer: IDebugDraw?, dispatcher: Dispatcher
    ): Double {
        pushProfile("solveGroup")
        try {
            // TODO: solver cache friendly
            if ((info.solverMode and SolverMode.SOLVER_CACHE_FRIENDLY) != 0) {
                // you need to provide at least some bodies
                // SimpleDynamicsWorld needs to switch off SOLVER_CACHE_FRIENDLY
                checkNotNull(bodies)
                assert(numBodies != 0)
                return solveGroupCacheFriendly(
                    manifold, manifoldOffset, numManifolds,
                    constraints, constraintsOffset, numConstraints,
                    info
                )
            }

            val info = ContactSolverInfo(info)

            val numIter = info.numIterations

            var totalPoints = 0
            for (j in 0 until numManifolds) {
                val manifold1 = manifold[manifoldOffset + j]
                prepareConstraints(manifold1, info)

                for (p in 0 until manifold1.numContacts) {
                    gOrder[totalPoints] = orderIndex(j, p)
                    totalPoints++
                }
            }

            if (constraints != null) {
                for (j in 0 until numConstraints) {
                    constraints[constraintsOffset + j]
                        .buildJacobian()
                }
            }

            // should traverse the contacts random order...
            for (iteration in 0 until numIter) {
                if ((info.solverMode and SolverMode.SOLVER_RANDOMIZE_ORDER) != 0) {
                    if ((iteration and 7) == 0) {
                        for (j in 0 until totalPoints) {
                            // JAVA NOTE: swaps references instead of copying values (but that's fine in this context)
                            val tmp = gOrder[j]
                            val swapi = randInt2(j + 1)
                            gOrder[j] = gOrder[swapi]
                            gOrder[swapi] = tmp
                        }
                    }
                }

                if (constraints != null) {
                    for (j in 0 until numConstraints) {
                        constraints[constraintsOffset + j]
                            .solveConstraint(info.timeStep)
                    }
                }

                for (j in 0 until totalPoints) {
                    val manifold1 = manifold[manifoldOffset + orderManifoldIndex(gOrder[j])]
                    solve(
                        (manifold1.getBody0() as RigidBody),
                        manifold1.getBody1() as RigidBody,
                        manifold1.getContactPoint(orderPointIndex(gOrder[j])), info
                    )
                }

                for (j in 0 until totalPoints) {
                    val manifold1 = manifold[manifoldOffset + orderManifoldIndex(gOrder[j])]
                    solveFriction(
                        (manifold1.getBody0() as RigidBody),
                        manifold1.getBody1() as RigidBody,
                        manifold1.getContactPoint(orderPointIndex(gOrder[j])), info
                    )
                }
            }
            return 0.0
        } finally {
            popProfile()
        }
    }

    fun prepareConstraints(manifoldPtr: PersistentManifold, info: ContactSolverInfo) {
        val body0 = manifoldPtr.getBody0() as RigidBody
        val body1 = manifoldPtr.getBody1() as RigidBody

        // only necessary to refresh the manifold once (first iteration). The integration is done outside the loop

        //#ifdef FORCE_REFESH_CONTACT_MANIFOLDS
        //manifoldPtr->refreshContactPoints(body0->getCenterOfMassTransform(),body1->getCenterOfMassTransform());
        //#endif //FORCE_REFESH_CONTACT_MANIFOLDS
        val numpoints = manifoldPtr.numContacts

        BulletStats.totalContactPoints += numpoints

        val tmpVec = Stack.newVec()
        val tmpVec1 = Stack.newVec()
        val tmpMat3 = Stack.newMat()

        val pos1 = Stack.newVec()
        val pos2 = Stack.newVec()
        val relPos1 = Stack.newVec()
        val relPos2 = Stack.newVec()
        val vel1 = Stack.newVec()
        val vel2 = Stack.newVec()
        val vel = Stack.newVec()
        val totalImpulse = Stack.newVec()
        val torqueAxis0 = Stack.newVec()
        val torqueAxis1 = Stack.newVec()
        val ftorqueAxis0 = Stack.newVec()
        val ftorqueAxis1 = Stack.newVec()

        val trans1 = Stack.newTrans()
        val trans2 = Stack.newTrans()

        for (i in 0 until numpoints) {
            val cp = manifoldPtr.getContactPoint(i)
            if (cp.distance <= 0.0) {
                cp.getPositionWorldOnA(pos1)
                cp.getPositionWorldOnB(pos2)

                relPos1.setSub(pos1, body0.getCenterOfMassPosition(tmpVec))
                relPos2.setSub(pos2, body1.getCenterOfMassPosition(tmpVec))

                // this jacobian entry is re-used for all iterations
                val mat1 = body0.getCenterOfMassTransform(trans1).basis
                mat1.transpose()

                val mat2 = body1.getCenterOfMassTransform(trans2).basis
                mat2.transpose()

                val jac = jacobiansPool.get()
                jac.init(
                    mat1, mat2,
                    relPos1, relPos2, cp.normalWorldOnB,
                    body0.getInvInertiaDiagLocal(tmpVec), body0.inverseMass,
                    body1.getInvInertiaDiagLocal(tmpVec1), body1.inverseMass
                )

                val jacDiagAB = jac.diagonal
                jacobiansPool.release(jac)

                var cpd = cp.userPersistentData as ConstraintPersistentData?
                if (cpd != null) {
                    // might be invalid
                    cpd.persistentLifeTime++
                    if (cpd.persistentLifeTime != cp.lifeTime) {
                        //printf("Invalid: cpd->m_persistentLifeTime = %i cp.getLifeTime() = %i\n",cpd->m_persistentLifeTime,cp.getLifeTime());
                        //new (cpd) btConstraintPersistentData;
                        cpd.reset()
                        cpd.persistentLifeTime = cp.lifeTime
                    } // else printf("Persistent: cpd->m_persistentLifeTime = %i cp.getLifeTime() = %i\n",cpd->m_persistentLifeTime,cp.getLifeTime());
                } else {
                    // todo: should this be in a pool?
                    //void* mem = btAlignedAlloc(sizeof(btConstraintPersistentData),16);
                    //cpd = new (mem)btConstraintPersistentData;
                    cpd = ConstraintPersistentData()

                    //assert(cpd != null);

                    //printf("totalCpd = %i Created Ptr %x\n",totalCpd,cpd);
                    cp.userPersistentData = cpd
                    cpd.persistentLifeTime = cp.lifeTime
                    //printf("CREATED: %x . cpd->m_persistentLifeTime = %i cp.getLifeTime() = %i\n",cpd,cpd->m_persistentLifeTime,cp.getLifeTime());
                }

                cpd.jacDiagABInv = 1.0 / jacDiagAB

                // Dependent on Rigidbody A and B types, fetch the contact/friction response func
                // perhaps do a similar thing for friction/restutution combiner funcs...
                cpd.frictionSolverFunc = frictionDispatch[body0.frictionSolverType][body1.frictionSolverType]
                cpd.contactSolverFunc = contactDispatch[body0.contactSolverType][body1.contactSolverType]

                body0.getVelocityInLocalPoint(relPos1, vel1)
                body1.getVelocityInLocalPoint(relPos2, vel2)
                vel.setSub(vel1, vel2)

                val relVel = cp.normalWorldOnB.dot(vel)
                val combinedRestitution = cp.combinedRestitution

                cpd.penetration = cp.distance
                /**btScalar(info.m_numIterations); */
                cpd.friction = cp.combinedFriction
                cpd.restitution = restitutionCurve(relVel, combinedRestitution)
                if (cpd.restitution <= 0.0) {
                    cpd.restitution = 0.0
                }

                // restitution and penetration work in same direction so
                // rel_vel
                val penVel = -cpd.penetration / info.timeStep

                if (cpd.restitution > penVel) {
                    cpd.penetration = 0.0
                }

                val relaxation = info.damping
                if ((info.solverMode and SolverMode.SOLVER_USE_WARMSTARTING) != 0) {
                    cpd.appliedImpulse *= relaxation
                } else {
                    cpd.appliedImpulse = 0.0
                }

                // for friction
                cpd.prevAppliedImpulse = cpd.appliedImpulse

                // re-calculate friction direction every frame, todo: check if this is really needed
                planeSpace1(cp.normalWorldOnB, cpd.frictionWorldTangential0, cpd.frictionWorldTangential1)

                //#define NO_FRICTION_WARMSTART 1
                //#ifdef NO_FRICTION_WARMSTART
                cpd.accumulatedTangentImpulse0 = 0.0
                cpd.accumulatedTangentImpulse1 = 0.0
                //#endif //NO_FRICTION_WARMSTART
                var denom0 = body0.computeImpulseDenominator(pos1, cpd.frictionWorldTangential0)
                var denom1 = body1.computeImpulseDenominator(pos2, cpd.frictionWorldTangential0)
                var denom = relaxation / (denom0 + denom1)
                cpd.jacDiagABInvTangent0 = denom

                denom0 = body0.computeImpulseDenominator(pos1, cpd.frictionWorldTangential1)
                denom1 = body1.computeImpulseDenominator(pos2, cpd.frictionWorldTangential1)
                denom = relaxation / (denom0 + denom1)
                cpd.jacDiagABInvTangent1 = denom

                totalImpulse.setScale(cpd.appliedImpulse, cp.normalWorldOnB)


                torqueAxis0.setCross(relPos1, cp.normalWorldOnB)
                cpd.angularComponentA.set(torqueAxis0)
                body0.getInvInertiaTensorWorld(tmpMat3).transform(cpd.angularComponentA)

                torqueAxis1.setCross(relPos2, cp.normalWorldOnB)
                cpd.angularComponentB.set(torqueAxis1)
                body1.getInvInertiaTensorWorld(tmpMat3).transform(cpd.angularComponentB)


                ftorqueAxis0.setCross(relPos1, cpd.frictionWorldTangential0)
                cpd.frictionAngularComponent0A.set(ftorqueAxis0)
                body0.getInvInertiaTensorWorld(tmpMat3).transform(cpd.frictionAngularComponent0A)


                ftorqueAxis1.setCross(relPos1, cpd.frictionWorldTangential1)
                cpd.frictionAngularComponent1A.set(ftorqueAxis1)
                body0.getInvInertiaTensorWorld(tmpMat3).transform(cpd.frictionAngularComponent1A)


                ftorqueAxis0.setCross(relPos2, cpd.frictionWorldTangential0)
                cpd.frictionAngularComponent0B.set(ftorqueAxis0)
                body1.getInvInertiaTensorWorld(tmpMat3).transform(cpd.frictionAngularComponent0B)


                ftorqueAxis1.setCross(relPos2, cpd.frictionWorldTangential1)
                cpd.frictionAngularComponent1B.set(ftorqueAxis1)
                body1.getInvInertiaTensorWorld(tmpMat3).transform(cpd.frictionAngularComponent1B)

                /** */

                // apply previous frames impulse on both bodies
                body0.applyImpulse(totalImpulse, relPos1)

                tmpVec.setNegate(totalImpulse)
                body1.applyImpulse(tmpVec, relPos2)
            }
        }

        Stack.subVec(14)
        Stack.subTrans(2)
        Stack.subMat(1)
    }

    @Suppress("unused")
    fun solveCombinedContactFriction(
        body0: RigidBody,
        body1: RigidBody,
        cp: ManifoldPoint,
        info: ContactSolverInfo
    ): Double {
        var maxImpulse = 0.0
        if (cp.distance <= 0.0) {
            val impulse = resolveSingleCollisionCombined(body0, body1, cp, info)
            if (maxImpulse < impulse) {
                maxImpulse = impulse
            }
        }
        return maxImpulse
    }

    fun solve(body0: RigidBody, body1: RigidBody, cp: ManifoldPoint, info: ContactSolverInfo): Double {
        var maxImpulse = 0.0
        if (cp.distance <= 0.0) {
            val cpd = cp.userPersistentData as ConstraintPersistentData?
            val impulse = cpd!!.contactSolverFunc!!.resolveContact(body0, body1, cp, info)
            if (maxImpulse < impulse) {
                maxImpulse = impulse
            }
        }
        return maxImpulse
    }

    fun solveFriction(body0: RigidBody, body1: RigidBody, cp: ManifoldPoint, info: ContactSolverInfo): Double {
        if (cp.distance <= 0.0) {
            val cpd = cp.userPersistentData as ConstraintPersistentData?
            cpd!!.frictionSolverFunc!!.resolveContact(body0, body1, cp, info)
        }
        return 0.0
    }

    override fun reset() {
        this.randSeed = 0
    }

    /**
     * Advanced: Override the default contact solving function for contacts, for certain types of rigidbody<br></br>
     * See RigidBody.contactSolverType and RigidBody.frictionSolverType
     */
    @Suppress("unused")
    fun setContactSolverFunc(func: ContactSolverFunc, type0: Int, type1: Int) {
        contactDispatch[type0][type1] = func
    }

    /**
     * Advanced: Override the default friction solving function for contacts, for certain types of rigidbody<br></br>
     * See RigidBody.contactSolverType and RigidBody.frictionSolverType
     */
    @Suppress("unused")
    fun setFrictionSolverFunc(func: ContactSolverFunc, type0: Int, type1: Int) {
        frictionDispatch[type0][type1] = func
    }

    companion object {
        private val MAX_CONTACT_SOLVER_TYPES = ContactConstraintEnum.MAX_CONTACT_SOLVER_TYPES.ordinal

        private const val SEQUENTIAL_IMPULSE_MAX_SOLVER_POINTS = 16384
        private fun getNormalImpulse(
            contactConstraint: SolverConstraint,
            solverInfo: ContactSolverInfo,
            relVel: Double
        ): Double {
            var positionalError = 0.0
            if (!solverInfo.splitImpulse || (contactConstraint.penetration > solverInfo.splitImpulsePenetrationThreshold)) {
                positionalError = -contactConstraint.penetration * solverInfo.erp / solverInfo.timeStep
            }

            val velocityError = contactConstraint.restitution - relVel // * damping;

            val penetrationImpulse = positionalError * contactConstraint.jacDiagABInv
            val velocityImpulse = velocityError * contactConstraint.jacDiagABInv
            return penetrationImpulse + velocityImpulse
        }

        /** ///////////////////////////////////////////////////////////////////////// */
        fun orderManifoldIndex(v: Long): Int {
            return unpackHigh(v)
        }

        fun orderPointIndex(v: Long): Int {
            return unpackLow(v)
        }

        fun orderIndex(manifoldIndex: Int, pointIndex: Int): Long {
            return pack(manifoldIndex, pointIndex)
        }
    }
}
