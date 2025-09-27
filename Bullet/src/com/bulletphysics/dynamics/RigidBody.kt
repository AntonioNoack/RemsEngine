package com.bulletphysics.dynamics

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.dispatch.ActivationState
import com.bulletphysics.collision.dispatch.CollisionFlags
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.TransformUtil.calculateAngularVelocity
import com.bulletphysics.linearmath.TransformUtil.calculateLinearVelocity
import com.bulletphysics.linearmath.TransformUtil.integrateTransform
import cz.advel.stack.Stack
import me.anno.maths.Maths.clamp
import org.joml.Matrix3d
import org.joml.Vector3d
import kotlin.math.pow

/**
 * RigidBody is the main class for rigid body objects. It is derived from
 * [CollisionObject], so it keeps reference to [CollisionShape].
 *
 * It is recommended for performance and memory use to share [CollisionShape] objects whenever possible.
 *
 * There are 3 types of rigid bodies:<br></br>
 *
 *  1. Dynamic rigid bodies, with positive mass. Motion is controlled by rigid body dynamics.
 *  1. Fixed objects with zero mass. They are not moving (basically collision objects).
 *  1. Kinematic objects, which are objects without mass, but the user can move them. There
 * is on-way interaction, and Bullet calculates a velocity based on the timestep and
 * previous and current world transform.
 *
 * Bullet automatically deactivates dynamic rigid bodies, when the velocity is below a threshold for a given time.
 *
 * Deactivated (sleeping) rigid bodies don't take any processing time,
 * except a minor broadphase collision detection impact (to allow active objects to activate/wake up sleeping objects).
 *
 * @author jezek2
 */
class RigidBody(mass: Double, shape: CollisionShape, localInertia: Vector3d) : CollisionObject() {

    constructor(mass: Double, shape: CollisionShape) : this(mass, shape, Vector3d())

    val invInertiaTensorWorld = Matrix3d()
    val linearVelocity = Vector3d()
    val angularVelocity = Vector3d()

    var inverseMass = 0.0

    var angularFactor = 1.0

    val gravity = Vector3d()
    val invInertiaLocal = Vector3d()
    private val totalForce = Vector3d()
    private val totalTorque = Vector3d()

    var linearDamping = 0.0
        set(value) {
            field = clamp(value)
        }

    var angularDamping = 0.5
        set(value) {
            field = clamp(value)
        }

    private var additionalDamping = false
    private var additionalDampingFactor = 0.005
    private var additionalLinearDampingThresholdSqr = 0.01
    private var additionalAngularDampingThresholdSqr = 0.01

    var linearSleepingThreshold = 0.8
    var angularSleepingThreshold = 1.0

    val predictedTransform = Transform()

    // keep track of typed constraints referencing this rigid body
    val constraintRefs = ArrayList<TypedConstraint>()

    init {
        friction = 0.5
        restitution = 0.0
        collisionShape = shape
        setMassProps(mass, localInertia)
    }

    fun setInitialTransform(transform: Transform) {
        worldTransform.set(transform)
        interpolationWorldTransform.set(transform)
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

        updateInertiaTensor()
    }

    fun integrateVelocities(step: Double) {
        if (isStaticOrKinematicObject) {
            return
        }

        linearVelocity.fma(inverseMass * step, totalForce)
        val tmp = Stack.newVec(totalTorque)
        invInertiaTensorWorld.transform(tmp)
        angularVelocity.fma(step, tmp)
        Stack.subVec(1)

        // clamp angular velocity. collision calculations will fail on higher angular velocities
        val angVel = angularVelocity.length()
        if (angVel * step > MAX_ANGULAR_VELOCITY) {
            angularVelocity.mul((MAX_ANGULAR_VELOCITY / step) / angVel)
        }
    }

    fun setCenterOfMassTransform(value: Transform) {
        if (isStaticOrKinematicObject) {
            interpolationWorldTransform.set(worldTransform)
        } else {
            interpolationWorldTransform.set(value)
        }
        interpolationLinearVelocity.set(linearVelocity)
        interpolationAngularVelocity.set(angularVelocity)
        worldTransform.set(value)
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

    fun setSleepingThresholds(linear: Double, angular: Double) {
        linearSleepingThreshold = linear
        angularSleepingThreshold = angular
    }

    fun applyTorque(torque: Vector3d) {
        totalTorque.add(torque)
    }

    fun applyForce(force: Vector3d, relPos: Vector3d) {
        applyCentralForce(force)

        val tmp = Stack.newVec()
        relPos.cross(force, tmp).mul(angularFactor)
        applyTorque(tmp)
        Stack.subVec(1)
    }

    fun applyCentralImpulse(impulse: Vector3d) {
        linearVelocity.fma(inverseMass, impulse)
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
                relPos.cross(impulse, tmp).mul(angularFactor)
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
            linearVelocity.fma(impulseMagnitude, linearComponent)
            if (angularFactor != 0.0) {
                angularVelocity.fma(impulseMagnitude * angularFactor, angularComponent)
            }
        }
    }

    fun clearForces() {
        totalForce.set(0.0, 0.0, 0.0)
        totalTorque.set(0.0, 0.0, 0.0)
    }

    fun updateInertiaTensor() {
        val basisPerLocalInertia = Stack.newMat()
        worldTransform.basis.scale(invInertiaLocal, basisPerLocalInertia)

        val worldBasisTransposed = Stack.newMat(worldTransform.basis)
        worldBasisTransposed.transpose()

        basisPerLocalInertia.mul(worldBasisTransposed, invInertiaTensorWorld)
        Stack.subMat(2)
    }

    fun getCenterOfMassPosition(out: Vector3d): Vector3d {
        out.set(worldTransform.origin)
        return out
    }

    fun getCenterOfMassTransform(out: Transform): Transform {
        out.set(worldTransform)
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
        angularVelocity.cross(relPos, out)
        out.add(linearVelocity)
        return out

        //for kinematic objects, we could also use use:
        //		return 	(m_worldTransform(rel_pos) - m_interpolationWorldTransform(rel_pos)) / m_kinematicTimeStep;
    }

    fun computeImpulseDenominator(pos: Vector3d, normal: Vector3d): Double {
        val r0 = Stack.newVec()
        pos.sub(getCenterOfMassPosition(Stack.newVec()), r0)

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
        if ((activationState == ActivationState.SLEEPING) || (activationState == ActivationState.ALWAYS_ACTIVE)) {
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
        if (state == ActivationState.ALWAYS_ACTIVE) {
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

    override fun checkCollideWithOverride(co: CollisionObject?): Boolean {
        if (co !is RigidBody) return true
        for (i in constraintRefs.indices) {
            val c = constraintRefs[i]
            if (c.rigidBodyA === co || c.rigidBodyB === co) {
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
    }
}
