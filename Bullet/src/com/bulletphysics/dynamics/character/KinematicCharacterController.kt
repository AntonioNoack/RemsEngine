package com.bulletphysics.dynamics.character

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.dispatch.CollisionWorld
import com.bulletphysics.collision.dispatch.CollisionWorld.ClosestConvexResultCallback
import com.bulletphysics.collision.dispatch.CollisionWorld.LocalConvexResult
import com.bulletphysics.collision.dispatch.PairCachingGhostObject
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.dynamics.ActionInterface
import com.bulletphysics.linearmath.IDebugDraw
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.bullet.bodies.CharacterBody
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.maths.Maths.clamp
import me.anno.ui.UIColors
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3d
import kotlin.math.cos
import kotlin.math.max

/**
 * KinematicCharacterController is an object that supports a sliding motion in a world.
 * It uses a [com.bulletphysics.collision.dispatch.GhostObject] and convex sweep test to test for upcoming collisions.
 * This is combined with discrete collision detection to recover from penetrations.
 *
 * Interaction between KinematicCharacterController and dynamic rigid bodies
 * needs to be explicitly implemented by the user.
 *
 * @author tomrbryn
 */
class KinematicCharacterController(
    val ghostObject: PairCachingGhostObject,
    /**
     * ghostObject.shape may be concave,
     * this shape has been converted to be convex
     * */
    private val convexShape: ConvexShape,
    private val settings: CharacterBody
) : ActionInterface {

    val stepHeight get() = settings.stepHeight
    val upAxis get() = settings.upAxis

    val fallSpeed: Double get() = settings.fallSpeed
    val jumpSpeed: Double get() = settings.jumpSpeed

    var verticalVelocity: Double = 0.0
    var verticalOffset: Double = 0.0

    /**
     * Slope angle that is set (used for returning the exact value)
     * todo can we somehow use this??? maybe when there was no step & angle is above this, reset?
     * */
    val maxSlopeRadians: Double get() = settings.maxSlopeDegrees.toRadians()

    /**
     * Cosine equivalent of maxSlopeRadians
     * */
    val maxSlopeCosine: Double get() = cos(maxSlopeRadians)

    /**
     * 1G acceleration
     * */
    val gravity: Double get() = settings.gravity

    // this is the desired walk direction, set by the user
    val targetVelocity = Vector3d()
    val normalizedDirection = Vector3d()

    // some internal variables
    val currentPosition = ghostObject.worldTransform.origin
    var currentStepOffset: Double = 0.0

    private val start = ghostObject.worldTransform
    private val end = Transform()

    val targetPosition = end.origin

    // keep track of the contact manifolds
    val manifolds = ArrayList<PersistentManifold>()

    var touchingContact: Boolean = false
    val touchingNormal = Vector3d()

    var wasOnGround: Boolean = false

    // ActionInterface interface
    override fun updateAction(collisionWorld: CollisionWorld, deltaTimeStep: Double) {
        preStep(collisionWorld)
        playerStep(collisionWorld, deltaTimeStep)
    }

    // ActionInterface interface
    override fun debugDraw(debugDrawer: IDebugDraw) {}

    fun setTargetVelocity(targetVelocity: Vector3d) {
        this.targetVelocity.set(targetVelocity)
        normalizedDirection.set(targetVelocity).safeNormalize()
    }

    @Suppress("unused")
    fun teleportTo(newPosition: Vector3d) {
        ghostObject.worldTransform.setTranslation(newPosition)
    }

    var penetration = 0.0
        private set

    val maxPenetrationIterations = 4

    fun recoverFromPenetrationI(collisionWorld: CollisionWorld) {
        touchingContact = false
        for (i in 0 until maxPenetrationIterations) {
            penetration = recoverFromPenetration(collisionWorld, true)
            if (penetration == 0.0) break
            touchingContact = true
        }
    }

    fun preStep(collisionWorld: CollisionWorld) {
        end.basis.set(start.basis)
        recoverFromPenetrationI(collisionWorld)
        targetPosition.set(currentPosition)
    }

    fun playerStep(collisionWorld: CollisionWorld, dt: Double) {
        wasOnGround = onGround()

        // Update fall velocity.
        verticalVelocity = clamp(verticalVelocity - gravity * dt, -fallSpeed, jumpSpeed)
        verticalOffset = verticalVelocity * dt

        stepUp(collisionWorld)
        stepForwardAndStrafe(collisionWorld, dt)
        stepDown(collisionWorld, dt)

        recoverFromPenetrationI(collisionWorld)
    }

    fun canJump(): Boolean {
        return onGround()
    }

    fun jump() {
        if (!canJump()) return
        verticalVelocity = jumpSpeed
    }

    fun onGround(): Boolean {
        return verticalVelocity == 0.0 && verticalOffset == 0.0
    }

    /**
     * Returns the reflection direction of a ray going 'direction' hitting a surface
     * with normal 'normal'.
     *
     * From: [Stanford-Article](http://www-cs-students.stanford.edu/~adityagp/final/node3.html)
     */
    fun computeReflectionDirection(direction: Vector3d, normal: Vector3d, out: Vector3d): Vector3d {
        normal.mul(-2.0 * direction.dot(normal), out)
        return out.add(direction)
    }

    /**
     * Returns the portion of 'direction' that is parallel to 'normal'
     */
    fun parallelComponent(direction: Vector3d, normal: Vector3d, out: Vector3d): Vector3d {
        val magnitude = direction.dot(normal)
        return normal.mul(magnitude, out)
    }

    /**
     * Returns the portion of 'direction' that is perpendicular to 'normal'
     */
    fun perpendicularComponent(direction: Vector3d, normal: Vector3d, out: Vector3d): Vector3d {
        // perpendicular = direction - parallel
        val perpendicular = parallelComponent(direction, normal, out)
        direction.sub(perpendicular, perpendicular)
        return perpendicular
    }

    /**
     * Returns > 0, if penetrating, 0 else
     * */
    fun recoverFromPenetration(collisionWorld: CollisionWorld, apply: Boolean): Double {
        collisionWorld.dispatcher.dispatchAllCollisionPairs(
            ghostObject.overlappingPairCache,
            collisionWorld.dispatchInfo, collisionWorld.dispatcher
        )

        var maxPenetration = 0.0
        var numAdds = 0
        val direction = Stack.newVec()
        ghostObject.overlappingPairCache.processAllOverlappingPairs { collisionPair ->

            manifolds.clear()
            collisionPair.algorithm?.getAllContactManifolds(manifolds)

            for (j in manifolds.indices) {
                val manifold = manifolds[j]
                val directionSign = if (manifold.body0 === ghostObject) -1.0 else 1.0
                for (p in 0 until manifold.numContacts) {
                    val pt = manifold.getContactPoint(p)

                    val dist = pt.distance
                    if (dist < 0.0) {
                        if (dist < maxPenetration) {
                            maxPenetration = dist
                            touchingNormal.set(pt.normalWorldOnB)
                            touchingNormal.mul(directionSign)
                        }

                        direction.fma(directionSign * dist, pt.normalWorldOnB)
                        numAdds++
                    }
                }
                //manifold->clearManifold();
            }
        }

        if (numAdds > 0 && apply) {
            direction.mul(1.0001 / numAdds)

            // todo when trying to walk up... this resets everything
            if (direction.length() > 1e-6) {
                DebugShapes.showDebugLine(
                    DebugLine(
                        Vector3d(currentPosition),
                        Vector3d(currentPosition).add(direction),
                        UIColors.fireBrick, 3f,
                    )
                )
            }

            currentPosition.add(direction)
        }

        return -maxPenetration
    }

    private val upAxisV: Vector3d
        get() = upAxisDirection[upAxis.id]

    fun stepUp(world: CollisionWorld) {
        // phase 1: up
        upAxisV.mulAdd(stepHeight + max(verticalOffset, 0.0), currentPosition, targetPosition)

        val ghostObject = ghostObject
        val callback = KinematicClosestNotMeConvexResultCallback(ghostObject, upAxisV, 0.0)
        callback.collisionFilter = ghostObject.broadphaseHandle!!.collisionFilter

        ghostObject.convexSweepTest(
            convexShape, start, end,
            callback, world.dispatchInfo.allowedCcdPenetration
        )

        if (callback.hasHit()) {
            // we moved up only a fraction of the step height
            currentStepOffset = stepHeight * callback.closestHitFraction
            currentPosition.lerp(targetPosition, callback.closestHitFraction)
            // set onGround = true
            verticalVelocity = 0.0
            verticalOffset = 0.0
        } else {
            currentStepOffset = stepHeight
            currentPosition.set(targetPosition)
        }
    }

    fun updateTargetPositionBasedOnCollision(
        hitNormal: Vector3d,
        tangentMag: Double = 0.5,
        normalMag: Double = 1.0
    ) {
        val movementDirection = Stack.newVec()
        targetPosition.sub(currentPosition, movementDirection)
        val movementLength = movementDirection.length()
        if (movementLength > BulletGlobals.SIMD_EPSILON) {
            movementDirection.normalize()

            val reflectDir = computeReflectionDirection(movementDirection, hitNormal, Stack.newVec())
            reflectDir.normalize()

            val parallelDir = parallelComponent(reflectDir, hitNormal, Stack.newVec())
            val perpendicularDir = perpendicularComponent(reflectDir, hitNormal, Stack.newVec())

            targetPosition.set(currentPosition)
            targetPosition.fma(tangentMag * movementLength, parallelDir)
            targetPosition.fma(normalMag * movementLength, perpendicularDir)
            Stack.subVec(3)
        }
        Stack.subVec(1)
    }

    fun stepForwardAndStrafe(collisionWorld: CollisionWorld, dt: Double) {
        // phase 2: forward and strafe
        currentPosition.fma(dt, targetVelocity, targetPosition)

        var fraction = 1.0
        val ghostObject = ghostObject
        val hitDistanceVec = Stack.newVec()
        val currentDir = Stack.newVec()

        var maxIter = 10
        while (fraction > 0.01 && maxIter-- > 0) {

            val callback = KinematicClosestNotMeConvexResultCallback(ghostObject, upAxisV, -1.0)
            callback.collisionFilter = ghostObject.broadphaseHandle!!.collisionFilter

            ghostObject.convexSweepTest(
                convexShape, start, end,
                callback, collisionWorld.dispatchInfo.allowedCcdPenetration
            )

            fraction -= callback.closestHitFraction

            if (callback.hasHit()) {
                // we moved only a fraction
                callback.hitPointWorld.sub(currentPosition, hitDistanceVec)

                updateTargetPositionBasedOnCollision(callback.hitNormalWorld)
                targetPosition.sub(currentPosition, currentDir)
                val distance2 = currentDir.lengthSquared()
                if (distance2 > BulletGlobals.SIMD_EPSILON) {
                    currentDir.normalize()
                    // see Quake2: "If velocity is against original velocity, stop to avoid tiny oscillations in sloping corners."
                    if (currentDir.dot(normalizedDirection) <= 0.0) {
                        break
                    }
                } else break
            } else {
                // we moved whole way
                currentPosition.set(targetPosition)
            }
        }

        Stack.subVec(2)
    }

    fun stepDown(collisionWorld: CollisionWorld, dt: Double) {
        // phase 3: down
        val additionalDownStep = if (wasOnGround && !onGround()) stepHeight else 0.0
        val stepDrop = currentStepOffset + additionalDownStep
        val gravityDrop = (if (additionalDownStep == 0.0 && verticalVelocity < 0.0) -verticalVelocity else 0.0) * dt
        targetPosition.fma(-(stepDrop + gravityDrop), upAxisV)

        // if we use maxSlopeCosine here, some objects are ignored, which prevents us from taking steps upwards
        val callback = KinematicClosestNotMeConvexResultCallback(ghostObject, upAxisV, -1.0)
        callback.collisionFilter = ghostObject.broadphaseHandle!!.collisionFilter

        ghostObject.convexSweepTest(
            convexShape, start, end,
            callback, collisionWorld.dispatchInfo.allowedCcdPenetration
        )

        if (callback.hasHit()) {
            // we dropped a fraction of the height -> hit floor
            currentPosition.lerp(targetPosition, callback.closestHitFraction)
            verticalVelocity = 0.0
            verticalOffset = 0.0
        } else {
            // we dropped the full height
            currentPosition.set(targetPosition)
        }

    }

    /** ///////////////////////////////////////////////////////////////////////// */
    private class KinematicClosestNotMeConvexResultCallback(
        me: CollisionObject?, up: Vector3d, minSlopeDot: Double
    ) : ClosestConvexResultCallback() {

        var me: CollisionObject?
        val up: Vector3d
        var minSlopeDot: Double

        init {
            init(Vector3d(), Vector3d())
            this.me = me
            this.up = up
            this.minSlopeDot = minSlopeDot
        }

        override fun addSingleResult(convexResult: LocalConvexResult, normalInWorldSpace: Boolean): Double {
            if (convexResult.hitCollisionObject === me) return 1.0
            if (normalInWorldSpace) {
                val hitNormalWorld = convexResult.hitNormalLocal
                val dotUp = up.dot(hitNormalWorld)
                if (dotUp < minSlopeDot) return 1.0
            } else {
                // need to transform normal into world space
                val hitNormalWorld = Stack.newVec()

                convexResult.hitCollisionObject!!
                    .worldTransform.basis
                    .transform(convexResult.hitNormalLocal, hitNormalWorld)

                val dotUp = up.dot(hitNormalWorld)
                if (dotUp < minSlopeDot) return 1.0

                Stack.subVec(1)
            }

            return super.addSingleResult(convexResult, normalInWorldSpace)
        }
    }

    companion object {
        private val upAxisDirection = arrayOf(
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 0.0),
            Vector3d(0.0, 0.0, 1.0),
        )
    }
}
