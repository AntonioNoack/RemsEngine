package com.bulletphysics.collision.dispatch

import com.bulletphysics.BulletGlobals
import com.bulletphysics.BulletStats.profile
import com.bulletphysics.collision.broadphase.BroadphaseInterface
import com.bulletphysics.collision.broadphase.BroadphaseProxy
import com.bulletphysics.collision.broadphase.Dispatcher
import com.bulletphysics.collision.broadphase.DispatcherInfo
import com.bulletphysics.collision.broadphase.OverlappingPairCache
import com.bulletphysics.collision.narrowphase.SubSimplexConvexCast
import com.bulletphysics.collision.narrowphase.TriangleConvexCastCallback
import com.bulletphysics.collision.narrowphase.TriangleRaycastCallback
import com.bulletphysics.collision.shapes.BvhTriangleMeshShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.CompoundShape
import com.bulletphysics.collision.shapes.ConcaveShape
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.dynamics.CollisionTree
import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.IDebugDraw
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.TransformUtil
import com.bulletphysics.linearmath.VectorUtil.setInterpolate3
import com.bulletphysics.linearmath.VectorUtil.setMax
import com.bulletphysics.linearmath.VectorUtil.setMin
import cz.advel.stack.Stack
import me.anno.ecs.components.collider.CollisionFilters
import me.anno.ecs.components.collider.CollisionFilters.collides
import me.anno.utils.structures.lists.Lists.swapRemove
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * CollisionWorld is interface and container for the collision detection.
 *
 * @author jezek2
 */
abstract class CollisionWorld(val dispatcher: Dispatcher, val broadphase: BroadphaseInterface) {

    val collisionObjects = ArrayList<CollisionObject>()
    val dispatchInfo = DispatcherInfo()

    @JvmField
    var debugDrawer: IDebugDraw? = null

    fun destroy() {
        // clean up remaining objects
        for (i in collisionObjects.indices) {
            val collisionObject = collisionObjects[i]
            val bp = collisionObject.broadphaseHandle ?: continue
            // only clear the cached algorithms
            broadphase.overlappingPairCache.cleanProxyFromPairs(bp, dispatcher)
            broadphase.destroyProxy(bp, dispatcher)
        }
    }

    fun addCollisionObject(
        collisionObject: CollisionObject,
        collisionFilter: Int// = CollisionFilterGroups.DEFAULT_ALL,
    ) {
        // check that the object isn't already added
        if (collisionObjects.contains(collisionObject)) return

        collisionObjects.add(collisionObject)

        // calculate new AABB
        val shape = collisionObject.collisionShape!!
        val minAabb = Stack.newVec3d()
        val maxAabb = Stack.newVec3d()
        shape.getBounds(collisionObject.worldTransform, minAabb, maxAabb)

        val type = shape.shapeType
        collisionObject.broadphaseHandle = broadphase.createProxy(
            minAabb, maxAabb, type,
            collisionObject, collisionFilter, dispatcher
        )

        Stack.subVec3d(2)
    }

    fun performDiscreteCollisionDetection() {
        profile("performDiscreteCollisionDetection") {
            updateAabbs()
            profile("calculateOverlappingPairs") {
                broadphase.calculateOverlappingPairs(dispatcher)
            }
            profile("dispatchAllCollisionPairs") {
                dispatcher.dispatchAllCollisionPairs(broadphase.overlappingPairCache, dispatchInfo, dispatcher)
            }
        }
    }

    fun removeCollisionObject(collisionObject: CollisionObject) {
        val bp = collisionObject.broadphaseHandle
        if (bp != null) {
            //
            // only clear the cached algorithms
            //
            broadphase.overlappingPairCache.cleanProxyFromPairs(bp, dispatcher)
            broadphase.destroyProxy(bp, dispatcher)
            collisionObject.broadphaseHandle = null
        }
        collisionObjects.swapRemove(collisionObject)
    }

    val pairCache: OverlappingPairCache
        get() = broadphase.overlappingPairCache

