package com.bulletphysics.dynamics

import com.bulletphysics.collision.broadphase.BroadphaseInterface
import com.bulletphysics.collision.broadphase.Dispatcher
import com.bulletphysics.collision.dispatch.CollisionConfiguration
import com.bulletphysics.collision.dispatch.CollisionWorld
import com.bulletphysics.dynamics.constraintsolver.BrokenConstraintCallback
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver
import com.bulletphysics.dynamics.constraintsolver.ContactSolverInfo
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint
import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import org.joml.Vector3d

/**
 * DynamicsWorld is the interface class for several dynamics implementation,
 * basic, discrete, parallel, and continuous etc.
 *
 * @author jezek2
 */
abstract class DynamicsWorld(
    dispatcher: Dispatcher,
    broadphasePairCache: BroadphaseInterface,
    collisionConfiguration: CollisionConfiguration
) : CollisionWorld(dispatcher, broadphasePairCache) {
    var brokenConstraintCallback: BrokenConstraintCallback? = null
    var internalTickCallback: InternalTickCallback? = null
    var worldUserInfo: Any? = null

    val solverInfo: ContactSolverInfo = ContactSolverInfo()

    fun stepSimulation(timeStep: Double): Int {
        return stepSimulation(timeStep, 1, 1.0 / 60f)
    }

    fun stepSimulation(timeStep: Double, maxSubSteps: Int): Int {
        return stepSimulation(timeStep, maxSubSteps, 1.0 / 60f)
    }

    /**
     * Proceeds the simulation over 'timeStep', units in preferably in seconds.
     *
     *
     *
     *
     * By default, Bullet will subdivide the timestep in constant substeps of each
     * 'fixedTimeStep'.
     *
     *
     *
     *
     * In order to keep the simulation real-time, the maximum number of substeps can
     * be clamped to 'maxSubSteps'.
     *
     *
     *
     *
     * You can disable subdividing the timestep/substepping by passing maxSubSteps=0
     * as second argument to stepSimulation, but in that case you have to keep the
     * timeStep constant.
     */
    abstract fun stepSimulation(timeStep: Double, maxSubSteps: Int, fixedTimeStep: Double): Int

    @Suppress("unused")
    abstract fun debugDrawWorld()

    fun addConstraint(constraint: TypedConstraint) {
        addConstraint(constraint, false)
    }

    open fun addConstraint(constraint: TypedConstraint, disableCollisionsBetweenLinkedBodies: Boolean) {
    }

    @Suppress("unused")
    open fun removeConstraint(constraint: TypedConstraint) {
    }

    @Suppress("unused")
    open fun addAction(action: ActionInterface) {
    }

    @Suppress("unused")
    open fun removeAction(action: ActionInterface) {
    }

    open fun addVehicle(vehicle: RaycastVehicle) {
    }

    @Suppress("unused")
    open fun removeVehicle(vehicle: RaycastVehicle) {
    }

    /**
     * Once a rigidbody is added to the dynamics world, it will get this gravity assigned.
     * Existing rigidbodies in the world get gravity assigned too, during this method.
     */
    abstract fun setGravity(gravity: Vector3d)

    abstract fun getGravity(out: Vector3d): Vector3d

    abstract fun addRigidBody(body: RigidBody)

    abstract fun removeRigidBody(body: RigidBody)

    abstract var constraintSolver: ConstraintSolver

    open val numConstraints: Int
        get() = 0

    @Suppress("unused")
    open fun getConstraint(index: Int): TypedConstraint? {
        return null
    }

    @get:Suppress("unused")
    open val numActions: Int
        get() = 0

    @Suppress("unused")
    open fun getAction(index: Int): ActionInterface? {
        return null
    }

    abstract fun clearForces()
}
