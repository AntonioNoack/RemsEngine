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
import org.joml.Matrix3f
import org.joml.Vector3d
import org.joml.Vector3f
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
class RigidBody(mass: Float, shape: CollisionShape, localInertia: Vector3f) : CollisionObject(shape) {

    constructor(mass: Float, shape: CollisionShape) : this(mass, shape, Vector3f())

    val invInertiaTensorWorld = Matrix3f()
    val linearVelocity = Vector3f()
    val angularVelocity = Vector3f()

    var inverseMass = 0f

    val angularFactor = Vector3f(1f)

    val gravity = Vector3f()
    val invInertiaLocal = Vector3f()
    private val totalForce = Vector3f()
    private val totalTorque = Vector3f()

    var linearDamping = 0f
        set(value) {
            field = clamp(value)
        }

    var angularDamping = 0.5f
        set(value) {
            field = clamp(value)
        }

    private var additionalDamping = false
    private var additionalDampingFactor = 0.005f
    private var additionalLinearDampingThresholdSqr = 0.01f
    private var additionalAngularDampingThresholdSqr = 0.01f

    var linearSleepingThreshold = 0.8f
    var angularSleepingThreshold = 1.0f

    val predictedTransform = Transform()

    // keep track of typed constraints referencing this rigid body
    val constraintsForIgnoredCollisions = ArrayList<TypedConstraint>()

    init {
        friction = 0.5f
        restitution = 0f
        setMassProps(mass, localInertia)
    }

    fun setInitialTransform(transform: Transform) {
        worldTransform.set(transform)
        interpolationWorldTransform.set(transform)
    }

    fun destroy() {
        // No constraints should point to this rigidbody
        // Remove constraints from the dynamics world before you delete the related rigidbodies.
        assert(constraintsForIgnoredCollisions.isEmpty())
    }

    fun proceedToTransform(newTrans: Transform) {
        setCenterOfMassTransform(newTrans)
    }

    /**
     * Continuous collision detection needs prediction.
     */
    fun predictIntegratedTransform(timeStep: Float, predictedTransform: Transform) {
        integrateTransform(worldTransform, linearVelocity, angularVelocity, timeStep, predictedTransform)
    }

    fun saveKinematicState(timeStep: Float) {
        //todo: clamp to some (user definable) safe minimum timestep, to limit maximum angular/linear velocities
        if (timeStep == 0f) return

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

        applyCentralForce(gravity, 1f / inverseMass)
    }

    fun setGravity(acceleration: Vector3f) {
        gravity.set(acceleration)
    }

    /**
     * Damps the velocity, using the given linearDamping and angularDamping.
     */
    fun applyDamping(timeStep: Float) {
        // On new damping: see discussion/issue report here: http://code.google.com/p/bullet/issues/detail?id=74
        // todo: do some performance comparisons (but other parts of the engine are probably bottleneck anyway

        //#define USE_OLD_DAMPING_METHOD 1
        //#ifdef USE_OLD_DAMPING_METHOD
        //linearVelocity.mul(MiscUtil.GEN_clamped((1.0 - timeStep * linearDamping), 0.0, 1.0));
        //angularVelocity.mul(MiscUtil.GEN_clamped((1.0 - timeStep * angularDamping), 0.0, 1.0));
        //#else

        linearVelocity.mul((1f - linearDamping).pow(timeStep))
        angularVelocity.mul((1f - angularDamping).pow(timeStep))

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
                val dampVel = 0.005f
                if (speed > dampVel) {
                    val dir = Stack.borrowVec3f(linearVelocity)
                    dir.normalize()
                    dir.mul(dampVel)
                    linearVelocity.sub(dir)
                } else {
                    linearVelocity.set(0.0, 0.0, 0.0)
                }
            }