    // JAVA NOTE: ported from 2.74, missing contact threshold stuff
    fun updateSingleAabb(colObj: CollisionObject) {
        val minAabb = Stack.newVec3d()
        val maxAabb = Stack.newVec3d()

        val shape = colObj.collisionShape!!
        shape.getBounds(colObj.worldTransform, minAabb, maxAabb)

        // need to increase the aabb for contact thresholds

        // Should be greater than or equal to the collision margin of the objects involved.
        // Too small: Contacts break too early → jittery or unstable simulation.
        // Too large: Contacts persist unrealistically → ghost contacts or sticking.
        val margin = BulletGlobals.contactBreakingThreshold.toDouble()
        minAabb.sub(margin)
        maxAabb.add(margin)

        if (colObj.isStaticOrKinematicObject || (minAabb.isFinite && maxAabb.isFinite)) {
            broadphase.setAabb(colObj.broadphaseHandle!!, minAabb, maxAabb, dispatcher)
        }

        Stack.subVec3d(2)
    }

    fun updateAabbs() {
        profile("updateAabbs") {
            for (i in collisionObjects.indices) {
                val colObj = collisionObjects[i]
                // todo this can be skipped if the object is inactive AND
                //  only iff we update it manually if we transform an object (editor)
                updateSingleAabb(colObj)
            }
        }
    }

    private class BridgeTriangleConvexCastCallback(
        castShape: ConvexShape, from: Transform, to: Vector3d,
        val resultCallback: ConvexResultCallback,
        val collisionObject: CollisionObject,
        triangleMesh: ConcaveShape, triangleToWorld: Transform
    ) : TriangleConvexCastCallback(
        castShape, from, to,
        triangleToWorld, triangleMesh.margin
    ) {

        var normalInWorldSpace: Boolean = false

        override fun reportHit(
            hitNormalLocal: Vector3f,
            hitPointLocal: Vector3d,
            hitFraction: Float,
            partId: Int,
            triangleIndex: Int
        ): Float {
            val shapeInfo = LocalShapeInfo()
            shapeInfo.shapePart = partId
            shapeInfo.triangleIndex = triangleIndex
            return if (hitFraction <= resultCallback.closestHitFraction) {
                val convexResult = LocalConvexResult(
                    collisionObject, shapeInfo,
                    hitNormalLocal, hitPointLocal, hitFraction
                )
                resultCallback.addSingleResult(convexResult, normalInWorldSpace)
            } else hitFraction
        }
    }

    /**
     * rayTest performs a raycast on all objects in the CollisionWorld, and calls the resultCallback.
     * This allows for several queries: first hit, all hits, any hit, dependent on the value returned by the callback.
     */
    fun rayTest(rayFromWorld: Vector3d, rayToWorld: Vector3d, resultCallback: RayResultCallback) {
        val rayFromTrans = Stack.newTrans()
        rayFromTrans.setIdentity()
        rayFromTrans.setTranslation(rayFromWorld)

        // go over all objects, and if the ray intersects their aabb, do a ray-shape query using convexCaster (CCD)
        val collisionObjectAabbMin = Stack.newVec3d()
        val collisionObjectAabbMax = Stack.newVec3d()
        val hitLambda = Stack.newFloatPtr()
        val hitNormal = Stack.newVec3f()

        // todo use collisionTree instead
        for (i in collisionObjects.indices) {
            // terminate further ray tests, once the closestHitFraction reached zero
            if (resultCallback.closestHitFraction == 0f) {
                break
            }

            val collisionObject = collisionObjects[i]
            // only perform raycast if filterMask matches
            if (resultCallback.needsCollision(collisionObject.broadphaseHandle!!)) {
                //RigidcollisionObject* collisionObject = ctrl->GetRigidcollisionObject();
                collisionObject.collisionShape!!
                    .getBounds(collisionObject.worldTransform, collisionObjectAabbMin, collisionObjectAabbMax)

                hitLambda[0] = resultCallback.closestHitFraction
                if (AabbUtil.rayAabb(
                        rayFromWorld, rayToWorld,
                        collisionObjectAabbMin, collisionObjectAabbMax,
                        hitLambda, hitNormal
                    )
                ) {
                    rayTestSingle(
                        rayFromTrans, rayToWorld,
                        collisionObject,
                        collisionObject.collisionShape!!,
                        collisionObject.worldTransform,
                        resultCallback
                    )
                }
            }
        }

        Stack.subTrans(1)
        Stack.subVec3f(1)
        Stack.subVec3d(2)
        Stack.subFloatPtr(1)
    }

