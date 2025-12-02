package com.bulletphysics.dynamics

import com.bulletphysics.BulletGlobals
import com.bulletphysics.BulletStats
import com.bulletphysics.BulletStats.profile
import com.bulletphysics.collision.broadphase.BroadphaseInterface
import com.bulletphysics.collision.broadphase.BroadphaseProxy
import com.bulletphysics.collision.broadphase.Dispatcher
import com.bulletphysics.collision.broadphase.OverlappingPairCache
import com.bulletphysics.collision.dispatch.ActivationState
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.dispatch.SimulationIslandManager
import com.bulletphysics.collision.dispatch.SimulationIslandManager.IslandCallback
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver
import com.bulletphysics.dynamics.constraintsolver.ContactSolverInfo
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint
import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import com.bulletphysics.linearmath.BulletProfiling
import com.bulletphysics.linearmath.DebugDrawModes
import com.bulletphysics.linearmath.IDebugDraw
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.TransformUtil
import cz.advel.stack.Stack
import me.anno.ecs.components.collider.CollisionFilters
import me.anno.ecs.components.collider.CollisionFilters.createFilter
import me.anno.graph.octtree.KdTreePairs.FLAG_SWAPPED_PAIRS
import me.anno.graph.octtree.KdTreePairs.queryPairs
import me.anno.utils.hpc.threadLocal
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector3d
import org.joml.Vector3f
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
    override var constraintSolver: ConstraintSolver
) : DynamicsWorld(dispatcher, pairCache) {

    val simulationIslandManager = SimulationIslandManager()
    val constraints = ArrayList<TypedConstraint>()
    val gravity = Vector3f(0f, -10f, 0f)

    //for variable timesteps
    var localTime: Float = 1f / 60f

    val vehicles = ArrayList<RaycastVehicle>()
    val actions = ArrayList<ActionInterface>()

    fun saveKinematicState(timeStep: Float) {
        for (i in collisionObjects.indices) {
            val colObj = collisionObjects[i]
            val body = colObj as? RigidBody ?: continue
            if (body.activationState != ActivationState.SLEEPING && body.isKinematicObject) {
                // to calculate velocities next frame
                body.saveKinematicState(timeStep)
            }
        }
    }

    override fun clearForces() {
        // todo: iterate over awake simulation islands!
        for (i in collisionObjects.indices) {
            val body = collisionObjects[i] as? RigidBody ?: continue
            body.clearForces()
        }
    }

    /**
     * Apply gravity, call this once per timestep.
     */
    fun applyGravity() {
        // todo: iterate over awake simulation islands!
        for (i in collisionObjects.indices) {
            val body = collisionObjects[i] as? RigidBody ?: continue
            if (body.isActive) body.applyGravity()
        }
    }

    fun synchronizeMotionStates() {

        var stackPos: IntArray? = null
        /*val interpolatedTransform = Stack.newTrans()
        // todo: iterate over awake simulation islands!
        for (i in collisionObjects.indices) {
            val colObj = collisionObjects[i]

            val body = colObj as? RigidBody
            if (body != null && body.motionState != null && !body.isStaticOrKinematicObject) {
                // we need to call the update at least once, even for sleeping objects
                // otherwise the 'graphics' transform never updates properly
                // so todo: add 'dirty' flag
                stackPos = Stack.getPosition(stackPos)
                TransformUtil.integrateTransform(
                    body.interpolationWorldTransform,
                    body.interpolationLinearVelocity,
                    body.interpolationAngularVelocity,
                    localTime * body.hitFraction, interpolatedTransform
                )
                body.motionState!!.setWorldTransform(interpolatedTransform)
                Stack.reset(stackPos)
            }
        }
        Stack.subTrans(1)*/

        if (debugDrawer != null && (debugDrawer!!.debugMode and DebugDrawModes.DRAW_WIREFRAME) != 0) {
            for (i in vehicles.indices) {
                val vehicle = vehicles[i]
                for (v in 0 until vehicle.numWheels) {
                    stackPos = Stack.getPosition(stackPos)
                    // synchronize the wheels with the chassis worldtransform
                    vehicle.updateWheelTransform(v)
                    Stack.reset(stackPos)
                }
            }
        }
    }

    override fun stepSimulation(timeStep: Float, maxSubSteps: Int, fixedTimeStep: Float): Int {
        var maxSubSteps = maxSubSteps
        var fixedTimeStep = fixedTimeStep
        // startProfiling() // resets the profiling counters

        val t0 = System.nanoTime()

        var numSimulationSubSteps = 0
        profile("stepSimulation") {
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
            val debugDrawer = debugDrawer
            if (debugDrawer != null && debugDrawer.debugMode.hasFlag(DebugDrawModes.NO_DEACTIVATION)) {
                BulletGlobals.isDeactivationDisabled = true
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
            BulletProfiling.incrementFrameCounter()

            //#endif //BT_NO_PROFILE
        }
        return numSimulationSubSteps
    }

    fun internalSingleStepSimulation(timeStep: Float) {
        profile("internalSingleStepSimulation") {
            // apply gravity, predict motion
            predictUnconstrainedMotion(timeStep)

            val dispatchInfo = dispatchInfo

            dispatchInfo.timeStep = timeStep
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

    override fun setGravity(gravity: Vector3f) {
        this.gravity.set(gravity)
        for (i in collisionObjects.indices) {
            val body = collisionObjects[i] as? RigidBody
            body?.setGravity(gravity)
        }
    }

    override fun getGravity(out: Vector3f): Vector3f {
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

        val isDynamic = !body.isStaticOrKinematicObject
        val collisionFilterGroup = when {
            isDynamic -> CollisionFilters.DEFAULT_GROUP_ID
            body.isStaticObject -> CollisionFilters.STATIC_GROUP_ID
            else -> CollisionFilters.KINEMATIC_GROUP_ID
        }
        val collisionFilterMask =
            if (isDynamic) CollisionFilters.ALL_MASK // dynamic reacts to all
            else CollisionFilters.ANY_DYNAMIC_MASK // static/kinematic doesn't react with itself

        val filter = createFilter(collisionFilterGroup, collisionFilterMask)
        addCollisionObject(body, filter)
    }

    @Suppress("unused")
    fun addRigidBody(body: RigidBody, filter: Int) {
        if (!body.isStaticOrKinematicObject) {
            body.setGravity(gravity)
        }

        addCollisionObject(body, filter)
    }

    fun updateActions(timeStep: Float) {
        profile("updateActions") {
            for (i in actions.indices) {
                actions[i].updateAction(this, timeStep)
            }
        }
    }

    fun updateVehicles(timeStep: Float) {
        profile("updateVehicles") {
            for (i in vehicles.indices) {
                vehicles[i].updateVehicle(timeStep)
            }
        }
    }

    fun updateActivationState(timeStep: Float) {
        profile("updateActivationState") {
            var stackPos: IntArray? = null
            for (i in collisionObjects.indices) {
                stackPos = Stack.getPosition(stackPos)
                val colObj = collisionObjects[i]
                val body = colObj as? RigidBody
                if (body != null) {
                    body.updateDeactivation(timeStep)

                    if (body.wantsSleeping()) {
                        if (body.isStaticOrKinematicObject) {
                            body.setActivationStateMaybe(ActivationState.SLEEPING)
                        } else {
                            if (body.activationState == ActivationState.ACTIVE) {
                                body.setActivationStateMaybe(ActivationState.WANTS_DEACTIVATION)
                            }
                            if (body.activationState == ActivationState.SLEEPING) {
                                body.angularVelocity.set(0f)
                                body.linearVelocity.set(0f)
                            }
                        }
                    } else {
                        if (body.activationState != ActivationState.ALWAYS_ACTIVE) {
                            body.setActivationStateMaybe(ActivationState.ACTIVE)
                        }
                    }
                }
                Stack.reset(stackPos)
            }
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
        var sortedConstraints: List<TypedConstraint> = emptyList()
        var numConstraints: Int = 0
        var debugDrawer: IDebugDraw? = null

        //StackAlloc* m_stackAlloc;
        var dispatcher: Dispatcher? = null

        fun init(
            solverInfo: ContactSolverInfo,
            solver: ConstraintSolver,
            sortedConstraints: List<TypedConstraint>,
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
                var currConstraintStart = -1
                var currNumConstraints = 0

                val sortedConstraints = sortedConstraints
                // find the first constraint for this island
                for (i in 0 until numConstraints) {
                    if (getConstraintIslandId(sortedConstraints[i]) == islandId) {
                        currConstraintStart = i
                        break
                    }
                }
                // count the number of constraints in this island
                for (i in 0 until numConstraints) {
                    if (getConstraintIslandId(sortedConstraints[i]) == islandId) {
                        currNumConstraints++
                    }
                }

                // only call solveGroup if there is some work: avoid virtual function call, its overhead can be excessive
                if (numManifolds + currNumConstraints > 0) {
                    solver!!.solveGroup(
                        bodies, numBodies, manifolds, manifoldsOffset, numManifolds, sortedConstraints,
                        currConstraintStart, currNumConstraints, solverInfo!!, debugDrawer, dispatcher!!
                    )
                }
            }
        }
    }

    private val sortedConstraints = ArrayList<TypedConstraint>()
    private val solverCallback = InplaceSolverIslandCallback()

    fun solveConstraints(solverInfo: ContactSolverInfo) {
        profile("solveConstraints") {
            // sorted version of all btTypedConstraint, based on islandId
            sortedConstraints.clear()
            sortedConstraints.addAll(constraints)
            sortedConstraints.sortWith(sortConstraintOnIslandPredicate)

            solverCallback.init(
                solverInfo, constraintSolver,
                sortedConstraints, sortedConstraints.size,
                debugDrawer, dispatcher
            )

            // solve all the constraints for this island
            simulationIslandManager.buildAndProcessIslands(dispatcher, collisionObjects, solverCallback)
        }
    }

    fun calculateSimulationIslands() {
        profile("calculateSimulationIslands") {
            simulationIslandManager.updateActivationState(this)

            for (i in constraints.indices) {
                val constraint = constraints[i]

                val colObj0 = constraint.rigidBodyA
                val colObj1 = constraint.rigidBodyB

                if (!colObj0.isStaticOrKinematicObject && !colObj1.isStaticOrKinematicObject) {
                    if (colObj0.isActive || colObj1.isActive) {
                        simulationIslandManager.unionFind
                            .combineIslands(colObj0.islandTag, colObj1.islandTag)
                    }
                }
            }

            // Store the island id in each body
            simulationIslandManager.storeIslandActivationState(this)
        }
    }

    private val sweepResults = ClosestNotMeConvexResultCallback()
    private val collisionTree = CollisionTree()

    fun integrateTransforms(timeStep: Float) {
        integrateTransformsBegin(timeStep)
        integrateTransformsCCD(timeStep)
        integrateTransformsEnd()
    }

    fun integrateTransformsBegin(timeStep: Float) {
        val linVel = Stack.newVec3f()
        val tmpSphere = tmpSphere.get()
        collisionTree.clear() // clear it here, so we later still have access to it

        for (i in collisionObjects.indices) {
            val self0 = collisionObjects[i]

            val self = self0 as? RigidBody ?: continue
            if (!self.isActive || self.isStaticOrKinematicObject) continue

            self.hitFraction = 1f
            val transform1 = self.predictedTransform
            self.predictIntegratedTransform(timeStep, transform1)

            val squareMotion = transform1.origin.distanceSquared(self.worldTransform.origin)
            if (self.ccdSquareMotionThreshold != 0f &&
                self.ccdSquareMotionThreshold < squareMotion &&
                self.collisionShape is ConvexShape
            ) {

                BulletStats.numClampedCcdMotions++
                tmpSphere.radius = self.ccdSweptSphereRadius
                val transform0 = self.worldTransform
                val angVel = TransformUtil.calculateVelocity(transform0, transform1, 1f, linVel)
                tmpSphere.calculateTemporalAabb(
                    transform0, linVel, angVel, 1f,
                    self.collisionAabbMin, self.collisionAabbMax
                )

                collisionTree.add(self)
            }
        }
        Stack.subVec3f(1)
    }

    fun integrateTransformsCCD(timeStep: Float) {
        profile("integrateTransforms") {
            collisionTree.queryPairs(FLAG_SWAPPED_PAIRS) { self, other ->

                // does this need swapped pairs? yes, looks like that
                // does this need self-pairs? no

                BulletStats.numClampedCcdMotions++

                val predictedTrans = self.predictedTransform
                val results = sweepResults
                results.init(
                    self, self.worldTransform.origin,
                    predictedTrans.origin, broadphase.overlappingPairCache, dispatcher
                )

                val tmpSphere = tmpSphere.get()
                tmpSphere.radius = self.ccdSweptSphereRadius

                results.collisionFilter = self.broadphaseHandle!!.collisionFilter

                convexSweepTest(
                    tmpSphere,
                    self.worldTransform,
                    predictedTrans,
                    results, dispatchInfo.allowedCcdPenetration,
                    listOf(other)
                )

                // JAVA NOTE: added closestHitFraction test to prevent objects being stuck
                if (results.hasHit() && results.closestHitFraction > 0.0001f) {
                    self.hitFraction = results.closestHitFraction
                    self.predictIntegratedTransform(timeStep * self.hitFraction, predictedTrans)
                    self.hitFraction = 0f
                }

                false
            }
        }
    }

    fun integrateTransformsEnd() {
        profile("integrateTransforms") {
            for (i in collisionObjects.indices) {
                val self = collisionObjects[i] as? RigidBody ?: continue
                if (!self.isActive || self.isStaticOrKinematicObject) continue
                self.proceedToTransform(self.predictedTransform)
            }
        }
    }

    fun predictUnconstrainedMotion(timeStep: Float) {
        profile("predictUnconstrainedMotion") {
            for (i in collisionObjects.indices) {
                val body = collisionObjects[i] as? RigidBody ?: continue
                if (!body.isStaticOrKinematicObject && body.isActive) {
                    body.integrateVelocities(timeStep)
                    // damping
                    body.applyDamping(timeStep)
                    body.predictIntegratedTransform(timeStep, body.interpolationWorldTransform)
                }
            }
        }
    }

    override fun convexSweepTest(
        selfShape: ConvexShape,
        convexFromWorld: Transform,
        convexToWorld: Transform,
        resultCallback: ConvexResultCallback
    ) {
        convexSweepTest(
            selfShape, convexFromWorld, convexToWorld,
            resultCallback, dispatchInfo.allowedCcdPenetration,
            collisionTree
        )
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

        override fun addSingleResult(convexResult: LocalConvexResult, normalInWorldSpace: Boolean): Float {
            if (convexResult.hitCollisionObject === me) return 1f

            val linVelA = Stack.newVec3d()
            val linVelB = Stack.newVec3d()
            convexToWorld.sub(convexFromWorld, linVelA)
            linVelB.set(0.0, 0.0, 0.0) //toB.getOrigin()-fromB.getOrigin();

            val relativeVelocity = Stack.newVec3d()
            linVelA.sub(linVelB, relativeVelocity)

            // don't report time of impact for motion away from the contact normal (or causes minor penetration)
            val allowedPenetration = 0.0
            val ignored = convexResult.hitNormalLocal.dot(relativeVelocity) >= -allowedPenetration

            Stack.subVec3d(3)
            if (ignored) return 1f

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

            val otherObj = proxy0.clientObject

            // call needsResponse, see http://code.google.com/p/bullet/issues/detail?id=179
            if (!dispatcher.needsResponse(me, otherObj)) return true

            // don't do CCD when there are already contact points (touching contact/penetration)
            val manifoldArray = Stack.newList<PersistentManifold>()
            val collisionPair = pairCache.findPair(me.broadphaseHandle!!, proxy0)
            val algorithm = collisionPair?.algorithm
            if (algorithm != null) {
                algorithm.getAllContactManifolds(manifoldArray)
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
            Stack.subList(1)

            return true
        }
    }

    companion object {

        val tmpSphere = threadLocal { SphereShape(1f) }

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
