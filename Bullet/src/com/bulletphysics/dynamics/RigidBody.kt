package com.bulletphysics.dynamics

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseProxy
import com.bulletphysics.collision.dispatch.ActivationState
import com.bulletphysics.collision.dispatch.CollisionFlags
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint
import com.bulletphysics.linearmath.MatrixUtil.getRotation
import com.bulletphysics.linearmath.MatrixUtil.scale
import com.bulletphysics.linearmath.MatrixUtil.transposeTransform
import com.bulletphysics.linearmath.MiscUtil.GEN_clamped
import com.bulletphysics.linearmath.MotionState
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.TransformUtil.calculateAngularVelocity
import com.bulletphysics.linearmath.TransformUtil.calculateLinearVelocity
import com.bulletphysics.linearmath.TransformUtil.integrateTransform
import com.bulletphysics.util.setCross
import com.bulletphysics.util.setMul
import com.bulletphysics.util.setScaleAdd
import com.bulletphysics.util.setSub
import cz.advel.stack.Stack
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.pow

/**
 * RigidBody is the main class for rigid body objects. It is derived from
 * [CollisionObject], so it keeps reference to [CollisionShape].
 *
 * It is recommended for performance and memory use to share [CollisionShape]
 * objects whenever possible.
 *
 * There are 3 types of rigid bodies:<br></br>
 *
 *  1. Dynamic rigid bodies, with positive mass. Motion is controlled by rigid body dynamics.
 *  1. Fixed objects with zero mass. They are not moving (basically collision objects).
 *  1. Kinematic objects, which are objects without mass, but the user can move them. There
 * is on-way interaction, and Bullet calculates a velocity based on the timestep and
 * previous and current world transform.
 *
 * Bullet automatically deactivates dynamic rigid bodies, when the velocity is below
 * a threshold for a given time.
 *
 * Deactivated (sleeping) rigid bodies don't take any processing time, except a minor
 * broadphase collision detection impact (to allow active objects to activate/wake up
 * sleeping objects).
 *
 * @author jezek2
 */
class RigidBody : CollisionObject {

    val invInertiaTensorWorld = Matrix3d()
    private val linearVelocity = Vector3d()
    private val angularVelocity = Vector3d()

    var inverseMass: Double = 0.0

    var angularFactor: Double = 0.0

    val gravity = Vector3d()
    private val invInertiaLocal = Vector3d()
    private val totalForce = Vector3d()
    private val totalTorque = Vector3d()

    var linearDamping: Double = 0.0
    var angularDamping: Double = 0.0

    private var additionalDamping = false
    private var additionalDampingFactor = 0.0
    private var additionalLinearDampingThresholdSqr = 0.0
    private var additionalAngularDampingThresholdSqr = 0.0
    private var additionalAngularDampingFactor = 0.0

    private var linearSleepingThreshold = 0.0
    private var angularSleepingThreshold = 0.0

    // optionalMotionState allows to automatically synchronize the world transform for active objects
    private var optionalMotionState: MotionState? = null

    val predictedTransform = Transform()

    // keep track of typed constraints referencing this rigid body
    val constraintRefs = ArrayList<TypedConstraint>()

    // for experimental overriding of friction/contact solver func
    var contactSolverType: Int = 0
    var frictionSolverType: Int = 0

    var debugBodyId: Int = 0

    constructor(constructionInfo: RigidBodyConstructionInfo) {
        setupRigidBody(constructionInfo)
    }

    @JvmOverloads
    constructor(
        mass: Double,
        motionState: MotionState?,
        collisionShape: CollisionShape,
        localInertia: Vector3d = Vector3d(0.0, 0.0, 0.0)
    ) {
        setupRigidBody(RigidBodyConstructionInfo(mass, motionState, collisionShape, localInertia))
    }