    /**
     * convexTest performs a swept convex cast on all objects in the [CollisionWorld], and calls the resultCallback
     * This allows for several queries: first hit, all hits, any hit, dependent on the value return by the callback.
     */
    abstract fun convexSweepTest(
        selfShape: ConvexShape,
        convexFromWorld: Transform,
        convexToWorld: Transform,
        resultCallback: ConvexResultCallback
    )

    /**
     * LocalShapeInfo gives extra information for complex shapes.
     * Currently, only btTriangleMeshShape is available, so it just contains triangleIndex and subpart.
     */
    class LocalShapeInfo {
        var shapePart: Int = 0
        var triangleIndex: Int = 0 //const btCollisionShape*	m_shapeTemp;
        //const btTransform*	m_shapeLocalTransform;
    }

    class LocalRayResult(
        val collisionObject: CollisionObject?,
        val localShapeInfo: LocalShapeInfo?,
        hitNormalLocal: Vector3f,
        var hitFraction: Float
    ) {
        val hitNormalLocal = Vector3f(hitNormalLocal)
    }

    /**
     * RayResultCallback is used to report new raycast results.
     */
    abstract class RayResultCallback {

        var closestHitFraction = 1f
        var collisionObject: CollisionObject? = null
        var collisionFilter = CollisionFilters.DEFAULT_ALL

        fun hasHit(): Boolean {
            return (collisionObject != null)
        }

        fun needsCollision(proxy0: BroadphaseProxy): Boolean {
            return collides(proxy0.collisionFilter, collisionFilter)
        }

        abstract fun addSingleResult(rayResult: LocalRayResult, normalInWorldSpace: Boolean): Float
    }

    open class ClosestRayResultCallback(rayFromWorld: Vector3d, rayToWorld: Vector3d) : RayResultCallback() {
        val rayFromWorld = Vector3d(rayFromWorld) //used to calculate hitPointWorld from hitFraction
        val rayToWorld = Vector3d(rayToWorld)

        val hitNormalWorld = Vector3f()
        val hitPointWorld = Vector3d()

        override fun addSingleResult(rayResult: LocalRayResult, normalInWorldSpace: Boolean): Float {
            // caller already does the filter on the closestHitFraction
            assert(rayResult.hitFraction <= closestHitFraction)

            closestHitFraction = rayResult.hitFraction
            collisionObject = rayResult.collisionObject
            if (normalInWorldSpace) {
                hitNormalWorld.set(rayResult.hitNormalLocal)
            } else {
                // need to transform normal into worldspace
                hitNormalWorld.set(rayResult.hitNormalLocal)
                collisionObject!!.worldTransform.transformDirection(hitNormalWorld)
            }

            setInterpolate3(hitPointWorld, rayFromWorld, rayToWorld, rayResult.hitFraction.toDouble())
            return rayResult.hitFraction
        }
    }

    class LocalConvexResult(
        val hitCollisionObject: CollisionObject?,
        val localShapeInfo: LocalShapeInfo?,
        hitNormalLocal: Vector3f,
        hitPointLocal: Vector3d, // local, so could it be 3f?
        var hitFraction: Float
    ) {
        val hitNormalLocal = Vector3f(hitNormalLocal)
        val hitPointLocal = Vector3d(hitPointLocal)
    }

    abstract class ConvexResultCallback {
        @JvmField
        var closestHitFraction = 1f

        @JvmField
        var collisionFilter = CollisionFilters.DEFAULT_ALL

        fun init() {
            closestHitFraction = 1f
            collisionFilter = CollisionFilters.DEFAULT_ALL
        }

        fun hasHit(): Boolean {
            return closestHitFraction < 1f
        }

        open fun needsCollision(proxy0: BroadphaseProxy): Boolean {
            return collides(proxy0.collisionFilter, collisionFilter)
        }

        abstract fun addSingleResult(convexResult: LocalConvexResult, normalInWorldSpace: Boolean): Float
    }

