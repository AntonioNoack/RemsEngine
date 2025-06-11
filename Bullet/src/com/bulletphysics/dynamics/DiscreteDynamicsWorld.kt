package com.bulletphysics.dynamics

import com.bulletphysics.BulletGlobals
import com.bulletphysics.BulletStats
import com.bulletphysics.BulletStats.popProfile
import com.bulletphysics.BulletStats.pushProfile
import com.bulletphysics.collision.broadphase.*
import com.bulletphysics.collision.dispatch.CollisionConfiguration
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.dispatch.SimulationIslandManager
import com.bulletphysics.collision.dispatch.SimulationIslandManager.IslandCallback
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver
import com.bulletphysics.dynamics.constraintsolver.ContactSolverInfo
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint
import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import com.bulletphysics.linearmath.*
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.getElement
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setSub
import kotlin.math.abs
import kotlin.math.min

/**
 * DiscreteDynamicsWorld provides discrete rigid body simulation.
 *
 * @author jezek2
 */
class DiscreteDynamicsWorld(
    dispatcher: Dispatcher,
    pairCache: BroadphaseInterface,
    override var constraintSolver: ConstraintSolver,
    collisionConfiguration: CollisionConfiguration
) : DynamicsWorld(dispatcher, pairCache, collisionConfiguration) {

    val simulationIslandManager = SimulationIslandManager()
    val constraints = ArrayList<TypedConstraint>()
    val gravity = Vector3d(0.0, -10.0, 0.0)

    //for variable timesteps
    var localTime: Double = 1.0 / 60.0

    val vehicles = ArrayList<RaycastVehicle>()
    val actions = ArrayList<ActionInterface?>()

    fun saveKinematicState(timeStep: Double) {
        for (i in collisionObjects.indices) {
            val colObj = collisionObjects[i]
            val body = colObj as? RigidBody ?: continue
            if (body.activationState != CollisionObject.ISLAND_SLEEPING && body.isKinematicObject) {
                // to calculate velocities next frame
                body.saveKinematicState(timeStep)
            }
        }
    }

    override fun debugDrawWorld() {
        val debugDrawer = debugDrawer ?: return
        if ((debugDrawer.debugMode and DebugDrawModes.DRAW_CONTACT_POINTS) != 0) {
            val color = Stack.newVec()
            color.set(0.0, 0.0, 0.0)
            for (i in 0 until dispatcher.numManifolds) {
                val contactManifold = dispatcher.getManifoldByIndexInternal(i)
                val numContacts = contactManifold.numContacts
                for (j in 0 until numContacts) {
                    val cp = contactManifold.getContactPoint(j)
                    debugDrawer.drawContactPoint(
                        cp.positionWorldOnB,
                        cp.normalWorldOnB,
                        cp.distance,
                        cp.lifeTime,
                        color
                    )
                }
            }
            Stack.subVec(1) // color
        }

        if ((debugDrawer.debugMode and (DebugDrawModes.DRAW_WIREFRAME or DebugDrawModes.DRAW_AABB)) != 0) {

            val tmpTrans = Stack.newTrans()
            val minAabb = Stack.newVec()
            val maxAabb = Stack.newVec()
            val colorvec = Stack.newVec()

            // todo: iterate over awake simulation islands!
            for (i in collisionObjects.indices) {
                val colObj = collisionObjects[i]
                if ((debugDrawer.debugMode and DebugDrawModes.DRAW_WIREFRAME) != 0) {
                    val color = Stack.newVec()
                    color.set(255.0, 255.0, 255.0)
                    when (colObj.activationState) {
                        CollisionObject.ACTIVE_TAG -> color.set(255.0, 255.0, 255.0)
                        CollisionObject.ISLAND_SLEEPING -> color.set(0.0, 255.0, 0.0)
                        CollisionObject.WANTS_DEACTIVATION -> color.set(0.0, 255.0, 255.0)
                        CollisionObject.DISABLE_DEACTIVATION -> color.set(255.0, 0.0, 0.0)
                        CollisionObject.DISABLE_SIMULATION -> color.set(255.0, 255.0, 0.0)
                        else -> {
                            color.set(255.0, 0.0, 0.0)
                        }
                    }

                    debugDrawObject(colObj.getWorldTransform(tmpTrans))
                }
                if ((debugDrawer.debugMode and DebugDrawModes.DRAW_AABB) != 0) {
                    colorvec.set(1.0, 0.0, 0.0)
                    colObj.collisionShape!!.getAabb(colObj.getWorldTransform(tmpTrans), minAabb, maxAabb)
                    debugDrawer.drawAabb(minAabb, maxAabb, colorvec)
                }
            }

            val wheelColor = Stack.newVec()
            val wheelPosWS = Stack.newVec()
            val axle = Stack.newVec()
            val tmp = Stack.newVec()

            for (i in vehicles.indices) {
                val vehicle = vehicles[i]
                for (v in 0 until vehicle.numWheels) {
                    wheelColor.set(0.0, 255.0, 255.0)
                    if (vehicle.getWheelInfo(v).raycastInfo.isInContact) {
                        wheelColor.set(0.0, 0.0, 255.0)
                    } else {
                        wheelColor.set(255.0, 0.0, 255.0)
                    }

                    wheelPosWS.set(vehicle.getWheelInfo(v).worldTransform.origin)

                    axle.set(
                        vehicle.getWheelInfo(v).worldTransform.basis
                            .getElement(0, vehicle.rightAxis),
                        vehicle.getWheelInfo(v).worldTransform.basis
                            .getElement(1, vehicle.rightAxis),
                        vehicle.getWheelInfo(v).worldTransform.basis
                            .getElement(2, vehicle.rightAxis)
                    )


                    //m_vehicle->getWheelInfo(v).m_raycastInfo.m_wheelAxleWS
                    //debug wheels (cylinders)
                    tmp.setAdd(wheelPosWS, axle)
                    debugDrawer.drawLine(wheelPosWS, tmp, wheelColor)
                    debugDrawer.drawLine(
                        wheelPosWS,
                        vehicle.getWheelInfo(v).raycastInfo.contactPointWS,
                        wheelColor
                    )
                }
            }

            if (debugDrawer.debugMode != 0) {
                for(i in actions.indices) {
                    actions[i]!!.debugDraw(debugDrawer)
                }
            }
        }
    }

    override fun clearForces() {
        // todo: iterate over awake simulation islands!
        for (i in 0 until collisionObjects.size) {
            val colObj = collisionObjects[i]

            val body = colObj as? RigidBody
            body?.clearForces()
        }
    }

    /**
     * Apply gravity, call this once per timestep.
     */
    fun applyGravity() {
        // todo: iterate over awake simulation islands!
        for (i in 0 until collisionObjects.size) {
            val colObj = collisionObjects[i]

            val body = colObj as? RigidBody
            if (body != null && body.isActive) {
                body.applyGravity()
            }
        }
    }

    fun synchronizeMotionStates() {
        val interpolatedTransform = Stack.newTrans()

        val tmpTrans = Stack.newTrans()
        val tmpLinVel = Stack.newVec()
        val tmpAngVel = Stack.newVec()

        var stackPos: IntArray? = null
        // todo: iterate over awake simulation islands!
        for (i in 0 until collisionObjects.size) {
            val colObj = collisionObjects[i]

            val body = colObj as? RigidBody
            if (body != null && body.motionState != null && !body.isStaticOrKinematicObject) {
                // we need to call the update at least once, even for sleeping objects
                // otherwise the 'graphics' transform never updates properly
                // so todo: add 'dirty' flag
                stackPos = Stack.getPosition(stackPos)
                TransformUtil.integrateTransform(
                    body.getInterpolationWorldTransform(tmpTrans),
                    body.getInterpolationLinearVelocity(tmpLinVel),
                    body.getInterpolationAngularVelocity(tmpAngVel),
                    localTime * body.hitFraction, interpolatedTransform
                )
                body.motionState!!.setWorldTransform(interpolatedTransform)
                Stack.reset(stackPos)
            }
        }
        Stack.subTrans(2)
        Stack.subVec(2)

        if (debugDrawer != null && (debugDrawer!!.debugMode and DebugDrawModes.DRAW_WIREFRAME) != 0) {
            for (i in vehicles.indices) {
                val vehicle = vehicles[i]
                for (v in 0 until vehicle.numWheels) {
                    stackPos = Stack.getPosition(stackPos)
                    // synchronize the wheels with the (interpolated) chassis worldtransform
                    vehicle.updateWheelTransform(v, true)
                    Stack.reset(stackPos)
                }
            }
        }
    }

    override fun stepSimulation(timeStep: Double, maxSubSteps: Int, fixedTimeStep: Double): Int {
        var maxSubSteps = maxSubSteps
        var fixedTimeStep = fixedTimeStep
        startProfiling()

        val t0 = System.nanoTime()

        pushProfile("stepSimulation")
        try {
            var numSimulationSubSteps = 0
            if (maxSubSteps != 0) {
                // fixed timestep with interpolation
                localTime += timeStep
                if (localTime >= fixedTimeStep) {
                    numSimulationSubSteps = (localTime / fixedTimeStep).toInt()
                    localTime -= numSimulationSubSteps * fixedTimeStep
                }
            } else {
                //variable timestep
                fixedTimeStep = timeStep
                localTime = timeStep
                if (abs(timeStep) >= BulletGlobals.FLT_EPSILON) {
                    numSimulationSubSteps = 1
                    maxSubSteps = 1
                }
            }

            // process some debugging flags
            if (debugDrawer != null) {
                BulletGlobals.isDeactivationDisabled = (debugDrawer!!.debugMode and DebugDrawModes.NO_DEACTIVATION) != 0
            }
            if (numSimulationSubSteps != 0) {
                saveKinematicState(fixedTimeStep)

                applyGravity()

                // clamp the number of substeps, to prevent simulation grinding spiralling down to a halt
                val clampedSimulationSteps = min(numSimulationSubSteps, maxSubSteps)

                repeat(clampedSimulationSteps) {
                    internalSingleStepSimulation(fixedTimeStep)
                    synchronizeMotionStates()
                }
            }

            synchronizeMotionStates()

            clearForces()

            //#ifndef BT_NO_PROFILE
            CProfileManager.incrementFrameCounter()

            //#endif //BT_NO_PROFILE
            return numSimulationSubSteps
        } finally {
            popProfile()

            BulletStats.stepSimulationTime = (System.nanoTime() - t0) / 1000000
        }
    }

    fun internalSingleStepSimulation(timeStep: Double) {
        pushProfile("internalSingleStepSimulation")
        try {
            // apply gravity, predict motion
            predictUnconstrainedMotion(timeStep)

            val dispatchInfo = dispatchInfo

            dispatchInfo.timeStep = timeStep
            dispatchInfo.stepCount = 0
            dispatchInfo.debugDraw = debugDrawer

            // perform collision detection
            performDiscreteCollisionDetection()

            calculateSimulationIslands()

            solverInfo.timeStep = timeStep

            // solve contact and other joint constraints
            solveConstraints(solverInfo)

            removeBrokenConstraints()

            //CallbackTriggers();

            // integrate transforms
            integrateTransforms(timeStep)

            // update vehicle simulation
            updateActions(timeStep)

            // update vehicle simulation
            updateVehicles(timeStep)

            updateActivationState(timeStep)

            internalTickCallback?.internalTick(this, timeStep)
        } finally {
            popProfile()
        }
    }

    private fun removeBrokenConstraints() {
        val callback = this.brokenConstraintCallback
        constraints.removeIf { constraint: TypedConstraint? ->
            if (callback != null && constraint!!.isBroken) {
                callback.onBrokenConstraint(constraint)
            }
            constraint!!.isBroken
        }
    }

    override fun setGravity(gravity: Vector3d) {
        this.gravity.set(gravity)
        var i = 0
        val l: Int = collisionObjects.size
        while (i < l) {
            val colObj = collisionObjects[i]
            val body = colObj as? RigidBody
            body?.setGravity(gravity)
            i++
        }
    }

    override fun getGravity(out: Vector3d): Vector3d {
        out.set(gravity)
        return out
    }

    override fun removeRigidBody(body: RigidBody) {
        removeCollisionObject(body)
    }

    override fun addRigidBody(body: RigidBody) {
        if (!body.isStaticOrKinematicObject) {
            body.setGravity(gravity)
        }

        if (body.collisionShape != null) {
            val isDynamic = !(body.isStaticObject || body.isKinematicObject)
            val collisionFilterGroup =
                if (isDynamic) CollisionFilterGroups.DEFAULT_FILTER else CollisionFilterGroups.STATIC_FILTER
            val collisionFilterMask =
                if (isDynamic) CollisionFilterGroups.ALL_FILTER else (CollisionFilterGroups.ALL_FILTER.toInt() xor CollisionFilterGroups.STATIC_FILTER.toInt()).toShort()

            addCollisionObject(body, collisionFilterGroup, collisionFilterMask)
        }
    }

    @Suppress("unused")
    fun addRigidBody(body: RigidBody, group: Short, mask: Short) {
        if (!body.isStaticOrKinematicObject) {
            body.setGravity(gravity)
        }

        if (body.collisionShape != null) {
            addCollisionObject(body, group, mask)
        }
    }

    fun updateActions(timeStep: Double) {
        pushProfile("updateActions")
        try {
            for (i in actions.indices) {
                actions[i]!!.updateAction(this, timeStep)
            }
        } finally {
            popProfile()
        }
    }

    fun updateVehicles(timeStep: Double) {
        pushProfile("updateVehicles")
        try {
            for (i in vehicles.indices) {
                val vehicle = vehicles[i]
                vehicle.updateVehicle(timeStep)
            }
        } finally {
            popProfile()
        }
    }

    fun updateActivationState(timeStep: Double) {
        pushProfile("updateActivationState")
        try {
            var stackPos: IntArray? = null
            for (i in 0 until collisionObjects.size) {
                stackPos = Stack.getPosition(stackPos)
                val colObj = collisionObjects[i]
                val body = colObj as? RigidBody
                if (body != null) {
                    body.updateDeactivation(timeStep)

                    if (body.wantsSleeping()) {
                        if (body.isStaticOrKinematicObject) {
                            body.setActivationStateMaybe(CollisionObject.ISLAND_SLEEPING)
                        } else {
                            if (body.activationState == CollisionObject.ACTIVE_TAG) {
                                body.setActivationStateMaybe(CollisionObject.WANTS_DEACTIVATION)
                            }
                            if (body.activationState == CollisionObject.ISLAND_SLEEPING) {
                                val zero = Stack.borrowVec()
                                zero.set(0.0, 0.0, 0.0)
                                body.setAngularVelocity(zero)
                                body.setLinearVelocity(zero)
                            }
                        }
                    } else {
                        if (body.activationState != CollisionObject.DISABLE_DEACTIVATION) {
                            body.setActivationStateMaybe(CollisionObject.ACTIVE_TAG)
                        }
                    }
                }
                Stack.reset(stackPos)
            }
        } finally {
            popProfile()
        }
    }

    override fun addConstraint(constraint: TypedConstraint, disableCollisionsBetweenLinkedBodies: Boolean) {
        constraints.add(constraint)
        if (disableCollisionsBetweenLinkedBodies) {
            constraint.rigidBodyA.addConstraintRef(constraint)
            constraint.rigidBodyB.addConstraintRef(constraint)
        }
    }

    override fun removeConstraint(constraint: TypedConstraint) {
        constraints.remove(constraint)
        constraint.rigidBodyA.removeConstraintRef(constraint)
        constraint.rigidBodyB.removeConstraintRef(constraint)
    }

    override fun addAction(action: ActionInterface) {
        actions.add(action)
    }

    override fun removeAction(action: ActionInterface) {
        actions.remove(action)
    }

    override fun addVehicle(vehicle: RaycastVehicle) {
        vehicles.add(vehicle)
    }

    override fun removeVehicle(vehicle: RaycastVehicle) {
        vehicles.remove(vehicle)
    }

    private class InplaceSolverIslandCallback : IslandCallback() {
        var solverInfo: ContactSolverInfo? = null
        var solver: ConstraintSolver? = null
        var sortedConstraints: List<TypedConstraint>? = null
        var numConstraints: Int = 0
        var debugDrawer: IDebugDraw? = null

        //StackAlloc* m_stackAlloc;
        var dispatcher: Dispatcher? = null

        fun init(
            solverInfo: ContactSolverInfo,
            solver: ConstraintSolver,
            sortedConstraints: List<TypedConstraint>?,
            numConstraints: Int,
            debugDrawer: IDebugDraw?,
            dispatcher: Dispatcher
        ) {
            this.solverInfo = solverInfo
            this.solver = solver
            this.sortedConstraints = sortedConstraints
            this.numConstraints = numConstraints
            this.debugDrawer = debugDrawer
            this.dispatcher = dispatcher
        }

        override fun processIsland(
            bodies: List<CollisionObject>, numBodies: Int,
            manifolds: List<PersistentManifold>, manifoldsOffset: Int, numManifolds: Int,
            islandId: Int
        ) {
            if (islandId < 0) {
                // we don't split islands, so all constraints/contact manifolds/bodies are passed into the solver regardless the island id
                solver!!.solveGroup(
                    bodies, numBodies, manifolds, manifoldsOffset, numManifolds,
                    sortedConstraints, 0, numConstraints,
                    solverInfo!!, debugDrawer, dispatcher!!
                )
            } else {

                // also add all non-contact constraints/joints for this island
                //ObjectArrayList<TypedConstraint> startConstraint = null;
                var startConstraintIdx = -1
                var numCurConstraints = 0

                val sortedConstraints = sortedConstraints

                if (sortedConstraints != null) {
                    // find the first constraint for this island
                    for (i in 0 until numConstraints) {
                        if (getConstraintIslandId(sortedConstraints[i]) == islandId) {
                            //startConstraint = &m_sortedConstraints[i];
                            //startConstraint = sortedConstraints.subList(i, sortedConstraints.size);
                            startConstraintIdx = i
                            break
                        }
                    }
                    // count the number of constraints in this island
                    for (i in 0 until numConstraints) {
                        if (getConstraintIslandId(sortedConstraints[i]) == islandId) {
                            numCurConstraints++
                        }
                    }
                }

                // only call solveGroup if there is some work: avoid virtual function call, its overhead can be excessive
                if (numManifolds + numCurConstraints > 0) {
                    solver!!.solveGroup(
                        bodies, numBodies, manifolds, manifoldsOffset, numManifolds, sortedConstraints,
                        startConstraintIdx, numCurConstraints, solverInfo!!, debugDrawer, dispatcher!!
                    )
                }
            }
        }
    }

    private val sortedConstraints = ArrayList<TypedConstraint>()
    private val solverCallback = InplaceSolverIslandCallback()

    fun solveConstraints(solverInfo: ContactSolverInfo) {
        pushProfile("solveConstraints")
        try {
            // sorted version of all btTypedConstraint, based on islandId
            sortedConstraints.clear()
            sortedConstraints.addAll(constraints)
            sortedConstraints.sortWith(sortConstraintOnIslandPredicate)

            val constraintsPtr = if (numConstraints != 0) sortedConstraints else null

            solverCallback.init(
                solverInfo,
                constraintSolver,
                constraintsPtr,
                sortedConstraints.size,
                debugDrawer,  /*,m_stackAlloc*/
                dispatcher
            )

            // solve all the constraints for this island
            simulationIslandManager.buildAndProcessIslands(
                dispatcher,
                collisionObjects,
                solverCallback
            )
        } finally {
            popProfile()
        }
    }

    fun calculateSimulationIslands() {
        pushProfile("calculateSimulationIslands")
        try {
            simulationIslandManager.updateActivationState(this)

            var i = 0
            val l = constraints.size
            while (i < l) {
                val constraint = constraints[i]

                val colObj0 = constraint.rigidBodyA
                val colObj1 = constraint.rigidBodyB

                if (!colObj0.isStaticOrKinematicObject && !colObj1.isStaticOrKinematicObject) {
                    if (colObj0.isActive || colObj1.isActive) {
                        simulationIslandManager.unionFind
                            .combineIslands((colObj0).islandTag, (colObj1).islandTag)
                    }
                }
                i++
            }

            // Store the island id in each body
            simulationIslandManager.storeIslandActivationState(this)
        } finally {
            popProfile()
        }
    }

    private val tmpSphere = SphereShape(1.0)
    private val sweepResults = ClosestNotMeConvexResultCallback()

    fun integrateTransforms(timeStep: Double) {
        pushProfile("integrateTransforms")
        try {
            val tmp = Stack.newVec()
            val tmpTrans = Stack.newTrans()
            val predictedTrans = Stack.newTrans()
            var stackPos: IntArray? = null
            for (i in 0 until collisionObjects.size) {
                val colObj = collisionObjects[i]
                val body = colObj as? RigidBody
                if (body != null) {
                    body.hitFraction = 1.0
                    if (body.isActive && !body.isStaticOrKinematicObject) {
                        stackPos = Stack.getPosition(stackPos)

                        body.predictIntegratedTransform(timeStep, predictedTrans)

                        tmp.setSub(predictedTrans.origin, body.getWorldTransform(tmpTrans).origin)
                        val squareMotion = tmp.lengthSquared()

                        if (body.ccdSquareMotionThreshold != 0.0 && body.ccdSquareMotionThreshold < squareMotion) {
                            pushProfile("CCD motion clamping")
                            try {
                                if (body.collisionShape!!.isConvex) {
                                    BulletStats.numClampedCcdMotions++

                                    val sweepResults = this.sweepResults
                                    sweepResults.init(
                                        body, body.getWorldTransform(tmpTrans).origin,
                                        predictedTrans.origin, broadphase.overlappingPairCache, dispatcher
                                    )

                                    val tmpSphere = this.tmpSphere
                                    tmpSphere.margin = body.ccdSweptSphereRadius

                                    sweepResults.collisionFilterGroup = body.broadphaseProxy!!.collisionFilterGroup
                                    sweepResults.collisionFilterMask = body.broadphaseProxy!!.collisionFilterMask

                                    convexSweepTest(
                                        tmpSphere,
                                        body.getWorldTransform(tmpTrans),
                                        predictedTrans,
                                        sweepResults
                                    )
                                    // JAVA NOTE: added closestHitFraction test to prevent objects being stuck
                                    if (sweepResults.hasHit() && (sweepResults.closestHitFraction > 0.0001f)) {
                                        body.hitFraction = sweepResults.closestHitFraction
                                        body.predictIntegratedTransform(timeStep * body.hitFraction, predictedTrans)
                                        body.hitFraction = 0.0
                                    }
                                }
                            } finally {
                                popProfile()
                            }
                        }
                        body.proceedToTransform(predictedTrans)
                        Stack.reset(stackPos)
                    }
                }
            }
        } finally {
            Stack.subVec(1)
            Stack.subTrans(2)
            popProfile()
        }
    }

    fun predictUnconstrainedMotion(timeStep: Double) {
        pushProfile("predictUnconstrainedMotion")
        try {
            var stackPos: IntArray? = null
            val tmpTrans = Stack.newTrans()
            for (i in 0 until collisionObjects.size) {
                val colObj = collisionObjects[i]
                val body = colObj as? RigidBody
                if (body != null && !body.isStaticOrKinematicObject && body.isActive) {
                    stackPos = Stack.getPosition(stackPos)
                    body.integrateVelocities(timeStep)
                    // damping
                    body.applyDamping(timeStep)
                    body.predictIntegratedTransform(timeStep, body.getInterpolationWorldTransform(tmpTrans))
                    Stack.reset(stackPos)
                }
            }
        } finally {
            Stack.subTrans(1)
            popProfile()
        }
    }

    fun startProfiling() {
        CProfileManager.reset()
    }

    fun debugDrawObject(worldTransform: Transform) {
        val tmp = Stack.newVec()
        val color = Stack.newVec()

        // Draw a small simplex at the center of the object
        val start = Stack.newVec(worldTransform.origin)

        tmp.set(1.0, 0.0, 0.0)
        worldTransform.basis.transform(tmp)
        tmp.add(start)
        color.set(1.0, 0.0, 0.0)
        debugDrawer!!.drawLine(start, tmp, color)

        tmp.set(0.0, 1.0, 0.0)
        worldTransform.basis.transform(tmp)
        tmp.add(start)
        color.set(0.0, 1.0, 0.0)
        debugDrawer!!.drawLine(start, tmp, color)

        tmp.set(0.0, 0.0, 1.0)
        worldTransform.basis.transform(tmp)
        tmp.add(start)
        color.set(0.0, 0.0, 1.0)
        debugDrawer!!.drawLine(start, tmp, color)

        Stack.subVec(3)
    }

    override val numConstraints: Int
        get() = constraints.size

    override fun getConstraint(index: Int): TypedConstraint? {
        return constraints[index]
    }

    override val numActions: Int
        get() = actions.size

    override fun getAction(index: Int): ActionInterface? {
        return actions[index]
    }

    private class ClosestNotMeConvexResultCallback : ClosestConvexResultCallback() {
        private lateinit var me: CollisionObject
        private lateinit var pairCache: OverlappingPairCache
        private lateinit var dispatcher: Dispatcher

        fun init(
            me: CollisionObject,
            fromA: Vector3d,
            toA: Vector3d,
            pairCache: OverlappingPairCache,
            dispatcher: Dispatcher
        ) {
            super.init(fromA, toA)
            this.me = me
            this.pairCache = pairCache
            this.dispatcher = dispatcher
        }

        override fun addSingleResult(convexResult: LocalConvexResult, normalInWorldSpace: Boolean): Double {
            if (convexResult.hitCollisionObject === me) {
                return 1.0
            }

            val linVelA = Stack.newVec()
            val linVelB = Stack.newVec()
            linVelA.setSub(convexToWorld, convexFromWorld)
            linVelB.set(0.0, 0.0, 0.0) //toB.getOrigin()-fromB.getOrigin();

            val relativeVelocity = Stack.newVec()
            relativeVelocity.setSub(linVelA, linVelB)

            // don't report time of impact for motion away from the contact normal (or causes minor penetration)
            val allowedPenetration = 0.0
            val ignored = convexResult.hitNormalLocal.dot(relativeVelocity) >= -allowedPenetration

            Stack.subVec(3)
            if (ignored) return 1.0

            return super.addSingleResult(convexResult, normalInWorldSpace)
        }

        override fun needsCollision(proxy0: BroadphaseProxy): Boolean {
            // don't collide with itself
            if (proxy0.clientObject === me) {
                return false
            }

            // don't do CCD when the collision filters are not matching
            if (!super.needsCollision(proxy0)) {
                return false
            }

            val otherObj = proxy0.clientObject as CollisionObject

            // call needsResponse, see http://code.google.com/p/bullet/issues/detail?id=179
            if (!dispatcher.needsResponse(me, otherObj)) return true

            // don't do CCD when there are already contact points (touching contact/penetration)
            val manifoldArray = Stack.newList<PersistentManifold>()
            val collisionPair = pairCache.findPair(me.broadphaseHandle!!, proxy0)
            if (collisionPair != null) {
                if (collisionPair.algorithm != null) {
                    //manifoldArray.resize(0);
                    collisionPair.algorithm!!.getAllContactManifolds(manifoldArray)
                    for (j in manifoldArray.indices) {
                        val manifold = manifoldArray[j]
                        if (manifold.numContacts > 0) {
                            // cleanup
                            manifoldArray.clear()
                            Stack.subList(1)
                            return false
                        }
                    }
                    // cleanup
                    manifoldArray.clear()
                }
            }
            Stack.subList(1)

            return true
        }
    }

    companion object {
        private fun getConstraintIslandId(constraint: TypedConstraint): Int {
            val colObj0 = constraint.rigidBodyA
            val colObj1 = constraint.rigidBodyB
            return if (colObj0.islandTag >= 0) colObj0.islandTag else colObj1.islandTag
        }

        /** ///////////////////////////////////////////////////////////////////////// */
        private val sortConstraintOnIslandPredicate =
            Comparator.comparingInt { it: TypedConstraint -> getConstraintIslandId(it) }
    }
}
