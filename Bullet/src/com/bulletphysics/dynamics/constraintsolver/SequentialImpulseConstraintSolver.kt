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
import com.bulletphysics.linearmath.IDebugDraw
import com.bulletphysics.linearmath.MiscUtil.resize
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector3d
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

    /** ///////////////////////////////////////////////////////////////////////// */
    private val bodiesPool = ObjectPool.get(SolverBody::class.java)
    private val constraintsPool = ObjectPool.get(SolverConstraint::class.java)

    private val tmpSolverBodyPool = ArrayList<SolverBody>()
    private val tmpSolverConstraintPool = ArrayList<SolverConstraint>()
    private val tmpSolverFrictionConstraintPool = ArrayList<SolverConstraint>()
    private val orderTmpConstraintPool = IntArrayList()
    private val orderFrictionConstraintPool = IntArrayList()

    val contactDispatch = Array(MAX_CONTACT_SOLVER_TYPES * MAX_CONTACT_SOLVER_TYPES) {
        ContactConstraint.resolveSingleCollision
    }

    val frictionDispatch = Array(MAX_CONTACT_SOLVER_TYPES * MAX_CONTACT_SOLVER_TYPES) {
        ContactConstraint.resolveSingleFriction
    }

    // btSeed2 is used for re-arranging the constraint rows. improves convergence/quality of friction
    var randSeed: Long = 0L

    init {
        BulletGlobals.contactDestroyedCallback = ContactDestroyedCallback {
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

        return abs(r % n).toInt()
    }

    private fun initSolverBody(solverBody: SolverBody, collisionObject: CollisionObject) {
        if (collisionObject is RigidBody) {
            solverBody.angularVelocity.set(collisionObject.angularVelocity)
            solverBody.centerOfMassPosition.set(collisionObject.worldTransform.origin)
            solverBody.friction = collisionObject.friction
            solverBody.invMass = collisionObject.inverseMass
            solverBody.linearVelocity.set(collisionObject.linearVelocity)
            solverBody.originalBody = collisionObject
            solverBody.angularFactor = collisionObject.angularFactor
        } else {
            solverBody.angularVelocity.set(0.0)
            solverBody.centerOfMassPosition.set(collisionObject.worldTransform.origin)
            solverBody.friction = collisionObject.friction
            solverBody.invMass = 0.0
            solverBody.linearVelocity.set(0.0)
            solverBody.originalBody = null
            solverBody.angularFactor = 1.0
        }

        solverBody.pushVelocity.set(0.0)
        solverBody.turnVelocity.set(0.0)
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
            contactConstraint.contactNormal.mul(body1.invMass, tmp)
            body1.internalApplyPushImpulse(tmp, contactConstraint.angularComponentA, normalImpulse)

            contactConstraint.contactNormal.mul(body2.invMass, tmp)
            body2.internalApplyPushImpulse(tmp, contactConstraint.angularComponentB, -normalImpulse)
            Stack.subVec(1)
        }
    }

    /**
     * velocity + friction
     * response  between two dynamic objects with friction
     */
    private fun resolveSingleCollisionCombinedCacheFriendly(
        body1: SolverBody, body2: SolverBody,
        contactConstraint: SolverConstraint,
        solverInfo: ContactSolverInfo
    ) {
        // Optimized version of projected relative velocity, use precomputed cross products with normal
        // body1.getVelocityInLocalPoint(contactConstraint.m_rel_posA,vel1);
        // body2.getVelocityInLocalPoint(contactConstraint.m_rel_posB,vel2);
        // btVector3 vel = vel1 - vel2;
        // btScalar  rel_vel = contactConstraint.m_contactNormal.dot(vel);

        val vel1Dotn = contactConstraint.contactNormal.dot(body1.linearVelocity) +
                contactConstraint.relPos1CrossNormal.dot(body1.angularVelocity)
        val vel2Dotn = contactConstraint.contactNormal.dot(body2.linearVelocity) +
                contactConstraint.relPos2CrossNormal.dot(body2.angularVelocity)

        val relVel = vel1Dotn - vel2Dotn

        var normalImpulse: Double = getNormalImpulse(contactConstraint, solverInfo, relVel)

        // See Erin Catto's GDC 2006 paper: Clamp the accumulated impulse
        val oldNormalImpulse = contactConstraint.appliedImpulse
        val sum = oldNormalImpulse + normalImpulse
        contactConstraint.appliedImpulse = max(0.0, sum)

        normalImpulse = contactConstraint.appliedImpulse - oldNormalImpulse

        val tmp = Stack.newVec()
        contactConstraint.contactNormal.mul(body1.invMass, tmp)
        body1.internalApplyImpulse(tmp, contactConstraint.angularComponentA, normalImpulse)

        contactConstraint.contactNormal.mul(body2.invMass, tmp)
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
            contactConstraint.contactNormal.mul(body1.invMass, tmp)
            body1.internalApplyImpulse(tmp, contactConstraint.angularComponentA, j1)

            contactConstraint.contactNormal.mul(body2.invMass, tmp)
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
        relPos1.cross(solverConstraint.contactNormal, ftorqueAxis1)
        solverConstraint.relPos1CrossNormal.set(ftorqueAxis1)
        if (body0 != null) {
            solverConstraint.angularComponentA.set(ftorqueAxis1)
            body0.invInertiaTensorWorld.transform(solverConstraint.angularComponentA)
        } else {
            solverConstraint.angularComponentA.set(0.0, 0.0, 0.0)
        }

        relPos2.cross(solverConstraint.contactNormal, ftorqueAxis1)
        solverConstraint.relPos2CrossNormal.set(ftorqueAxis1)
        if (body1 != null) {
            solverConstraint.angularComponentB.set(ftorqueAxis1)
            body1.invInertiaTensorWorld.transform(solverConstraint.angularComponentB)
        } else {
            solverConstraint.angularComponentB.set(0.0, 0.0, 0.0)
        }

        val vec = Stack.newVec()
        var denom0 = 0.0
        var denom1 = 0.0
        if (body0 != null) {
            solverConstraint.angularComponentA.cross(relPos1, vec)
            denom0 = body0.inverseMass + normalAxis.dot(vec)
        }
        if (body1 != null) {
            solverConstraint.angularComponentB.cross(relPos2, vec)
            denom1 = body1.inverseMass + normalAxis.dot(vec)
        }

        solverConstraint.jacDiagABInv = relaxation / (denom0 + denom1)

        Stack.subVec(2)
    }

    fun solveGroupCacheFriendlySetup(
        manifoldPtr: List<PersistentManifold>, manifoldOffset: Int, numManifolds: Int,
        constraints: List<TypedConstraint>?, constraintsOffset: Int,
        numConstraints: Int, infoGlobal: ContactSolverInfo
    ) {
        if (numConstraints + numManifolds == 0) return
        pushProfile("solveGroupCacheFriendlySetup")
        try {

            for (i in 0 until numManifolds) {
                val manifold = manifoldPtr[manifoldOffset + i]
                if (manifold.numContacts > 0) {
                    setupManifold(manifold, infoGlobal)
                }
            }

            if (constraints != null) {
                for (j in 0 until numConstraints) {
                    constraints[constraintsOffset + j].buildJacobian()
                }
            }

            preparePools()
        } finally {
            popProfile()
        }
    }

    private fun setupManifold(manifold: PersistentManifold, infoGlobal: ContactSolverInfo) {

        val colObj0 = manifold.body0
        val colObj1 = manifold.body1

        val solverBodyIdA = defineSolverBody(colObj0)
        val solverBodyIdB = defineSolverBody(colObj1)

        for (j in 0 until manifold.numContacts) {
            val cp = manifold.getContactPoint(j)
            if (cp.distance <= 0.0) {
                setupContact(cp, colObj0, colObj1, solverBodyIdA, solverBodyIdB, infoGlobal)
            }
        }
    }

    private fun defineSolverBody(colObj: CollisionObject): Int {
        if (colObj.islandTag >= 0) {
            if (colObj.companionId >= 0) {
                // body has already been converted
                return colObj.companionId
            } else {
                val solverBodyId = tmpSolverBodyPool.size
                val solverBody = bodiesPool.get()
                tmpSolverBodyPool.add(solverBody)
                initSolverBody(solverBody, colObj)
                colObj.companionId = solverBodyId
                return solverBodyId
            }
        } else {
            // create a static body
            val solverBodyId = tmpSolverBodyPool.size
            val solverBody = bodiesPool.get()
            tmpSolverBodyPool.add(solverBody)
            initSolverBody(solverBody, colObj)
            return solverBodyId
        }
    }

    private fun calculateAngularComponent(
        cp: ManifoldPoint, relPos: Vector3d,
        rigidBody: RigidBody?, dst: Vector3d
    ) {
        if (rigidBody != null) {
            relPos.cross(cp.normalWorldOnB, dst) // torqueAxis
            rigidBody.invInertiaTensorWorld.transform(dst)
        } else {
            dst.set(0.0, 0.0, 0.0)
        }
    }

    private fun calculateJacobianDiagonal(
        cp: ManifoldPoint,
        relPos1: Vector3d, relPos2: Vector3d,
        rb0: RigidBody?, rb1: RigidBody?,
        solverConstraint: SolverConstraint,
        relaxation: Double
    ) {
        //#ifdef COMPUTE_IMPULSE_DENOM
        //btScalar denom0 = rb0->computeImpulseDenominator(pos1,cp.m_normalWorldOnB);
        //btScalar denom1 = rb1->computeImpulseDenominator(pos2,cp.m_normalWorldOnB);
        //#else
        val tmp = Stack.newVec()
        var denominator0 = 0.0
        var denominator1 = 0.0
        if (rb0 != null) {
            solverConstraint.angularComponentA.cross(relPos1, tmp)
            denominator0 = rb0.inverseMass + cp.normalWorldOnB.dot(tmp)
        }
        if (rb1 != null) {
            solverConstraint.angularComponentB.cross(relPos2, tmp)
            denominator1 = rb1.inverseMass + cp.normalWorldOnB.dot(tmp)
        }

        //#endif //COMPUTE_IMPULSE_DENOM
        solverConstraint.jacDiagABInv = relaxation / (denominator0 + denominator1)
        Stack.subVec(1)
    }

    private fun calculateVelocity(relPos: Vector3d, rigidBody: RigidBody?, dst: Vector3d) {
        if (rigidBody != null) {
            rigidBody.getVelocityInLocalPoint(relPos, dst)
        } else {
            dst.set(0.0, 0.0, 0.0)
        }
    }

    private fun calculateVelocity(
        relPos1: Vector3d, relPos2: Vector3d,
        rb1: RigidBody?, rb2: RigidBody?, dst: Vector3d
    ) {
        val vel1 = Stack.newVec()
        val vel2 = Stack.newVec()
        calculateVelocity(relPos1, rb1, vel1)
        calculateVelocity(relPos2, rb2, vel2)
        vel1.sub(vel2, dst)
        Stack.subVec(2)
    }

    private fun warmstarting(
        solverConstraint: SolverConstraint, solverBodyId: Int,
        rigidBody: RigidBody, first: Boolean, tmp: Vector3d
    ) {
        solverConstraint.contactNormal.mul(rigidBody.inverseMass, tmp)
        tmpSolverBodyPool[solverBodyId]
            .internalApplyImpulse(
                tmp,
                if (first) solverConstraint.angularComponentA else solverConstraint.angularComponentB,
                if (first) solverConstraint.appliedImpulse else -solverConstraint.appliedImpulse
            )
    }

    private fun warmstarting(
        baseImpulse: Double,
        solverConstraint: SolverConstraint,
        frictionConstraint: SolverConstraint,
        rb1: RigidBody?, rb2: RigidBody?,
        infoGlobal: ContactSolverInfo, tmp: Vector3d,
    ) {
        if (infoGlobal.solverMode.hasFlag(SolverMode.SOLVER_USE_WARMSTARTING)) {
            frictionConstraint.appliedImpulse = baseImpulse * infoGlobal.warmstartingFactor
            if (rb1 != null) warmstarting(frictionConstraint, solverConstraint.solverBodyIdA, rb1, true, tmp)
            if (rb2 != null) warmstarting(frictionConstraint, solverConstraint.solverBodyIdB, rb2, false, tmp)
        } else {
            frictionConstraint.appliedImpulse = 0.0
        }
    }

    private fun defineLateralFriction(
        cp: ManifoldPoint, relVel: Double, vel: Vector3d,
        solverConstraint: SolverConstraint, frictionIndex: Int,
        solverBodyIdA: Int, solverBodyIdB: Int,
        relPos1: Vector3d, relPos2: Vector3d, colObj1: CollisionObject, colObj2: CollisionObject,
        relaxation: Double
    ) {
        solverConstraint.frictionIndex = tmpSolverFrictionConstraintPool.size
        if (!cp.lateralFrictionInitialized) {
            cp.normalWorldOnB.mul(relVel, cp.lateralFrictionDir1)
            vel.sub(cp.lateralFrictionDir1, cp.lateralFrictionDir1)

            val latRelVel = cp.lateralFrictionDir1.lengthSquared()
            if (latRelVel > BulletGlobals.FLT_EPSILON) {

                cp.lateralFrictionDir1.mul(1.0 / sqrt(latRelVel))
                addFrictionConstraint(
                    cp.lateralFrictionDir1,
                    solverBodyIdA, solverBodyIdB, frictionIndex, cp,
                    relPos1, relPos2, colObj1, colObj2, relaxation
                )
                cp.lateralFrictionDir1.cross(cp.normalWorldOnB, cp.lateralFrictionDir2)
                cp.lateralFrictionDir2.normalize() //??
                addFrictionConstraint(
                    cp.lateralFrictionDir2,
                    solverBodyIdA, solverBodyIdB, frictionIndex, cp,
                    relPos1, relPos2, colObj1, colObj2, relaxation
                )
            } else {
                // re-calculate friction direction every frame, todo: check if this is really needed
                cp.normalWorldOnB.findSystem(cp.lateralFrictionDir1, cp.lateralFrictionDir2, false)
                addFrictionConstraint(
                    cp.lateralFrictionDir1,
                    solverBodyIdA, solverBodyIdB, frictionIndex, cp,
                    relPos1, relPos2, colObj1, colObj2, relaxation
                )
                addFrictionConstraint(
                    cp.lateralFrictionDir2,
                    solverBodyIdA, solverBodyIdB, frictionIndex, cp,
                    relPos1, relPos2, colObj1, colObj2, relaxation
                )
            }
            cp.lateralFrictionInitialized = true
        } else {
            addFrictionConstraint(
                cp.lateralFrictionDir1,
                solverBodyIdA, solverBodyIdB, frictionIndex, cp,
                relPos1, relPos2, colObj1, colObj2, relaxation
            )
            addFrictionConstraint(
                cp.lateralFrictionDir2,
                solverBodyIdA, solverBodyIdB, frictionIndex, cp,
                relPos1, relPos2, colObj1, colObj2, relaxation
            )
        }
    }

    private fun setupContact(
        cp: ManifoldPoint, colObj1: CollisionObject, colObj2: CollisionObject,
        solverBodyIdA: Int, solverBodyIdB: Int, infoGlobal: ContactSolverInfo
    ) {

        val tmp = Stack.newVec()

        val relPos1 = cp.positionWorldOnA.sub(colObj1.worldTransform.origin, Stack.newVec())
        val relPos2 = cp.positionWorldOnB.sub(colObj2.worldTransform.origin, Stack.newVec())

        val relaxation = 1.0
        val frictionIndex = tmpSolverConstraintPool.size
        val solverConstraint = constraintsPool.get()
        tmpSolverConstraintPool.add(solverConstraint)

        val rb1 = colObj1 as? RigidBody
        val rb2 = colObj2 as? RigidBody

        solverConstraint.solverBodyIdA = solverBodyIdA
        solverConstraint.solverBodyIdB = solverBodyIdB
        solverConstraint.constraintType = SolverConstraintType.SOLVER_CONTACT_1D
        solverConstraint.originalContactPoint = cp

        calculateAngularComponent(cp, relPos1, rb1, solverConstraint.angularComponentA)
        calculateAngularComponent(cp, relPos2, rb2, solverConstraint.angularComponentB)

        calculateJacobianDiagonal(cp, relPos1, relPos2, rb1, rb2, solverConstraint, relaxation)

        solverConstraint.contactNormal.set(cp.normalWorldOnB)
        relPos1.cross(cp.normalWorldOnB, solverConstraint.relPos1CrossNormal)
        relPos2.cross(cp.normalWorldOnB, solverConstraint.relPos2CrossNormal)

        val vel = Stack.newVec()
        calculateVelocity(relPos1, relPos2, rb1, rb2, vel)
        val relVel = cp.normalWorldOnB.dot(vel)

        solverConstraint.penetration = min(cp.distance + infoGlobal.linearSlop, 0.0)

        //solverConstraint.m_penetration = cp.getDistance();
        solverConstraint.friction = cp.combinedFriction
        solverConstraint.restitution = restitutionCurve(relVel, cp.combinedRestitution)
        if (solverConstraint.restitution <= 0.0) {
            solverConstraint.restitution = 0.0
        }

        val penetrationVelocity = -solverConstraint.penetration / infoGlobal.timeStep
        if (solverConstraint.restitution > penetrationVelocity) {
            solverConstraint.penetration = 0.0
        }

        // warm starting (or zero if disabled)
        warmstarting(cp.appliedImpulse, solverConstraint, solverConstraint, rb1, rb2, infoGlobal, tmp)
        solverConstraint.appliedPushImpulse = 0.0

        defineLateralFriction(
            cp, relVel, vel, solverConstraint, frictionIndex,
            solverBodyIdA, solverBodyIdB, relPos1, relPos2, colObj1, colObj2, relaxation
        )

        val frictionConstraint1 = tmpSolverFrictionConstraintPool[solverConstraint.frictionIndex]
        val frictionConstraint2 = tmpSolverFrictionConstraintPool[solverConstraint.frictionIndex + 1]
        warmstarting(cp.appliedImpulseLateral1, solverConstraint, frictionConstraint1, rb1, rb2, infoGlobal, tmp)
        warmstarting(cp.appliedImpulseLateral2, solverConstraint, frictionConstraint2, rb1, rb2, infoGlobal, tmp)

        Stack.subVec(4)
    }

    private fun preparePools() {
        val numConstraintPool = tmpSolverConstraintPool.size
        val numFrictionPool = tmpSolverFrictionConstraintPool.size

        resize(orderTmpConstraintPool, numConstraintPool, 0)
        resize(orderFrictionConstraintPool, numFrictionPool, 0)

        for (j in 0 until numConstraintPool) {
            orderTmpConstraintPool[j] = j
        }
        for (j in 0 until numFrictionPool) {
            orderFrictionConstraintPool[j] = j
        }
    }

    fun solveGroupCacheFriendlyIterations(
        constraints: List<TypedConstraint>?, constraintsOffset: Int, numConstraints: Int,
        infoGlobal: ContactSolverInfo
    ) {
        pushProfile("solveGroupCacheFriendlyIterations")
        try {

            val numConstraintPool = tmpSolverConstraintPool.size
            val numFrictionPool = tmpSolverFrictionConstraintPool.size

            // should traverse the contacts random order...
            for (iteration in 0 until infoGlobal.numIterations) {

                if (infoGlobal.solverMode.hasFlag(SolverMode.SOLVER_RANDOMIZE_ORDER) &&
                    (iteration and 7) == 0
                ) randomizeOrder(numConstraintPool, numFrictionPool)

                if (constraints != null) {
                    solveConstraints(constraints, constraintsOffset, numConstraints, infoGlobal)
                }

                solvePoolConstraints(infoGlobal)
                solveFrictionConstraints()
            }

            if (infoGlobal.splitImpulse) {
                repeat(infoGlobal.numIterations) {
                    splitImpulse(infoGlobal)
                }
            }
        } finally {
            popProfile()
        }
    }

    private fun randomizeOrder(numConstraintPool: Int, numFrictionPool: Int) {
        for (j in 0 until numConstraintPool) {
            val tmp = orderTmpConstraintPool.get(j)
            val swapi = randInt2(j + 1)
            orderTmpConstraintPool.set(j, orderTmpConstraintPool.get(swapi))
            orderTmpConstraintPool.set(swapi, tmp)
        }

        for (j in 0 until numFrictionPool) {
            val tmp = orderFrictionConstraintPool.get(j)
            val swapi = randInt2(j + 1)
            orderFrictionConstraintPool.set(j, orderFrictionConstraintPool.get(swapi))
            orderFrictionConstraintPool.set(swapi, tmp)
        }
    }

    private fun solveConstraints(
        constraints: List<TypedConstraint>, constraintsOffset: Int, numConstraints: Int,
        infoGlobal: ContactSolverInfo
    ) {
        for (k in 0 until numConstraints) {
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
        }
    }

    private fun solvePoolConstraints(infoGlobal: ContactSolverInfo) {
        for (k in 0 until tmpSolverConstraintPool.size) {
            val solveManifold = tmpSolverConstraintPool[orderTmpConstraintPool.get(k)]
            resolveSingleCollisionCombinedCacheFriendly(
                tmpSolverBodyPool[solveManifold.solverBodyIdA],
                tmpSolverBodyPool[solveManifold.solverBodyIdB], solveManifold, infoGlobal
            )
        }
    }

    private fun solveFrictionConstraints() {
        for (k in 0 until tmpSolverFrictionConstraintPool.size) {

            val solveManifold = tmpSolverFrictionConstraintPool[orderFrictionConstraintPool.get(k)]
            val totalImpulse = tmpSolverConstraintPool[solveManifold.frictionIndex].appliedImpulse +
                    tmpSolverConstraintPool[solveManifold.frictionIndex].appliedPushImpulse

            resolveSingleFrictionCacheFriendly(
                tmpSolverBodyPool[solveManifold.solverBodyIdA],
                tmpSolverBodyPool[solveManifold.solverBodyIdB], solveManifold,
                totalImpulse
            )
        }
    }

    private fun splitImpulse(infoGlobal: ContactSolverInfo) {
        val numPoolConstraints = tmpSolverConstraintPool.size
        for (j in 0 until numPoolConstraints) {
            val solveManifold = tmpSolverConstraintPool[orderTmpConstraintPool.get(j)]
            resolveSplitPenetrationImpulseCacheFriendly(
                tmpSolverBodyPool[solveManifold.solverBodyIdA],
                tmpSolverBodyPool[solveManifold.solverBodyIdB], solveManifold, infoGlobal
            )
        }
    }

    fun solveGroupCacheFriendly(
        manifoldPtr: List<PersistentManifold>, manifoldOffset: Int, numManifolds: Int,
        constraints: List<TypedConstraint>?, constraintsOffset: Int, numConstraints: Int,
        infoGlobal: ContactSolverInfo
    ) {
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
            pt.appliedImpulseLateral1 = tmpSolverFrictionConstraintPool[solveManifold.frictionIndex].appliedImpulse
            pt.appliedImpulseLateral2 = tmpSolverFrictionConstraintPool[solveManifold.frictionIndex + 1].appliedImpulse
            // do a callback here?
        }

        if (infoGlobal.splitImpulse) {
            for (i in tmpSolverBodyPool.indices) {
                tmpSolverBodyPool[i].writebackVelocity(infoGlobal.timeStep)
            }
        } else {
            for (i in tmpSolverBodyPool.indices) {
                tmpSolverBodyPool[i].writebackVelocity()
            }
        }

        bodiesPool.releaseAll(tmpSolverBodyPool)
        tmpSolverBodyPool.clear()

        constraintsPool.releaseAll(tmpSolverConstraintPool)
        tmpSolverConstraintPool.clear()

        constraintsPool.releaseAll(tmpSolverFrictionConstraintPool)
        tmpSolverFrictionConstraintPool.clear()
    }

    /**
     * Sequentially applies impulses.
     */
    override fun solveGroup(
        bodies: List<CollisionObject>, numBodies: Int,
        manifold: List<PersistentManifold>, manifoldOffset: Int, numManifolds: Int,
        constraints: List<TypedConstraint>?, constraintsOffset: Int, numConstraints: Int,
        info: ContactSolverInfo, debugDrawer: IDebugDraw?, dispatcher: Dispatcher
    ) {
        pushProfile("solveGroup")
        try {
            // you need to provide at least some bodies
            // SimpleDynamicsWorld needs to switch off SOLVER_CACHE_FRIENDLY
            checkNotNull(bodies)
            assert(numBodies != 0)
            return solveGroupCacheFriendly(
                manifold, manifoldOffset, numManifolds,
                constraints, constraintsOffset, numConstraints,
                info
            )
        } finally {
            popProfile()
        }
    }

    fun solve(body0: RigidBody, body1: RigidBody, cp: ManifoldPoint, info: ContactSolverInfo): Double {
        var maxImpulse = 0.0
        if (cp.distance <= 0.0) {
            val cpd = cp.userPersistentData as ConstraintPersistentData
            val impulse = cpd.contactSolverFunc!!.resolveContact(body0, body1, cp, info)
            if (maxImpulse < impulse) {
                maxImpulse = impulse
            }
        }
        return maxImpulse
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
        contactDispatch[type0 * MAX_CONTACT_SOLVER_TYPES + type1] = func
    }

    /**
     * Advanced: Override the default friction solving function for contacts, for certain types of rigidbody<br></br>
     * See RigidBody.contactSolverType and RigidBody.frictionSolverType
     */
    @Suppress("unused")
    fun setFrictionSolverFunc(func: ContactSolverFunc, type0: Int, type1: Int) {
        frictionDispatch[type0 * MAX_CONTACT_SOLVER_TYPES + type1] = func
    }

    companion object {
        private val MAX_CONTACT_SOLVER_TYPES = ContactConstraintEnum.MAX_CONTACT_SOLVER_TYPES.ordinal
        private fun getNormalImpulse(
            contactConstraint: SolverConstraint,
            solverInfo: ContactSolverInfo,
            relVel: Double
        ): Double {
            var positionalError = 0.0
            if (!solverInfo.splitImpulse || (contactConstraint.penetration > solverInfo.splitImpulsePenetrationThreshold)) {
                positionalError = -contactConstraint.penetration * solverInfo.erp / solverInfo.timeStep
            }

            val velocityError = contactConstraint.restitution - relVel // * damping
            val penetrationImpulse = positionalError * contactConstraint.jacDiagABInv
            val velocityImpulse = velocityError * contactConstraint.jacDiagABInv
            return penetrationImpulse + velocityImpulse
        }
    }
}