            val angSpeed = angularVelocity.length()
            if (angSpeed < angularDamping) {
                val angDampVel = 0.005f
                if (angSpeed > angDampVel) {
                    val dir = Stack.borrowVec3f(angularVelocity)
                    dir.normalize()
                    dir.mul(angDampVel)
                    angularVelocity.sub(dir)
                } else {
                    angularVelocity.set(0.0, 0.0, 0.0)
                }
            }
        }
    }

    fun setMassProps(mass: Float, inertia: Vector3f) {
        if (mass == 0f) {
            collisionFlags = collisionFlags or CollisionFlags.STATIC_OBJECT
            inverseMass = 0f
        } else {
            collisionFlags = collisionFlags and (CollisionFlags.STATIC_OBJECT.inv())
            inverseMass = 1f / mass
        }

        invInertiaLocal.set(
            if (inertia.x != 0f) 1f / inertia.x else 0f,
            if (inertia.y != 0f) 1f / inertia.y else 0f,
            if (inertia.z != 0f) 1f / inertia.z else 0f
        )

        updateInertiaTensor()
    }

    fun integrateVelocities(timeStep: Float) {
        if (isStaticOrKinematicObject) {
            return
        }

        linearVelocity.fma(inverseMass * timeStep, totalForce)
        val tmp = Stack.newVec3f(totalTorque)
        invInertiaTensorWorld.transform(tmp)
        angularVelocity.fma(timeStep, tmp)
        Stack.subVec3f(1)

        // clamp angular velocity. collision calculations will fail on higher angular velocities
        val angVel = angularVelocity.length()
        if (angVel * timeStep > MAX_ANGULAR_VELOCITY) {
            angularVelocity.mul((MAX_ANGULAR_VELOCITY / timeStep) / angVel)
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

    fun applyCentralForce(force: Vector3f) {
        totalForce.add(force)
    }

    fun applyCentralForce(force: Vector3f, strength: Float) {
        totalForce.x += strength * force.x
        totalForce.y += strength * force.y
        totalForce.z += strength * force.z
    }

    fun setSleepingThresholds(linear: Float, angular: Float) {
        linearSleepingThreshold = linear
        angularSleepingThreshold = angular
    }

    fun applyTorque(torque: Vector3f) {
        totalTorque.add(torque)
    }

    fun applyForce(force: Vector3f, relPos: Vector3f) {
        applyCentralForce(force)

        val tmp = Stack.newVec3f()
        relPos.cross(force, tmp).mul(angularFactor)
        applyTorque(tmp)
        Stack.subVec3f(1)
    }

    fun applyCentralImpulse(impulse: Vector3f) {
        linearVelocity.fma(inverseMass, impulse)
    }

    fun applyTorqueImpulse(torque: Vector3f) {
        val tmp = Stack.borrowVec3f(torque)
        invInertiaTensorWorld.transform(tmp)
        angularVelocity.add(tmp)
    }

    /**
     * impulse and relPos are in world space
     * */
    fun applyImpulse(impulse: Vector3f, relPos: Vector3f) {
        if (inverseMass != 0f) {
            applyCentralImpulse(impulse)
            if (!angularFactor.equals(0f, 0f, 0f)) {
                val tmp = Stack.newVec3f()
                relPos.cross(impulse, tmp).mul(angularFactor)
                applyTorqueImpulse(tmp)
                Stack.subVec3f(1)
            }
        }
    }

    /**
     * Optimization for the iterative solver: avoid calculating constant terms involving inertia, normal, relative position.
     */
    fun internalApplyImpulse(linearComponent: Vector3f, angularComponent: Vector3f, impulseMagnitude: Float) {
        if (inverseMass != 0f) {
            linearVelocity.fma(impulseMagnitude, linearComponent)
            val angularFactor = angularFactor
            angularVelocity.add(
                impulseMagnitude * angularFactor.x * angularComponent.x,
                impulseMagnitude * angularFactor.y * angularComponent.y,
                impulseMagnitude * angularFactor.z * angularComponent.z
            )
        }
    }

    fun clearForces() {
        totalForce.set(0f)
        totalTorque.set(0f)
    }

    fun updateInertiaTensor() {
        val basisPerLocalInertia = Stack.newMat()
            .set(worldTransform.basis)
            .scale(invInertiaLocal)

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

    fun setLinearVelocity(linVel: Vector3f) {
        assert(collisionFlags != CollisionFlags.STATIC_OBJECT)
        linearVelocity.set(linVel)
    }

    fun setAngularVelocity(angVel: Vector3f) {
        assert(collisionFlags != CollisionFlags.STATIC_OBJECT)
        angularVelocity.set(angVel)
    }

    fun getVelocityInLocalPoint(relPos: Vector3f, out: Vector3f): Vector3f {
        // we also calculate lin/ang velocity for kinematic objects
        angularVelocity.cross(relPos, out)
        out.add(linearVelocity)
        return out
    }

    fun computeImpulseDenominator(relPos: Vector3d, normal: Vector3f): Float {
        val delta = Stack.newVec3f()
        relPos.sub(worldTransform.origin, delta)

        val cross = Stack.newVec3f()
        delta.cross(normal, cross)

        invInertiaTensorWorld.transformTranspose(cross)

        val answer = inverseMass + normal.dot(cross.cross(delta))
        Stack.subVec3f(2)
        return answer
    }

    fun computeAngularImpulseDenominator(axis: Vector3f): Float {
        val vec = Stack.borrowVec3f()
        invInertiaTensorWorld.transformTranspose(axis, vec)
        return axis.dot(vec)
    }

    fun updateDeactivation(timeStep: Float) {
        if ((activationState == ActivationState.SLEEPING) || (activationState == ActivationState.ALWAYS_ACTIVE)) {
            return
        }

        if (linearVelocity.lengthSquared() < linearSleepingThreshold * linearSleepingThreshold &&
            angularVelocity.lengthSquared() < angularSleepingThreshold * angularSleepingThreshold
        ) {
            deactivationTime += timeStep
        } else {
            deactivationTime = 0f
            setActivationStateMaybe(ActivationState.ACTIVE)
        }
    }

    fun wantsSleeping(): Boolean {
        val state = activationState
        if (state == ActivationState.ALWAYS_ACTIVE) {
            return false
        }

        // disable deactivation
        if (BulletGlobals.isDeactivationDisabled || (BulletGlobals.deactivationTime == 0f)) {
            return false
        }

        if (state == ActivationState.SLEEPING || state == ActivationState.WANTS_DEACTIVATION) {
            return true
        }

        return deactivationTime > BulletGlobals.deactivationTime
    }

    override fun checkCollideWith(co: CollisionObject): Boolean {
        if (co !is RigidBody) return true
        for (i in constraintsForIgnoredCollisions.indices) {
            val c = constraintsForIgnoredCollisions[i]
            if (c.rigidBodyA === co || c.rigidBodyB === co) {
                return false
            }
        }
        return true
    }

    fun addConstraintRef(c: TypedConstraint) {
        val index = constraintsForIgnoredCollisions.indexOf(c)
        if (index == -1) {
            constraintsForIgnoredCollisions.add(c)
        }
    }

    fun removeConstraintRef(c: TypedConstraint) {
        constraintsForIgnoredCollisions.remove(c)
    }

    companion object {
        private const val MAX_ANGULAR_VELOCITY = BulletGlobals.SIMD_HALF_PI
    }
}