    open class ClosestConvexResultCallback : ConvexResultCallback() {
        val convexFromWorld = Vector3d() // used to calculate hitPointWorld from hitFraction
        val convexToWorld = Vector3d()
        val hitNormalWorld = Vector3f()
        val hitPointWorld = Vector3d()
        var hitCollisionObject: CollisionObject? = null

        fun init(convexFromWorld: Vector3d, convexToWorld: Vector3d) {
            super.init()
            this.convexFromWorld.set(convexFromWorld)
            this.convexToWorld.set(convexToWorld)
            this.hitCollisionObject = null
        }

        override fun addSingleResult(convexResult: LocalConvexResult, normalInWorldSpace: Boolean): Float {
            // caller already does the filter on the m_closestHitFraction
            assert(convexResult.hitFraction <= closestHitFraction)

            closestHitFraction = convexResult.hitFraction
            hitCollisionObject = convexResult.hitCollisionObject
            if (normalInWorldSpace) {
                hitNormalWorld.set(convexResult.hitNormalLocal)
                if (hitNormalWorld.length() > 2) {
                    println("CollisionWorld.addSingleResult world $hitNormalWorld")
                }
            } else {
                // need to transform normal into world space
                hitNormalWorld.set(convexResult.hitNormalLocal)
                convexResult.hitCollisionObject!!.worldTransform
                    .transformDirection(hitNormalWorld)
                if (hitNormalWorld.length() > 2) {
                    println("CollisionWorld.addSingleResult world $hitNormalWorld")
                }
            }

            hitPointWorld.set(convexResult.hitPointLocal)
            return convexResult.hitFraction
        }
    }