    private fun setupRigidBody(constructionInfo: RigidBodyConstructionInfo) {
        linearVelocity.set(0.0, 0.0, 0.0)
        angularVelocity.set(0.0, 0.0, 0.0)
        angularFactor = 1.0
        gravity.set(0.0, 0.0, 0.0)
        totalForce.set(0.0, 0.0, 0.0)
        totalTorque.set(0.0, 0.0, 0.0)
        linearDamping = 0.0
        angularDamping = 0.5
        linearSleepingThreshold = constructionInfo.linearSleepingThreshold
        angularSleepingThreshold = constructionInfo.angularSleepingThreshold
        optionalMotionState = constructionInfo.motionState
        contactSolverType = 0
        frictionSolverType = 0
        additionalDamping = constructionInfo.additionalDamping
        additionalDampingFactor = constructionInfo.additionalDampingFactor
        additionalLinearDampingThresholdSqr = constructionInfo.additionalLinearDampingThresholdSqr
        additionalAngularDampingThresholdSqr = constructionInfo.additionalAngularDampingThresholdSqr
        additionalAngularDampingFactor = constructionInfo.additionalAngularDampingFactor

        if (optionalMotionState != null) {
            optionalMotionState!!.getWorldTransform(worldTransform)
        } else {
            worldTransform.set(constructionInfo.startWorldTransform)
        }

        interpolationWorldTransform.set(worldTransform)
        interpolationLinearVelocity.set(0.0, 0.0, 0.0)
        interpolationAngularVelocity.set(0.0, 0.0, 0.0)

        // moved to CollisionObject
        friction = constructionInfo.friction
        restitution = constructionInfo.restitution

        collisionShape = constructionInfo.collisionShape
        debugBodyId = uniqueId++

        setMassProps(constructionInfo.mass, constructionInfo.localInertia)
        setDamping(constructionInfo.linearDamping, constructionInfo.angularDamping)
        updateInertiaTensor()
    }

    fun destroy() {
        // No constraints should point to this rigidbody
        // Remove constraints from the dynamics world before you delete the related rigidbodies.
        assert(constraintRefs.isEmpty())
    }

    fun proceedToTransform(newTrans: Transform) {
        setCenterOfMassTransform(newTrans)
    }

    /**
     * Continuous collision detection needs prediction.
     */
    fun predictIntegratedTransform(timeStep: Double, predictedTransform: Transform) {
        integrateTransform(worldTransform, linearVelocity, angularVelocity, timeStep, predictedTransform)
    }

    fun saveKinematicState(timeStep: Double) {
        //todo: clamp to some (user definable) safe minimum timestep, to limit maximum angular/linear velocities
        if (timeStep == 0.0) return
        //if we use motionState to synchronize world transforms, get the new kinematic/animated world transform
        motionState?.getWorldTransform(worldTransform)

        // linear
        calculateLinearVelocity(interpolationWorldTransform, worldTransform, timeStep, linearVelocity)
        interpolationLinearVelocity.set(linearVelocity)

        // angular
        calculateAngularVelocity(interpolationWorldTransform, worldTransform, timeStep, angularVelocity)
        interpolationAngularVelocity.set(angularVelocity)

        // save world transform as previous transform
        interpolationWorldTransform.set(worldTransform)
    }

    fun applyGravity() {
        if (isStaticOrKinematicObject) return

        applyCentralForce(gravity, 1.0 / inverseMass)
    }

    fun setGravity(acceleration: Vector3d) {
        gravity.set(acceleration)
    }

    fun setDamping(lin_damping: Double, ang_damping: Double) {
        linearDamping = GEN_clamped(lin_damping, 0.0, 1.0)
        angularDamping = GEN_clamped(ang_damping, 0.0, 1.0)
    }

