package com.bulletphysics.dynamics.character

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.dispatch.CollisionWorld
import com.bulletphysics.collision.dispatch.CollisionWorld.ClosestConvexResultCallback
import com.bulletphysics.collision.dispatch.CollisionWorld.ClosestRayResultCallback
import com.bulletphysics.collision.dispatch.CollisionWorld.LocalConvexResult
import com.bulletphysics.collision.dispatch.CollisionWorld.LocalRayResult
import com.bulletphysics.collision.dispatch.PairCachingGhostObject
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.dynamics.ActionInterface
import com.bulletphysics.linearmath.IDebugDraw
import com.bulletphysics.util.revTransform
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setInterpolate
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setScaleAdd
import com.bulletphysics.util.setSub
import cz.advel.stack.Stack
import me.anno.ecs.components.collider.Axis
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
    private val ghostObject: PairCachingGhostObject,
    private val convexShape: ConvexShape,
    var stepHeight: Double,
    var upAxis: Axis
) : ActionInterface {

    // is also in ghostObject, but it needs to be convex, so we store it here
    // to avoid upcast

    var verticalVelocity: Double = 0.0
    var verticalOffset: Double = 0.0

    /**
     * Terminal velocity of a skydiver in m/s.
     * */
    var fallSpeed: Double = 55.0
    var jumpSpeed: Double = 10.0
    var maxJumpHeight: Double = 0.0

    /**
     * Slope angle that is set (used for returning the exact value)
     * */
    var maxSlopeRadians: Double = 0.0

    /**
     * Cosine equivalent of m_maxSlopeRadians (calculated once when set, for optimization)
     * */
    var maxSlopeCosine: Double = 0.0

    /**
     * 1G acceleration
     * */
    var gravity: Double = 9.8
    var turnAngle: Double = 0.0
    var addedMargin: Double = 0.02 // @todo: remove this and fix the code

    // this is the desired walk direction, set by the user
    private val walkDirection = Vector3d()
    var normalizedDirection: Vector3d = Vector3d()

    // some internal variables
    var currentPosition: Vector3d = Vector3d()
    var currentStepOffset: Double = 0.0
    var targetPosition: Vector3d = Vector3d()

    // keep track of the contact manifolds
    var manifoldArray = ArrayList<PersistentManifold>()

    var touchingContact: Boolean = false
    var touchingNormal: Vector3d = Vector3d()

    var wasOnGround: Boolean = false

    var useGhostObjectSweepTest: Boolean = true
    private var useWalkDirection: Boolean = true
    private var velocityTimeInterval: Double = 1.0

    var maxSlope: Double
        get() = maxSlopeRadians
        set(slopeRadians) {
            maxSlopeRadians = slopeRadians
            maxSlopeCosine = cos(slopeRadians)
        }

    init {
        this.maxSlope = (50.0 / 180.0) * Math.PI
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
     *
     *
     *
     * This call will reset any velocity set by [.setVelocityForTimeInterval].
     */
    @Suppress("unused")
    fun setWalkDirection(walkDirection: Vector3d) {
        useWalkDirection = true
        this.walkDirection.set(walkDirection)
        normalizedDirection.set(getNormalizedVector(walkDirection, Stack.newVec()))
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
        normalizedDirection.set(getNormalizedVector(walkDirection, Stack.newVec()))
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

        currentPosition.set(ghostObject.getWorldTransform(Stack.newTrans()).origin)
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
            println("playerStep 4")

            //printf("  time: %f", m_velocityTimeInterval);

            // still have some time left for moving!
            val dtMoving = min(dt, velocityTimeInterval)
            velocityTimeInterval -= dt

            // how far will we move while we are moving?
            val move = Stack.newVec()
            move.setScale(dtMoving, walkDirection)

            //printf("  dtMoving: %f", dtMoving);

            // okay, step
            stepForwardAndStrafe(collisionWorld, move)
        }
        stepDown(collisionWorld, dt)

        //printf("\n");
        xform.setTranslation(currentPosition)
        ghostObject.setWorldTransform(xform)
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
     *
     *
     *
     * From: [Stanford-Article](http://www-cs-students.stanford.edu/~adityagp/final/node3.html)
     */
    fun computeReflectionDirection(direction: Vector3d, normal: Vector3d, out: Vector3d): Vector3d {
        // return direction - (btScalar(2.0) * direction.dot(normal)) * normal;
        out.set(normal)
        out.mul(-2.0 * direction.dot(normal))
        out.add(direction)
        return out
    }

    /**
     * Returns the portion of 'direction' that is parallel to 'normal'
     */
    fun parallelComponent(direction: Vector3d, normal: Vector3d, out: Vector3d): Vector3d {
        //btScalar magnitude = direction.dot(normal);
        //return normal * magnitude;
        out.set(normal)
        out.mul(direction.dot(normal))
        return out
    }

    /**
     * Returns the portion of 'direction' that is perpindicular to 'normal'
     */
    fun perpindicularComponent(direction: Vector3d, normal: Vector3d, out: Vector3d): Vector3d {
        //return direction - parallelComponent(direction, normal);
        val perpendicular = parallelComponent(direction, normal, out)
        perpendicular.mul(-1.0)
        perpendicular.add(direction)
        return perpendicular
    }

    fun recoverFromPenetration(collisionWorld: CollisionWorld): Boolean {
        var penetration = false

        collisionWorld.dispatcher.dispatchAllCollisionPairs(
            ghostObject.overlappingPairCache, collisionWorld.dispatchInfo, collisionWorld.dispatcher
        )

        currentPosition.set(ghostObject.getWorldTransform(Stack.newTrans()).origin)

        var maxPen = 0.0
        for (i in 0 until ghostObject.overlappingPairCache.numOverlappingPairs) {
            manifoldArray.clear()

            val collisionPair = ghostObject.overlappingPairCache.overlappingPairArray.getQuick(i)
            collisionPair?.algorithm?.getAllContactManifolds(manifoldArray)

            for (j in manifoldArray.indices) {
                val manifold = manifoldArray[j]
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

                        currentPosition.setScaleAdd(directionSign * dist * 0.2, pt.normalWorldOnB, currentPosition)

                        penetration = true
                    } // else printf("touching %f\n", dist);
                }

                //manifold->clearManifold();
            }
        }

        val newTrans = ghostObject.getWorldTransform(Stack.newTrans())
        newTrans.setTranslation(currentPosition)
        ghostObject.setWorldTransform(newTrans)

        //printf("m_touchingNormal = %f,%f,%f\n",m_touchingNormal[0],m_touchingNormal[1],m_touchingNormal[2]);

        //System.out.println("recoverFromPenetration "+penetration+" "+touchingNormal);
        return penetration
    }

    private val upAxisValue get() = upAxisDirection[upAxis.id]

    fun stepUp(world: CollisionWorld) {
        // phase 1: up
        val start = Stack.newTrans()
        val end = Stack.newTrans()
        targetPosition.setScaleAdd(stepHeight + max(verticalOffset, 0.0), upAxisValue, currentPosition)

        start.setIdentity()
        end.setIdentity()

        /* FIXME: Handle penetration properly */
        start.origin.setScaleAdd(convexShape.margin + addedMargin, upAxisValue, currentPosition)
        end.setTranslation(targetPosition)

        // Find only sloped/flat surface hits, avoid wall and ceiling hits...
        val up = Stack.newVec()
        up.setScale(-1.0, upAxisValue)
        val callback = KinematicClosestNotMeConvexResultCallback(ghostObject, up, 0.0)
        callback.collisionFilterGroup = this.ghostObject.broadphaseHandle!!.collisionFilterGroup
        callback.collisionFilterMask = this.ghostObject.broadphaseHandle!!.collisionFilterMask

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
            currentPosition.setInterpolate(currentPosition, targetPosition, callback.closestHitFraction)
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
        movementDirection.setSub(targetPosition, currentPosition)
        val movementLength = movementDirection.length()
        if (movementLength > BulletGlobals.SIMD_EPSILON) {
            movementDirection.normalize()

            val reflectDir = computeReflectionDirection(movementDirection, hitNormal, Stack.newVec())
            reflectDir.normalize()

            val parallelDir = parallelComponent(reflectDir, hitNormal, Stack.newVec())
            val perpindicularDir = perpindicularComponent(reflectDir, hitNormal, Stack.newVec())

            targetPosition.set(currentPosition)
            if (false)  //tangentMag != 0.0)
            {
                val parComponent = Stack.newVec()
                parComponent.setScale(tangentMag * movementLength, parallelDir)
                //printf("parComponent=%f,%f,%f\n",parComponent[0],parComponent[1],parComponent[2]);
                targetPosition.add(parComponent)
            }

            if (normalMag != 0.0) {
                val perpComponent = Stack.newVec()
                perpComponent.setScale(normalMag * movementLength, perpindicularDir)
                //printf("perpComponent=%f,%f,%f\n",perpComponent[0],perpComponent[1],perpComponent[2]);
                targetPosition.add(perpComponent)
            }
        } // else printf("movementLength don't normalize a zero vector\n");
    }

    fun stepForwardAndStrafe(collisionWorld: CollisionWorld, walkMove: Vector3d) {
        // printf("m_normalizedDirection=%f,%f,%f\n",
        // 	m_normalizedDirection[0],m_normalizedDirection[1],m_normalizedDirection[2]);
        // phase 2: forward and strafe
        val start = Stack.newTrans()
        val end = Stack.newTrans()
        targetPosition.setAdd(currentPosition, walkMove)
        start.setIdentity()
        end.setIdentity()

        var fraction = 1.0
        val distance2Vec = Stack.newVec()
        distance2Vec.setSub(currentPosition, targetPosition)
        // var distance2 = distance2Vec.lengthSquared()

        //printf("distance2=%f\n",distance2);

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

            val callback = KinematicClosestNotMeConvexResultCallback(ghostObject, upAxisValue, -1.0)
            callback.collisionFilterGroup = this.ghostObject.broadphaseHandle!!.collisionFilterGroup
            callback.collisionFilterMask = this.ghostObject.broadphaseHandle!!.collisionFilterMask

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
                hitDistanceVec.setSub(callback.hitPointWorld, currentPosition)

                //double hitDistance = hitDistanceVec.length();

                // if the distance is farther than the collision margin, move
                //if (hitDistance > addedMargin) {
                //	//printf("callback.m_closestHitFraction=%f\n",callback.m_closestHitFraction);
                //	currentPosition.interpolate(currentPosition, targetPosition, callback.closestHitFraction);
                //}
                updateTargetPositionBasedOnCollision(callback.hitNormalWorld)

                currentDir.setSub(targetPosition, currentPosition)
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
        stepDrop.setScale(currentStepOffset + additionalDownStep, upAxisValue)
        val downVelocity = (if (additionalDownStep == 0.0 && verticalVelocity < 0.0) -verticalVelocity else 0.0) * dt
        val gravityDrop = Stack.newVec()
        gravityDrop.setScale(downVelocity, upAxisValue)
        targetPosition.sub(stepDrop)
        targetPosition.sub(gravityDrop)

        start.setIdentity()
        end.setIdentity()

        start.setTranslation(currentPosition)
        end.setTranslation(targetPosition)

        val callback = KinematicClosestNotMeConvexResultCallback(ghostObject, upAxisValue, maxSlopeCosine)
        callback.collisionFilterGroup = this.ghostObject.broadphaseHandle!!.collisionFilterGroup
        callback.collisionFilterMask = this.ghostObject.broadphaseHandle!!.collisionFilterMask

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
            currentPosition.setInterpolate(currentPosition, targetPosition, callback.closestHitFraction)
            verticalVelocity = 0.0
            verticalOffset = 0.0
        } else {
            // we dropped the full height
            currentPosition.set(targetPosition)
        }
    }

    /** ///////////////////////////////////////////////////////////////////////// */
    private class KinematicClosestNotMeRayResultCallback(var me: CollisionObject?) :
        ClosestRayResultCallback(Vector3d(), Vector3d()) {
        override fun addSingleResult(rayResult: LocalRayResult, normalInWorldSpace: Boolean): Double {
            if (rayResult.collisionObject === me) {
                return 1.0
            }

            return super.addSingleResult(rayResult, normalInWorldSpace)
        }
    }

    /** ///////////////////////////////////////////////////////////////////////// */
    private class KinematicClosestNotMeConvexResultCallback(me: CollisionObject?, up: Vector3d, minSlopeDot: Double) :
        ClosestConvexResultCallback() {
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
            if (convexResult.hitCollisionObject === me) {
                return 1.0
            }

            val hitNormalWorld: Vector3d?
            if (normalInWorldSpace) {
                hitNormalWorld = convexResult.hitNormalLocal
            } else {
                //need to transform normal into worldspace
                hitNormalWorld = Stack.newVec()
                hitCollisionObject!!.getWorldTransform(Stack.newTrans()).basis.revTransform(
                    convexResult.hitNormalLocal,
                    hitNormalWorld
                )
            }

            val dotUp = up.dot(hitNormalWorld)
            if (dotUp < minSlopeDot) {
                return 1.0
            }

            return super.addSingleResult(convexResult, normalInWorldSpace)
        }
    }

    companion object {
        private val upAxisDirection = arrayOf<Vector3d>(
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 0.0),
            Vector3d(0.0, 0.0, 1.0),
        )

        // static helper method
        private fun getNormalizedVector(v: Vector3d, out: Vector3d): Vector3d {
            out.set(v)
            out.normalize()
            if (out.length() < BulletGlobals.SIMD_EPSILON) {
                out.set(0.0, 0.0, 0.0)
            }
            return out
        }
    }
}