    private class BridgeTriangleRaycastCallback(
        from: Vector3d, to: Vector3d,
        var resultCallback: RayResultCallback,
        var collisionObject: CollisionObject?
    ) : TriangleRaycastCallback(from, to) {

        override fun reportHit(hitNormalLocal: Vector3f, hitFraction: Float, partId: Int, triangleIndex: Int): Float {
            val shapeInfo = LocalShapeInfo()
            shapeInfo.shapePart = partId
            shapeInfo.triangleIndex = triangleIndex

            val rayResult = LocalRayResult(collisionObject, shapeInfo, hitNormalLocal, hitFraction)

            val normalInWorldSpace = false
            return resultCallback.addSingleResult(rayResult, normalInWorldSpace)
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(CollisionWorld::class)

        fun calculateTemporalBounds(
            selfShape: ConvexShape,
            convexFromWorld: Transform,
            convexToWorld: Transform,
            selfAabbMin: Vector3d,
            selfAabbMax: Vector3d
        ) {

            val linVel = Stack.newVec3f()
            val angVel = TransformUtil.calculateVelocity(convexFromWorld, convexToWorld, 1f, linVel)
            val tmp = Stack.newTrans()
            tmp.setIdentity()
            tmp.basis.set(convexFromWorld.basis)
            selfShape.calculateTemporalAabb(tmp, linVel, angVel, 1f, selfAabbMin, selfAabbMax)

            Stack.subVec3f(1)
            Stack.subTrans(1)
        }

        /**
         * convexTest performs a swept convex cast on all objects in the [CollisionWorld], and calls the resultCallback
         * This allows for several queries: first hit, all hits, any hit, dependent on the value return by the callback.
         */
        fun convexSweepTest(
            selfShape: ConvexShape,
            convexFromWorld: Transform,
            convexToWorld: Transform,
            resultCallback: ConvexResultCallback,
            allowedPenetration: Float,
            otherObjects: List<CollisionObject>
        ) {
            val selfAabbMin = Stack.newVec3d()
            val selfAabbMax = Stack.newVec3d()

            // Compute AABB that encompasses angular movement
            calculateTemporalBounds(selfShape, convexFromWorld, convexToWorld, selfAabbMin, selfAabbMax)

            val otherAabbMin = Stack.newVec3d()
            val otherAabbMax = Stack.newVec3d()
            val hitLambda = Stack.newFloatPtr()

            // go over all objects, and if the ray intersects their aabb + cast shape aabb,
            // do a ray-shape query using convexCaster (CCD)
            val hitNormal = Stack.newVec3f()
            var stackPos: IntArray? = null
            for (i in otherObjects.indices) {
                val other = otherObjects[i]
                // only perform raycast if filterMask matches
                if (resultCallback.needsCollision(other.broadphaseHandle!!)) {
                    stackPos = Stack.getPosition(stackPos)

                    val otherShape = other.collisionShape!!
                    otherShape.getBounds(other.worldTransform, otherAabbMin, otherAabbMax)
                    AabbUtil.aabbExpand(otherAabbMin, otherAabbMax, selfAabbMin, selfAabbMax) // minkowski sum
                    hitLambda[0] = 1f // could use resultCallback.closestHitFraction, but needs testing
                    if (AabbUtil.rayAabb(
                            convexFromWorld.origin,
                            convexToWorld.origin,
                            otherAabbMin, otherAabbMax,
                            hitLambda, hitNormal
                        )
                    ) {
                        objectQuerySingle(
                            selfShape, convexFromWorld, convexToWorld,
                            other, otherShape, other.worldTransform,
                            resultCallback, allowedPenetration
                        )
                    }
                    Stack.reset(stackPos)
                }
            }

            Stack.subFloatPtr(1)
            Stack.subVec3d(3)
            Stack.subVec3f(1)
        }

        /**
         * convexTest performs a swept convex cast on all objects in the [CollisionWorld], and calls the resultCallback
         * This allows for several queries: first hit, all hits, any hit, dependent on the value return by the callback.
         */
        fun convexSweepTest(
            selfShape: ConvexShape,
            convexFromWorld: Transform,
            convexToWorld: Transform,
            callback: ConvexResultCallback,
            allowedPenetration: Float,
            otherObjects: CollisionTree
        ) {
            val selfAabbMin = Stack.newVec3d()
            val selfAabbMax = Stack.newVec3d()

            // Compute AABB that encompasses angular movement
            calculateTemporalBounds(selfShape, convexFromWorld, convexToWorld, selfAabbMin, selfAabbMax)

            val otherAabbMin = Stack.newVec3d()
            val otherAabbMax = Stack.newVec3d()
            val hitLambda = Stack.newFloatPtr()

            // go over all objects, and if the ray intersects their aabb + cast shape aabb,
            // do a ray-shape query using convexCaster (CCD)
            val hitNormal = Stack.newVec3f()
            var stackPos: IntArray? = null
            otherObjects.query(selfAabbMin, selfAabbMax) { other ->
                // only perform raycast if filterMask matches
                if (callback.needsCollision(other.broadphaseHandle!!)) {
                    stackPos = Stack.getPosition(stackPos)

                    val otherShape = other.collisionShape!!
                    otherShape.getBounds(other.worldTransform, otherAabbMin, otherAabbMax)
                    AabbUtil.aabbExpand(otherAabbMin, otherAabbMax, selfAabbMin, selfAabbMax) // minkowski sum
                    hitLambda[0] = 1f // could use resultCallback.closestHitFraction, but needs testing
                    if (AabbUtil.rayAabb(
                            convexFromWorld.origin,
                            convexToWorld.origin,
                            otherAabbMin, otherAabbMax,
                            hitLambda, hitNormal
                        )
                    ) {
                        objectQuerySingle(
                            selfShape, convexFromWorld, convexToWorld,
                            other, otherShape, other.worldTransform,
                            callback, allowedPenetration
                        )
                    }
                    Stack.reset(stackPos)
                }
                false
            }

            Stack.subFloatPtr(1)
            Stack.subVec3d(3)
            Stack.subVec3f(1)
        }

        @JvmStatic
        fun rayTestSingle(
            rayFromTrans: Transform, rayToTrans: Vector3d,
            collisionObject: CollisionObject,
            collisionShape: CollisionShape,
            collisionTransform: Transform,
            callback: RayResultCallback
        ) {
            val pointShape = SphereShape(0f)
            pointShape.margin = 0f

            when (collisionShape) {
                is ConvexShape -> {
                    val castResult = Stack.newCastResult()
                    castResult.fraction = callback.closestHitFraction

                    val simplexSolver = Stack.newVSS()
                    if (SubSimplexConvexCast.calcTimeOfImpactImpl(
                            pointShape, collisionShape, simplexSolver,
                            rayFromTrans, rayToTrans,
                            collisionTransform, collisionTransform.origin, castResult
                        )
                    ) {
                        //add hit
                        if (castResult.normal.lengthSquared() > 0.0001f) {
                            if (castResult.fraction < callback.closestHitFraction) {
                                //#ifdef USE_SUBSIMPLEX_CONVEX_CAST
                                //rotate normal into worldspace
                                rayFromTrans.basis.transform(castResult.normal)

                                //#endif //USE_SUBSIMPLEX_CONVEX_CAST
                                castResult.normal.normalize()
                                val localRayResult = LocalRayResult(
                                    collisionObject,
                                    null,
                                    castResult.normal,
                                    castResult.fraction
                                )

                                val normalInWorldSpace = true
                                callback.addSingleResult(localRayResult, normalInWorldSpace)
                            }
                        }
                    }

                    Stack.subVSS(1)
                    Stack.subCastResult(1)
                }
                is ConcaveShape -> {
                    val worldToCollisionObject = Stack.newTrans()
                    worldToCollisionObject.setInverse(collisionTransform)

                    val rayFromLocal = Stack.newVec3d(rayFromTrans.origin)
                    worldToCollisionObject.transformPosition(rayFromLocal)

                    val rayToLocal = Stack.newVec3d(rayToTrans)
                    worldToCollisionObject.transformPosition(rayToLocal)

                    val rcb = BridgeTriangleRaycastCallback(
                        rayFromLocal, rayToLocal,
                        callback, collisionObject
                    )
                    rcb.hitFraction = callback.closestHitFraction

                    if (collisionShape is BvhTriangleMeshShape) {
                        // optimized version for BvhTriangleMeshShape
                        collisionShape.performRaycast(rcb, rayFromLocal, rayToLocal)
                    } else {

                        val rayAabbMinLocal = Stack.newVec3d(rayFromLocal)
                        setMin(rayAabbMinLocal, rayToLocal)

                        val rayAabbMaxLocal = Stack.newVec3d(rayFromLocal)
                        setMax(rayAabbMaxLocal, rayToLocal)

                        collisionShape.processAllTriangles(rcb, rayAabbMinLocal, rayAabbMaxLocal)

                        Stack.subVec3d(2)
                    }

                    Stack.subVec3d(2)
                    Stack.subTrans(1)
                }
                is CompoundShape -> {
                    // todo: use AABB tree or other BVH acceleration structure!
                    val childWorldTrans = Stack.newTrans()
                    val children = collisionShape.children
                    for (i in children.indices) {
                        val child = children[i]
                        val childCollisionShape = child.shape
                        childWorldTrans.setMul(collisionTransform, child.transform)
                        // replace collision shape so that callback can determine the triangle
                        val saveCollisionShape = collisionObject.collisionShape
                        collisionObject.collisionShape = (childCollisionShape)
                        rayTestSingle(
                            rayFromTrans, rayToTrans,
                            collisionObject,
                            childCollisionShape,
                            childWorldTrans,
                            callback
                        )
                        // restore
                        collisionObject.collisionShape = (saveCollisionShape)
                    }
                    Stack.subTrans(1)
                }
                else -> LOGGER.warn("rayTestSingle is not supported for ${collisionShape.javaClass}")
            }
        }

        /**
         * objectQuerySingle performs a collision detection query and calls the resultCallback. It is used internally by rayTest.
         */
        @JvmStatic
        fun objectQuerySingle(
            convexShape: ConvexShape,
            convexFromTrans: Transform,
            convexToTrans: Transform,
            otherObject: CollisionObject,
            otherShape: CollisionShape,
            otherWorldTransform: Transform,
            resultCallback: ConvexResultCallback,
            allowedPenetration: Float
        ) {
            when (otherShape) {
                is ConvexShape -> {
                    val castResult = Stack.newCastResult()
                    castResult.allowedPenetration = allowedPenetration
                    castResult.fraction = 1f

                    // JAVA TODO: should be convexCaster1
                    //ContinuousConvexCollision convexCaster1(castShape,convexShape,&simplexSolver,&gjkEpaPenetrationSolver);
                    //btSubsimplexConvexCast convexCaster3(castShape,convexShape,&simplexSolver);
                    val castPtr = Stack.newGjkCC(convexShape, otherShape)
                    if (castPtr.calcTimeOfImpact(
                            convexFromTrans, convexToTrans.origin,
                            otherWorldTransform, otherWorldTransform.origin,
                            castResult
                        )
                    ) {
                        // add hit
                        if (castResult.normal.lengthSquared() > 0.0001f && castResult.fraction < resultCallback.closestHitFraction) {
                            castResult.normal.normalize()
                            val localConvexResult = LocalConvexResult(
                                otherObject,
                                null,
                                castResult.normal,
                                castResult.hitPoint,
                                castResult.fraction
                            )

                            val normalInWorldSpace = true
                            resultCallback.addSingleResult(localConvexResult, normalInWorldSpace)
                        }
                    }
                    Stack.subCastResult(1)
                    Stack.subGjkCC(1)
                }
                is ConcaveShape -> {
                    val worldToCollisionObject = Stack.newTrans()
                    worldToCollisionObject.setInverse(otherWorldTransform)

                    val convexFromLocal = Stack.newVec3d()
                    convexFromLocal.set(convexFromTrans.origin)
                    worldToCollisionObject.transformPosition(convexFromLocal)

                    val convexToLocal = Stack.newVec3d()
                    convexToLocal.set(convexToTrans.origin)
                    worldToCollisionObject.transformPosition(convexToLocal)

                    // rotation of box in local mesh space = MeshRotation^-1 * ConvexToRotation
                    val rotationXform = Stack.newTrans()
                    val tmpMat = Stack.newMat()
                    worldToCollisionObject.basis.mul(convexToTrans.basis, tmpMat)
                    rotationXform.set(tmpMat)

                    val callback = BridgeTriangleConvexCastCallback(
                        convexShape, convexFromTrans, convexToTrans.origin, resultCallback,
                        otherObject, otherShape, otherWorldTransform
                    )
                    callback.hitFraction = resultCallback.closestHitFraction

                    val boxMinLocal = Stack.newVec3d()
                    val boxMaxLocal = Stack.newVec3d()
                    convexShape.getBounds(rotationXform, boxMinLocal, boxMaxLocal)

                    if (otherShape is BvhTriangleMeshShape) {

                        callback.normalInWorldSpace = true

                        otherShape.performConvexCast(callback, convexFromLocal, convexToLocal, boxMinLocal, boxMaxLocal)

                        Stack.subVec3d(4)
                        Stack.subTrans(2)
                        Stack.subMat(1)
                    } else {

                        callback.normalInWorldSpace = false

                        val rayAabbMinLocal = Stack.newVec3d()
                        val rayAabbMaxLocal = Stack.newVec3d()

                        convexFromLocal.min(convexToLocal, rayAabbMinLocal).add(boxMinLocal)
                        convexFromLocal.max(convexToLocal, rayAabbMaxLocal).add(boxMaxLocal)

                        otherShape.processAllTriangles(callback, rayAabbMinLocal, rayAabbMaxLocal)

                        Stack.subVec3d(6)
                        Stack.subTrans(2)
                        Stack.subMat(1)
                    }
                }
                is CompoundShape -> {
                    // todo: use AABB tree or other BVH acceleration structure!
                    val childWorldTrans = Stack.newTrans()
                    val children = otherShape.children
                    for (i in children.indices) {
                        val child = children[i]
                        val childCollisionShape = child.shape
                        childWorldTrans.setMul(otherWorldTransform, child.transform)
                        // replace collision shape so that callback can determine the triangle
                        val saveCollisionShape = otherObject.collisionShape
                        otherObject.collisionShape = childCollisionShape
                        objectQuerySingle(
                            convexShape, convexFromTrans, convexToTrans,
                            otherObject, childCollisionShape, childWorldTrans,
                            resultCallback, allowedPenetration
                        )
                        // restore
                        otherObject.collisionShape = saveCollisionShape
                    }
                    Stack.subTrans(1)
                }
                else -> LOGGER.warn("objectQuerySingle is not supported for ${otherShape.javaClass}")
            }
        }
    }
}