    /**
     * Damps the velocity, using the given linearDamping and angularDamping.
     */
    fun applyDamping(timeStep: Double) {
        // On new damping: see discussion/issue report here: http://code.google.com/p/bullet/issues/detail?id=74
        // todo: do some performance comparisons (but other parts of the engine are probably bottleneck anyway

        //#define USE_OLD_DAMPING_METHOD 1
        //#ifdef USE_OLD_DAMPING_METHOD
        //linearVelocity.mul(MiscUtil.GEN_clamped((1.0 - timeStep * linearDamping), 0.0, 1.0));
        //angularVelocity.mul(MiscUtil.GEN_clamped((1.0 - timeStep * angularDamping), 0.0, 1.0));
        //#else

        linearVelocity.mul((1.0 - linearDamping).pow(timeStep))
        angularVelocity.mul((1.0 - angularDamping).pow(timeStep))

        //#endif
        if (additionalDamping) {
            // Additional damping can help avoiding lowpass jitter motion, help stability for ragdolls etc.
            // Such damping is undesirable, so once the overall simulation quality of the rigid body dynamics system has improved, this should become obsolete
            if ((angularVelocity.lengthSquared() < additionalAngularDampingThresholdSqr) &&
                (linearVelocity.lengthSquared() < additionalLinearDampingThresholdSqr)
            ) {
                angularVelocity.mul(additionalDampingFactor)
                linearVelocity.mul(additionalDampingFactor)
            }

            val speed = linearVelocity.length()
            if (speed < linearDamping) {
                val dampVel = 0.005
                if (speed > dampVel) {
                    val dir = Stack.newVec(linearVelocity)
                    dir.normalize()
                    dir.mul(dampVel)
                    linearVelocity.sub(dir)
                } else {
                    linearVelocity.set(0.0, 0.0, 0.0)
                }
            }

            val angSpeed = angularVelocity.length()
            if (angSpeed < angularDamping) {
                val angDampVel = 0.005
                if (angSpeed > angDampVel) {
                    val dir = Stack.newVec(angularVelocity)
                    dir.normalize()
                    dir.mul(angDampVel)
                    angularVelocity.sub(dir)
                } else {
                    angularVelocity.set(0.0, 0.0, 0.0)
                }
            }
        }
    }

    fun setMassProps(mass: Double, inertia: Vector3d) {
        if (mass == 0.0) {
            collisionFlags = collisionFlags or CollisionFlags.STATIC_OBJECT
            inverseMass = 0.0
        } else {
            collisionFlags = collisionFlags and (CollisionFlags.STATIC_OBJECT.inv())
            inverseMass = 1.0 / mass
        }

        invInertiaLocal.set(
            if (inertia.x != 0.0) 1.0 / inertia.x else 0.0,
            if (inertia.y != 0.0) 1.0 / inertia.y else 0.0,
            if (inertia.z != 0.0) 1.0 / inertia.z else 0.0
        )
    }

    fun integrateVelocities(step: Double) {
        if (isStaticOrKinematicObject) {
            return
        }

        linearVelocity.setScaleAdd(inverseMass * step, totalForce, linearVelocity)
        val tmp = Stack.newVec(totalTorque)
        invInertiaTensorWorld.transform(tmp)
        angularVelocity.setScaleAdd(step, tmp, angularVelocity)
        Stack.subVec(1)

        // clamp angular velocity. collision calculations will fail on higher angular velocities
        val angVel = angularVelocity.length()
        if (angVel * step > MAX_ANGULAR_VELOCITY) {
            angularVelocity.mul((MAX_ANGULAR_VELOCITY / step) / angVel)
        }
    }

    fun setCenterOfMassTransform(xform: Transform) {
        if (isStaticOrKinematicObject) {
            interpolationWorldTransform.set(worldTransform)
        } else {
            interpolationWorldTransform.set(xform)
        }
        interpolationLinearVelocity.set(linearVelocity)
        interpolationAngularVelocity.set(angularVelocity)
        worldTransform.set(xform)
        updateInertiaTensor()
    }

    fun applyCentralForce(force: Vector3d) {
        totalForce.add(force)
    }

    fun applyCentralForce(force: Vector3d, strength: Double) {
        totalForce.x += strength * force.x
        totalForce.y += strength * force.y
        totalForce.z += strength * force.z
    }

    fun getInvInertiaDiagLocal(out: Vector3d): Vector3d {
        out.set(invInertiaLocal)
        return out
    }

