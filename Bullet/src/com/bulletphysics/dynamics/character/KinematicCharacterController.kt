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
import cz.advel.stack.Stack
import me.anno.bullet.bodies.CharacterBody
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

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
    private val convexShape: ConvexShape,
    private val settings: CharacterBody
) : ActionInterface {

    // is also in ghostObject, but it needs to be convex, so we store it here
    // to avoid upcast

    val stepHeight get() = settings.stepHeight
    val upAxis get() = settings.upAxis

    val fallSpeed: Double get() = settings.fallSpeed
    val jumpSpeed: Double get() = settings.jumpSpeed

    var verticalVelocity: Double = 0.0
    var verticalOffset: Double = 0.0

    /**
     * Slope angle that is set (used for returning the exact value)
     * */
    var maxSlopeRadians: Double get() = settings.maxSlopeDegrees.toRadians()

    /**
     * Cosine equivalent of maxSlopeRadians
     * */
    val maxSlopeCosine: Double get() = cos(maxSlopeRadians)

    /**
     * 1G acceleration
     * */
    val gravity: Double get() = settings.gravity
    var addedMargin: Double = 0.02 // @todo: remove this and fix the code

    // this is the desired walk direction, set by the user
    val walkDirection = Vector3d()
    val normalizedDirection = Vector3d()

    // some internal variables
    val currentPosition = Vector3d()
    var currentStepOffset: Double = 0.0
    val targetPosition = Vector3d()

    // keep track of the contact manifolds
    val manifolds = ArrayList<PersistentManifold>()

    var touchingContact: Boolean = false
    val touchingNormal = Vector3d()

    var wasOnGround: Boolean = false

    var useGhostObjectSweepTest: Boolean = true
    var useWalkDirection: Boolean = true
    private var velocityTimeInterval: Double = 1.0

    init {
        this.maxSlopeRadians = (50.0 / 180.0) * Math.PI
    }

    // ActionInterface interface
    override fun updateAction(collisionWorld: CollisionWorld, deltaTimeStep: Double) {
        preStep(collisionWorld)
        playerStep(collisionWorld, deltaTimeStep)
    }

    // ActionInterface interface
    override fun debugDraw(debugDrawer: IDebugDraw) {
    }

    /**
     * This should probably be called setPositionIncrementPerSimulatorStep. This
     * is neither a direction nor a velocity, but the amount to increment the
     * position each simulation iteration, regardless of dt.
     *
     * This call will reset any velocity set by [.setVelocityForTimeInterval].
     */
    @Suppress("unused")
    fun setWalkDirection(walkDirection: Vector3d) {
        useWalkDirection = true
        this.walkDirection.set(walkDirection)
        normalizedDirection.set(walkDirection).safeNormalize()
    }

    /**
     * Caller provides a velocity with which the character should move for the
     * given time period. After the time period, velocity is reset to zero.
     * This call will reset any walk direction set by [.setWalkDirection].
     * Negative time intervals will result in no motion.
     */
    @Suppress("unused")
    fun setVelocityForTimeInterval(velocity: Vector3d, timeInterval: Double) {
        useWalkDirection = false
        walkDirection.set(velocity)
        normalizedDirection.set(walkDirection).safeNormalize()
        velocityTimeInterval = timeInterval
    }

    fun reset() {
    }

    fun warp(origin: Vector3d) {
        val newTrans = Stack.newTrans()
        newTrans.setIdentity()
        newTrans.setTranslation(origin)
        ghostObject.setWorldTransform(newTrans)
        Stack.subTrans(1)
    }

    fun preStep(collisionWorld: CollisionWorld) {
        var numPenetrationLoops = 0
        touchingContact = false
        while (recoverFromPenetration(collisionWorld)) {
            numPenetrationLoops++
            touchingContact = true
            if (numPenetrationLoops > 4) {
                //printf("character could not recover from penetration = %d\n", numPenetrationLoops);
                break
            }
        }

        currentPosition.set(ghostObject.worldTransform.origin)
        targetPosition.set(currentPosition)
        //printf("m_targetPosition=%f,%f,%f\n",m_targetPosition[0],m_targetPosition[1],m_targetPosition[2]);
    }

    fun playerStep(collisionWorld: CollisionWorld, dt: Double) {
        //printf("playerStep(): ");
        //printf("  dt = %f", dt);

        // quick check...

        if (!useWalkDirection && velocityTimeInterval <= 0.0) {
            //printf("\n");
            return  // no motion
        }

        wasOnGround = onGround()

        // Update fall velocity.
        verticalVelocity -= gravity * dt
        if (verticalVelocity > 0.0 && verticalVelocity > jumpSpeed) {
            verticalVelocity = jumpSpeed
        }
        if (verticalVelocity < 0.0 && abs(verticalVelocity) > abs(fallSpeed)) {
            verticalVelocity = -abs(fallSpeed)
        }
        verticalOffset = verticalVelocity * dt

        val xform = ghostObject.getWorldTransform(Stack.newTrans())

        //printf("walkDirection(%f,%f,%f)\n",walkDirection[0],walkDirection[1],walkDirection[2]);
        //printf("walkSpeed=%f\n",walkSpeed);
        stepUp(collisionWorld)
        if (useWalkDirection) {
            //System.out.println("playerStep 3");
            stepForwardAndStrafe(collisionWorld, walkDirection)
        } else {
            // still have some time left for moving!
            val dtMoving = min(dt, velocityTimeInterval)
            velocityTimeInterval -= dt

            // how far will we move while we are moving?
            val move = Stack.newVec()
            walkDirection.mul(dtMoving, move)

            // okay, step
            stepForwardAndStrafe(collisionWorld, move)
            Stack.subVec(1) // move
        }
        stepDown(collisionWorld, dt)

        //printf("\n");
        xform.setTranslation(currentPosition)
        ghostObject.setWorldTransform(xform)
        Stack.subTrans(1)
    }

    fun canJump(): Boolean {
        return onGround()
    }

    @Suppress("unused")
    fun jump() {
        if (!canJump()) return

        verticalVelocity = jumpSpeed

        //#if 0
        //currently no jumping.
        //btTransform xform;
        //m_rigidBody->getMotionState()->getWorldTransform (xform);
        //btVector3 up = xform.getBasis()[1];
        //up.normalize ();
        //btScalar magnitude = (btScalar(1.0)/m_rigidBody->getInvMass()) * btScalar(8.0);
        //m_rigidBody->applyCentralImpulse (up * magnitude);
        //#endif
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

    fun recoverFromPenetration(collisionWorld: CollisionWorld): Boolean {
        var penetration = false

        collisionWorld.dispatcher.dispatchAllCollisionPairs(
            ghostObject.overlappingPairCache, collisionWorld.dispatchInfo, collisionWorld.dispatcher
        )

        currentPosition.set(ghostObject.worldTransform.origin)

        var maxPen = 0.0
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
                        if (dist < maxPen) {
                            maxPen = dist
                            touchingNormal.set(pt.normalWorldOnB) //??
                            touchingNormal.mul(directionSign)
                        }

                        currentPosition.fma(directionSign * dist * 0.2, pt.normalWorldOnB)

                        penetration = true
                    } // else printf("touching %f\n", dist);
                }

                //manifold->clearManifold();
            }
        }

        ghostObject.worldTransform
            .setTranslation(currentPosition)

        //printf("m_touchingNormal = %f,%f,%f\n",m_touchingNormal[0],m_touchingNormal[1],m_touchingNormal[2]);

        //System.out.println("recoverFromPenetration "+penetration+" "+touchingNormal);
        return penetration
    }

    private val upAxisValue: Vector3d
        get() = upAxisDirection[upAxis.id]

    fun stepUp(world: CollisionWorld) {
        // phase 1: up
        val start = Stack.newTrans()
        val end = Stack.newTrans()
        upAxisValue.mulAdd(stepHeight + max(verticalOffset, 0.0), currentPosition, targetPosition)

        start.setIdentity()
        end.setIdentity()

        /* FIXME: Handle penetration properly */
        upAxisValue.mulAdd(convexShape.margin + addedMargin, currentPosition, start.origin)
        end.setTranslation(targetPosition)

        // Find only sloped/flat surface hits, avoid wall and ceiling hits...
        val up = Stack.newVec()
        upAxisValue.negate(up)

        val ghostObject = ghostObject
        val callback = KinematicClosestNotMeConvexResultCallback(ghostObject, up, 0.0)
        callback.collisionFilter = ghostObject.broadphaseHandle!!.collisionFilter

        if (useGhostObjectSweepTest) {
            ghostObject.convexSweepTest(
                convexShape,
                start,
                end,
                callback,
                world.dispatchInfo.allowedCcdPenetration
            )
        } else {
            world.convexSweepTest(convexShape, start, end, callback)
        }

        if (callback.hasHit()) {
            // we moved up only a fraction of the step height
            currentStepOffset = stepHeight * callback.closestHitFraction
            currentPosition.lerp(targetPosition, callback.closestHitFraction)
            verticalVelocity = 0.0
            verticalOffset = 0.0
        } else {
            currentStepOffset = stepHeight
            currentPosition.set(targetPosition)
        }
    }

    fun updateTargetPositionBasedOnCollision(
        hitNormal: Vector3d,
        tangentMag: Double = 0.0,
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
            if (false)  //tangentMag != 0.0)
            {
                val parComponent = Stack.newVec()
                parallelDir.mul(tangentMag * movementLength, parComponent)
                //printf("parComponent=%f,%f,%f\n",parComponent[0],parComponent[1],parComponent[2]);
                targetPosition.add(parComponent)
            }

            if (normalMag != 0.0) {
                val perpComponent = Stack.newVec()
                perpendicularDir.mul(normalMag * movementLength, perpComponent)
                //printf("perpComponent=%f,%f,%f\n",perpComponent[0],perpComponent[1],perpComponent[2]);
                targetPosition.add(perpComponent)
            }
        } // else printf("movementLength don't normalize a zero vector\n");
    }

    fun stepForwardAndStrafe(collisionWorld: CollisionWorld, walkMove: Vector3d) {
        // phase 2: forward and strafe
        val start = Stack.newTrans()
        val end = Stack.newTrans()
        currentPosition.add(walkMove, targetPosition)
        start.setIdentity()
        end.setIdentity()

        var fraction = 1.0
        val distance2Vec = Stack.newVec()
        currentPosition.sub(targetPosition, distance2Vec)
        // var distance2 = distance2Vec.lengthSquared()

        /*if (touchingContact) {
			if (normalizedDirection.dot(touchingNormal) > 0.0) {
				updateTargetPositionBasedOnCollision(touchingNormal);
			}
		}*/
        val hitDistanceVec = Stack.newVec()
        val currentDir = Stack.newVec()

        var maxIter = 10
        while (fraction > 0.01f && maxIter-- > 0) {
            start.setTranslation(currentPosition)
            end.setTranslation(targetPosition)

            val ghostObject = ghostObject
            val callback = KinematicClosestNotMeConvexResultCallback(ghostObject, upAxisValue, -1.0)
            callback.collisionFilter = ghostObject.broadphaseHandle!!.collisionFilter

            val margin = convexShape.margin
            convexShape.margin = margin + addedMargin

            if (useGhostObjectSweepTest) {
                ghostObject.convexSweepTest(
                    convexShape, start, end, callback,
                    collisionWorld.dispatchInfo.allowedCcdPenetration
                )
            } else {
                collisionWorld.convexSweepTest(convexShape, start, end, callback)
            }

            convexShape.margin = margin

            fraction -= callback.closestHitFraction

            if (callback.hasHit()) {
                // we moved only a fraction
                callback.hitPointWorld.sub(currentPosition, hitDistanceVec)

                //double hitDistance = hitDistanceVec.length();

                // if the distance is farther than the collision margin, move
                //if (hitDistance > addedMargin) {
                //	//printf("callback.m_closestHitFraction=%f\n",callback.m_closestHitFraction);
                //	currentPosition.interpolate(currentPosition, targetPosition, callback.closestHitFraction);
                //}
                updateTargetPositionBasedOnCollision(callback.hitNormalWorld)

                targetPosition.sub(currentPosition, currentDir)
                val distance2 = currentDir.lengthSquared()
                if (distance2 > BulletGlobals.SIMD_EPSILON) {
                    currentDir.normalize()
                    // see Quake2: "If velocity is against original velocity, stop ead to avoid tiny oscilations in sloping corners."
                    if (currentDir.dot(normalizedDirection) <= 0.0) {
                        break
                    }
                } else {
                    //printf("currentDir: don't normalize a zero vector\n");
                    break
                }
            } else {
                // we moved whole way
                currentPosition.set(targetPosition)
            }

            //if (callback.m_closestHitFraction == 0.f)
            //    break;
        }

        Stack.subTrans(2)
        Stack.subVec(3)
    }

    fun stepDown(collisionWorld: CollisionWorld, dt: Double) {
        val start = Stack.newTrans()
        val end = Stack.newTrans()

        // phase 3: down
        val additionalDownStep = if (wasOnGround /*&& !onGround()*/) stepHeight else 0.0
        val stepDrop = Stack.newVec()
        upAxisValue.mul(currentStepOffset + additionalDownStep, stepDrop)
        val downVelocity = (if (additionalDownStep == 0.0 && verticalVelocity < 0.0) -verticalVelocity else 0.0) * dt
        val gravityDrop = Stack.newVec()
        upAxisValue.mul(downVelocity, gravityDrop)
        targetPosition.sub(stepDrop)
        targetPosition.sub(gravityDrop)

        start.setIdentity()
        end.setIdentity()

        start.setTranslation(currentPosition)
        end.setTranslation(targetPosition)

        val ghostObject = ghostObject
        val callback = KinematicClosestNotMeConvexResultCallback(ghostObject, upAxisValue, maxSlopeCosine)
        callback.collisionFilter = ghostObject.broadphaseHandle!!.collisionFilter

        if (useGhostObjectSweepTest) {
            ghostObject.convexSweepTest(
                convexShape,
                start,
                end,
                callback,
                collisionWorld.dispatchInfo.allowedCcdPenetration
            )
        } else {
            collisionWorld.convexSweepTest(convexShape, start, end, callback)
        }

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
                // need to transform normal into worldspace
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
