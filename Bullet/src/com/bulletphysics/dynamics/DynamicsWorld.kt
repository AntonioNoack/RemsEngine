package com.bulletphysics.dynamics

import com.bulletphysics.collision.broadphase.BroadphaseInterface
import com.bulletphysics.collision.broadphase.Dispatcher
import com.bulletphysics.collision.dispatch.CollisionWorld
import com.bulletphysics.dynamics.constraintsolver.BrokenConstraintCallback
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver
import com.bulletphysics.dynamics.constraintsolver.ContactSolverInfo
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint
import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import org.joml.Vector3f

/**
 * DynamicsWorld is the interface class for several dynamics implementation,
 * basic, discrete, parallel, and continuous etc.
 *
 * @author jezek2
 */
abstract class DynamicsWorld(
    dispatcher: Dispatcher,
    broadphasePairCache: BroadphaseInterface
) : CollisionWorld(dispatcher, broadphasePairCache) {

    var brokenConstraintCallback: BrokenConstraintCallback? = null
    var internalTickCallback: InternalTickCallback? = null

    val solverInfo: ContactSolverInfo = ContactSolverInfo()

    fun stepSimulation(timeStep: Float, maxSubSteps: Int = 1): Int {
        return stepSimulation(timeStep, maxSubSteps, 1f / 60f)
    }

    /**
     * Proceeds the simulation over 'timeStep', units in preferably in seconds.
     *
     * By default, Bullet will subdivide the timestep in constant substeps of each
     * 'fixedTimeStep'.
     *
     * In order to keep the simulation real-time, the maximum number of substeps can
     * be clamped to 'maxSubSteps'.
     *
     * You can disable subdividing the timestep/substepping by passing maxSubSteps=0
     * as second argument to stepSimulation, but in that case you have to keep the
     * timeStep constant.
     */
    abstract fun stepSimulation(timeStep: Float, maxSubSteps: Int, fixedTimeStep: Float): Int

    abstract fun addConstraint(constraint: TypedConstraint, disableCollisionsBetweenLinkedBodies: Boolean = false)
    abstract fun removeConstraint(constraint: TypedConstraint)

    @Suppress("unused")
    abstract fun addAction(action: ActionInterface)

    @Suppress("unused")
    abstract fun removeAction(action: ActionInterface)

    abstract fun addVehicle(vehicle: RaycastVehicle)
    abstract fun removeVehicle(vehicle: RaycastVehicle)

    /**
     * Once a rigidbody is added to the dynamics world, it will get this gravity assigned.
     * Existing rigidbodies in the world get gravity assigned too, during this method.
     */
    abstract fun setGravity(gravity: Vector3f)
    abstract fun getGravity(out: Vector3f): Vector3f

    abstract fun addRigidBody(body: RigidBody)
    abstract fun removeRigidBody(body: RigidBody)

    abstract var constraintSolver: ConstraintSolver

    abstract fun clearForces()
}