    @Suppress("unused")
    fun setInvInertiaDiagLocal(diagInvInertia: Vector3d) {
        invInertiaLocal.set(diagInvInertia)
    }

    @Suppress("unused")
    fun setSleepingThresholds(linear: Double, angular: Double) {
        linearSleepingThreshold = linear
        angularSleepingThreshold = angular
    }

    fun applyTorque(torque: Vector3d) {
        totalTorque.add(torque)
    }

    @Suppress("unused")
    fun applyForce(force: Vector3d, relPos: Vector3d) {
        applyCentralForce(force)

        val tmp = Stack.newVec()
        tmp.setCross(relPos, force)
        tmp.mul(angularFactor)
        applyTorque(tmp)
        Stack.subVec(1)
    }

    fun applyCentralImpulse(impulse: Vector3d) {
        linearVelocity.setScaleAdd(inverseMass, impulse, linearVelocity)
    }

    fun applyTorqueImpulse(torque: Vector3d) {
        val tmp = Stack.borrowVec(torque)
        invInertiaTensorWorld.transform(tmp)
        angularVelocity.add(tmp)
    }

    fun applyImpulse(impulse: Vector3d, relPos: Vector3d) {
        if (inverseMass != 0.0) {
            applyCentralImpulse(impulse)
            if (angularFactor != 0.0) {
                val tmp = Stack.newVec()
                tmp.setCross(relPos, impulse)
                tmp.mul(angularFactor)
                applyTorqueImpulse(tmp)
                Stack.subVec(1)
            }
        }
    }

    /**
     * Optimization for the iterative solver: avoid calculating constant terms involving inertia, normal, relative position.
     */
    fun internalApplyImpulse(linearComponent: Vector3d, angularComponent: Vector3d, impulseMagnitude: Double) {
        if (inverseMass != 0.0) {
            linearVelocity.setScaleAdd(impulseMagnitude, linearComponent, linearVelocity)
            if (angularFactor != 0.0) {
                angularVelocity.setScaleAdd(impulseMagnitude * angularFactor, angularComponent, angularVelocity)
            }
        }
    }

    fun clearForces() {
        totalForce.set(0.0, 0.0, 0.0)
        totalTorque.set(0.0, 0.0, 0.0)
    }

    fun updateInertiaTensor() {
        val mat1 = Stack.newMat()
        scale(mat1, worldTransform.basis, invInertiaLocal)

        val mat2 = Stack.newMat(worldTransform.basis)
        mat2.transpose()

        invInertiaTensorWorld.setMul(mat1, mat2)
        Stack.subMat(2)
    }

    fun getCenterOfMassPosition(out: Vector3d): Vector3d {
        out.set(worldTransform.origin)
        return out
    }

    @Suppress("unused")
    fun getOrientation(out: Quaterniond): Quaterniond {
        getRotation(worldTransform.basis, out)
        return out
    }

    fun getCenterOfMassTransform(out: Transform): Transform {
        out.set(worldTransform)
        return out
    }

    fun getLinearVelocity(out: Vector3d): Vector3d {
        out.set(linearVelocity)
        return out
    }

    fun getAngularVelocity(out: Vector3d): Vector3d {
        out.set(angularVelocity)
        return out
    }

    fun setLinearVelocity(linVel: Vector3d) {
        assert(collisionFlags != CollisionFlags.STATIC_OBJECT)
        linearVelocity.set(linVel)
    }

    fun setAngularVelocity(angVel: Vector3d) {
        assert(collisionFlags != CollisionFlags.STATIC_OBJECT)
        angularVelocity.set(angVel)
    }

    fun getVelocityInLocalPoint(relPos: Vector3d, out: Vector3d): Vector3d {
        // we also calculate lin/ang velocity for kinematic objects
        out.setCross(angularVelocity, relPos)
        out.add(linearVelocity)
        return out

        //for kinematic objects, we could also use use:
        //		return 	(m_worldTransform(rel_pos) - m_interpolationWorldTransform(rel_pos)) / m_kinematicTimeStep;
    }

    @Suppress("unused")
    fun translate(v: Vector3d) {
        worldTransform.origin.add(v)
    }

    fun getAabb(aabbMin: Vector3d, aabbMax: Vector3d) {
        collisionShape!!.getAabb(worldTransform, aabbMin, aabbMax)
    }

    fun computeImpulseDenominator(pos: Vector3d, normal: Vector3d): Double {
        val r0 = Stack.newVec()
        r0.setSub(pos, getCenterOfMassPosition(Stack.newVec()))

        val c0 = Stack.newVec()
        r0.cross(normal, c0)

        invInertiaTensorWorld.transformTranspose(c0)

        val answer = inverseMass + normal.dot(c0.cross(r0))
        Stack.subVec(3)
        return answer
    }

    fun computeAngularImpulseDenominator(axis: Vector3d): Double {
        val vec = Stack.borrowVec()
        invInertiaTensorWorld.transformTranspose(axis, vec)
        return axis.dot(vec)
    }

    fun updateDeactivation(timeStep: Double) {
        if ((activationState == ActivationState.SLEEPING) || (activationState == ActivationState.DISABLE_DEACTIVATION)) {
            return
        }

        if (linearVelocity.lengthSquared() < linearSleepingThreshold * linearSleepingThreshold &&
            angularVelocity.lengthSquared() < angularSleepingThreshold * angularSleepingThreshold
        ) {
            deactivationTime += timeStep
        } else {
            deactivationTime = 0.0
            setActivationStateMaybe(ActivationState.ACTIVE)
        }
    }

    fun wantsSleeping(): Boolean {
        val state = activationState
        if (state == ActivationState.DISABLE_DEACTIVATION) {
            return false
        }

        // disable deactivation
        if (BulletGlobals.isDeactivationDisabled || (BulletGlobals.deactivationTime == 0.0)) {
            return false
        }

        if (state == ActivationState.SLEEPING || state == ActivationState.WANTS_DEACTIVATION) {
            return true
        }

        return deactivationTime > BulletGlobals.deactivationTime
    }

    val broadphaseProxy: BroadphaseProxy?
        get() = broadphaseHandle

    @Suppress("unused")
    fun setNewBroadphaseProxy(broadphaseProxy: BroadphaseProxy?) {
        this.broadphaseHandle = broadphaseProxy
    }

    @set:Suppress("unused")
    var motionState: MotionState?
        get() = optionalMotionState
        set(motionState) {
            this.optionalMotionState = motionState
            if (optionalMotionState != null) {
                motionState!!.getWorldTransform(worldTransform)
            }
        }

    @get:Suppress("unused")
    val isInWorld: Boolean
        /**
         * Is this rigidbody added to a CollisionWorld/DynamicsWorld/Broadphase?
         */
        get() = (this.broadphaseProxy != null)

    override fun checkCollideWithOverride(co: CollisionObject?): Boolean {
        // TODO: change to cast
        val otherRb: RigidBody? = upcast(co)
        if (otherRb == null) {
            return true
        }

        for (i in constraintRefs.indices) {
            val c = constraintRefs[i]
            if (c.rigidBodyA === otherRb || c.rigidBodyB === otherRb) {
                return false
            }
        }

        return true
    }

    fun addConstraintRef(c: TypedConstraint) {
        val index = constraintRefs.indexOf(c)
        if (index == -1) {
            constraintRefs.add(c)
        }

        checkCollideWith = true
    }

    fun removeConstraintRef(c: TypedConstraint) {
        constraintRefs.remove(c)
        checkCollideWith = !constraintRefs.isEmpty()
    }

    companion object {
        private const val MAX_ANGULAR_VELOCITY = BulletGlobals.SIMD_HALF_PI

        private var uniqueId = 0

        /**
         * To keep collision detection and dynamics separate we don't store a rigidbody pointer,
         * but a rigidbody is derived from CollisionObject, so we can safely perform an upcast.
         */
        fun upcast(colObj: CollisionObject?): RigidBody? {
            if (colObj is RigidBody) {
                return colObj
            }
            return null
        }
    }
}
