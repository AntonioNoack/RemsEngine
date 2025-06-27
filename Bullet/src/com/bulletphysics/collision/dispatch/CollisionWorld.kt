package com.bulletphysics.collision.dispatch

import com.bulletphysics.BulletGlobals
import com.bulletphysics.BulletStats.popProfile
import com.bulletphysics.BulletStats.pushProfile
import com.bulletphysics.collision.broadphase.BroadphaseInterface
import com.bulletphysics.collision.broadphase.BroadphaseProxy
import com.bulletphysics.collision.broadphase.CollisionFilterGroups
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.collides
import com.bulletphysics.collision.broadphase.Dispatcher
import com.bulletphysics.collision.broadphase.DispatcherInfo
import com.bulletphysics.collision.broadphase.OverlappingPairCache
import com.bulletphysics.collision.narrowphase.ConvexCast
import com.bulletphysics.collision.narrowphase.SubSimplexConvexCast
import com.bulletphysics.collision.narrowphase.TriangleConvexCastCallback
import com.bulletphysics.collision.narrowphase.TriangleRaycastCallback
import com.bulletphysics.collision.shapes.BvhTriangleMeshShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.CompoundShape
import com.bulletphysics.collision.shapes.ConcaveShape
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.collision.shapes.TriangleMeshShape
import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.IDebugDraw
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.TransformUtil
import com.bulletphysics.linearmath.VectorUtil.setInterpolate3
import com.bulletphysics.linearmath.VectorUtil.setMax
import com.bulletphysics.linearmath.VectorUtil.setMin
import com.bulletphysics.util.setMul
import cz.advel.stack.Stack
import me.anno.utils.assertions.assertFalse
import me.anno.utils.structures.lists.Lists.swapRemove
import org.joml.Vector3d

/**
 * CollisionWorld is interface and container for the collision detection.
 *
 * @author jezek2
 */
open class CollisionWorld(val dispatcher: Dispatcher, val broadphase: BroadphaseInterface) {

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
        assertFalse(collisionObjects.contains(collisionObject))

        collisionObjects.add(collisionObject)

        // calculate new AABB
        val shape = collisionObject.collisionShape!!
        val minAabb = Stack.newVec()
        val maxAabb = Stack.newVec()
        shape.getBounds(collisionObject.worldTransform, minAabb, maxAabb)

        val type = shape.shapeType
        collisionObject.broadphaseHandle = broadphase.createProxy(
            minAabb, maxAabb, type,
            collisionObject, collisionFilter,
            dispatcher, null
        )

        Stack.subVec(2)
    }

    fun performDiscreteCollisionDetection() {
        pushProfile("performDiscreteCollisionDetection")
        try {
            updateAabbs()
            pushProfile("calculateOverlappingPairs")
            try {
                broadphase.calculateOverlappingPairs(dispatcher)
            } finally {
                popProfile()
            }
            pushProfile("dispatchAllCollisionPairs")
            try {
                dispatcher.dispatchAllCollisionPairs(broadphase.overlappingPairCache, dispatchInfo, dispatcher)
            } finally {
                popProfile()
            }
        } finally {
            popProfile()
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
        val minAabb = Stack.newVec()
        val maxAabb = Stack.newVec()
        val tmp = Stack.newVec()
        val tmpTrans = Stack.newTrans()

        colObj.collisionShape!!.getBounds(colObj.getWorldTransform(tmpTrans), minAabb, maxAabb)
        // need to increase the aabb for contact thresholds
        val contactThreshold = Stack.newVec()
        contactThreshold.set(
            BulletGlobals.contactBreakingThreshold,
            BulletGlobals.contactBreakingThreshold,
            BulletGlobals.contactBreakingThreshold
        )
        minAabb.sub(contactThreshold)
        maxAabb.add(contactThreshold)

        // moving objects should be moderately sized, probably something wrong if not
        maxAabb.sub(minAabb, tmp)

        // TODO: optimize
        if (colObj.isStaticObject || (tmp.lengthSquared() < 1e12f)) {
            broadphase.setAabb(colObj.broadphaseHandle!!, minAabb, maxAabb, dispatcher)
        } else {
            // something went wrong, investigate
            // this assert is unwanted in 3D modelers (danger of loosing work)
            colObj.setActivationStateMaybe(ActivationState.DISABLE_SIMULATION)

            val debugDrawer = debugDrawer
            if (updateAabbsReportMe && debugDrawer != null) {
                updateAabbsReportMe = false
                debugDrawer.reportErrorWarning("Overflow in AABB, object removed from simulation")
                debugDrawer.reportErrorWarning("If you can reproduce this, please email bugs@continuousphysics.com\n")
                debugDrawer.reportErrorWarning("Please include above information, your Platform, version of OS.\n")
                debugDrawer.reportErrorWarning("Thanks.\n")
            }
        }
        Stack.subVec(4)
        Stack.subTrans(1)
    }

    fun updateAabbs() {
        pushProfile("updateAabbs")
        try {
            var stackPos: IntArray? = null
            for (i in collisionObjects.indices) {
                val colObj = collisionObjects[i]
                // only update aabb of active objects
                if (colObj.isActive) {
                    stackPos = Stack.getPosition(stackPos)
                    updateSingleAabb(colObj)
                    Stack.reset(stackPos)
                }
            }
        } finally {
            popProfile()
        }
    }

    private class BridgeTriangleConvexCastCallback(
        castShape: ConvexShape, from: Transform, to: Transform,
        var resultCallback: ConvexResultCallback,
        var collisionObject: CollisionObject?,
        triangleMesh: TriangleMeshShape, triangleToWorld: Transform
    ) : TriangleConvexCastCallback(
        castShape, from, to,
        triangleToWorld, triangleMesh.margin
    ) {

        var normalInWorldSpace: Boolean = false

        override fun reportHit(
            hitNormalLocal: Vector3d,
            hitPointLocal: Vector3d,
            hitFraction: Double,
            partId: Int,
            triangleIndex: Int
        ): Double {
            val shapeInfo = LocalShapeInfo()
            shapeInfo.shapePart = partId
            shapeInfo.triangleIndex = triangleIndex
            if (hitFraction <= resultCallback.closestHitFraction) {
                val convexResult =
                    LocalConvexResult(collisionObject, shapeInfo, hitNormalLocal, hitPointLocal, hitFraction)
                return resultCallback.addSingleResult(convexResult, normalInWorldSpace)
            }
            return hitFraction
        }
    }

    /**
     * rayTest performs a raycast on all objects in the CollisionWorld, and calls the resultCallback.
     * This allows for several queries: first hit, all hits, any hit, dependent on the value returned by the callback.
     */
    fun rayTest(rayFromWorld: Vector3d, rayToWorld: Vector3d, resultCallback: RayResultCallback) {
        val rayFromTrans = Stack.newTrans()
        val rayToTrans = Stack.newTrans()
        rayFromTrans.setIdentity()
        rayFromTrans.setTranslation(rayFromWorld)
        rayToTrans.setIdentity()

        rayToTrans.setTranslation(rayToWorld)

        // go over all objects, and if the ray intersects their aabb, do a ray-shape query using convexCaster (CCD)
        val collisionObjectAabbMin = Stack.newVec()
        val collisionObjectAabbMax = Stack.newVec()
        val hitLambda = Stack.newDoublePtr()
        val tmpTrans = Stack.newTrans()
        val hitNormal = Stack.newVec()

        for (i in collisionObjects.indices) {
            // terminate further ray tests, once the closestHitFraction reached zero
            if (resultCallback.closestHitFraction == 0.0) {
                break
            }

            val collisionObject = collisionObjects[i]
            // only perform raycast if filterMask matches
            if (resultCallback.needsCollision(collisionObject.broadphaseHandle!!)) {
                //RigidcollisionObject* collisionObject = ctrl->GetRigidcollisionObject();
                collisionObject.collisionShape!!
                    .getBounds(
                        collisionObject.getWorldTransform(tmpTrans),
                        collisionObjectAabbMin,
                        collisionObjectAabbMax
                    )

                hitLambda[0] = resultCallback.closestHitFraction
                if (AabbUtil.rayAabb(
                        rayFromWorld,
                        rayToWorld,
                        collisionObjectAabbMin,
                        collisionObjectAabbMax,
                        hitLambda,
                        hitNormal
                    )
                ) {
                    rayTestSingle(
                        rayFromTrans, rayToTrans,
                        collisionObject,
                        collisionObject.collisionShape!!,
                        collisionObject.getWorldTransform(tmpTrans),
                        resultCallback
                    )
                }
            }
        }

        Stack.subTrans(3)
        Stack.subVec(3)
        Stack.subDoublePtr(1)
    }

    /**
     * convexTest performs a swept convex cast on all objects in the [CollisionWorld], and calls the resultCallback
     * This allows for several queries: first hit, all hits, any hit, dependent on the value return by the callback.
     */
    fun convexSweepTest(
        selfShape: ConvexShape,
        convexFromWorld: Transform,
        convexToWorld: Transform,
        resultCallback: ConvexResultCallback
    ) {
        convexSweepTest(
            selfShape, convexFromWorld, convexToWorld,
            resultCallback, dispatchInfo.allowedCcdPenetration,
            collisionObjects
        )
    }

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
        hitNormalLocal: Vector3d,
        hitFraction: Double
    ) {
        val hitNormalLocal: Vector3d = Vector3d()
        var hitFraction: Double

        init {
            this.hitNormalLocal.set(hitNormalLocal)
            this.hitFraction = hitFraction
        }
    }

    /**
     * RayResultCallback is used to report new raycast results.
     */
    abstract class RayResultCallback {

        var closestHitFraction: Double = 1.0
        var collisionObject: CollisionObject? = null
        var collisionFilter = CollisionFilterGroups.DEFAULT_ALL

        fun hasHit(): Boolean {
            return (collisionObject != null)
        }

        fun needsCollision(proxy0: BroadphaseProxy): Boolean {
            return collides(proxy0.collisionFilter, collisionFilter)
        }

        abstract fun addSingleResult(rayResult: LocalRayResult, normalInWorldSpace: Boolean): Double
    }

    open class ClosestRayResultCallback(rayFromWorld: Vector3d, rayToWorld: Vector3d) : RayResultCallback() {
        val rayFromWorld: Vector3d = Vector3d() //used to calculate hitPointWorld from hitFraction
        val rayToWorld: Vector3d = Vector3d()

        @JvmField
        val hitNormalWorld: Vector3d = Vector3d()

        @JvmField
        val hitPointWorld: Vector3d = Vector3d()

        init {
            this.rayFromWorld.set(rayFromWorld)
            this.rayToWorld.set(rayToWorld)
        }

        override fun addSingleResult(rayResult: LocalRayResult, normalInWorldSpace: Boolean): Double {
            // caller already does the filter on the closestHitFraction
            assert(rayResult.hitFraction <= closestHitFraction)

            closestHitFraction = rayResult.hitFraction
            collisionObject = rayResult.collisionObject
            if (normalInWorldSpace) {
                hitNormalWorld.set(rayResult.hitNormalLocal)
            } else {
                // need to transform normal into worldspace
                hitNormalWorld.set(rayResult.hitNormalLocal)
                collisionObject!!.getWorldTransform(Stack.newTrans()).basis.transform(hitNormalWorld)
            }

            setInterpolate3(hitPointWorld, rayFromWorld, rayToWorld, rayResult.hitFraction)
            return rayResult.hitFraction
        }
    }

    class LocalConvexResult(
        val hitCollisionObject: CollisionObject?,
        val localShapeInfo: LocalShapeInfo?,
        hitNormalLocal: Vector3d,
        hitPointLocal: Vector3d,
        hitFraction: Double
    ) {
        val hitNormalLocal: Vector3d = Vector3d()
        val hitPointLocal: Vector3d = Vector3d()
        var hitFraction: Double

        init {
            this.hitNormalLocal.set(hitNormalLocal)
            this.hitPointLocal.set(hitPointLocal)
            this.hitFraction = hitFraction
        }
    }

    abstract class ConvexResultCallback {
        @JvmField
        var closestHitFraction: Double = 1.0

        @JvmField
        var collisionFilter = CollisionFilterGroups.DEFAULT_ALL

        fun init() {
            closestHitFraction = 1.0
            collisionFilter = CollisionFilterGroups.DEFAULT_ALL
        }

        fun hasHit(): Boolean {
            return closestHitFraction < 1.0
        }

        open fun needsCollision(proxy0: BroadphaseProxy): Boolean {
            return collides(proxy0.collisionFilter, collisionFilter)
        }

        abstract fun addSingleResult(convexResult: LocalConvexResult, normalInWorldSpace: Boolean): Double
    }

    open class ClosestConvexResultCallback : ConvexResultCallback() {
        @JvmField
        val convexFromWorld: Vector3d = Vector3d() // used to calculate hitPointWorld from hitFraction

        @JvmField
        val convexToWorld: Vector3d = Vector3d()
        val hitNormalWorld: Vector3d = Vector3d()
        val hitPointWorld: Vector3d = Vector3d()
        var hitCollisionObject: CollisionObject? = null

        fun init(convexFromWorld: Vector3d, convexToWorld: Vector3d) {
            super.init()
            this.convexFromWorld.set(convexFromWorld)
            this.convexToWorld.set(convexToWorld)
            this.hitCollisionObject = null
        }

        override fun addSingleResult(convexResult: LocalConvexResult, normalInWorldSpace: Boolean): Double {
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
                hitCollisionObject!!.getWorldTransform(Stack.newTrans()).basis.transform(hitNormalWorld)
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

        override fun reportHit(hitNormalLocal: Vector3d, hitFraction: Double, partId: Int, triangleIndex: Int): Double {
            val shapeInfo = LocalShapeInfo()
            shapeInfo.shapePart = partId
            shapeInfo.triangleIndex = triangleIndex

            val rayResult = LocalRayResult(collisionObject, shapeInfo, hitNormalLocal, hitFraction)

            val normalInWorldSpace = false
            return resultCallback.addSingleResult(rayResult, normalInWorldSpace)
        }
    }

    companion object {

        private var updateAabbsReportMe = true

        fun calculateTemporalBounds(
            selfShape: ConvexShape,
            convexFromWorld: Transform,
            convexToWorld: Transform,
            selfAabbMin: Vector3d,
            selfAabbMax: Vector3d
        ) {

            val linVel = Stack.newVec()
            val angVel = TransformUtil.calculateVelocity(convexFromWorld, convexToWorld, 1.0, linVel)
            val tmp = Stack.newTrans()
            tmp.setIdentity()
            tmp.basis.set(convexFromWorld.basis)
            selfShape.calculateTemporalAabb(tmp, linVel, angVel, 1.0, selfAabbMin, selfAabbMax)

            Stack.subVec(1)
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
            allowedPenetration: Double,
            otherObjects: List<CollisionObject>
        ) {
            val selfAabbMin = Stack.newVec()
            val selfAabbMax = Stack.newVec()

            // Compute AABB that encompasses angular movement
            calculateTemporalBounds(selfShape, convexFromWorld, convexToWorld, selfAabbMin, selfAabbMax)

            val otherAabbMin = Stack.newVec()
            val otherAabbMax = Stack.newVec()
            val hitLambda = Stack.newDoublePtr()

            // go over all objects, and if the ray intersects their aabb + cast shape aabb,
            // do a ray-shape query using convexCaster (CCD)
            val hitNormal = Stack.newVec()
            var stackPos: IntArray? = null
            for (i in otherObjects.indices) {
                val other = otherObjects[i]
                // only perform raycast if filterMask matches
                if (resultCallback.needsCollision(other.broadphaseHandle!!)) {
                    stackPos = Stack.getPosition(stackPos)

                    val otherShape = other.collisionShape!!
                    otherShape.getBounds(other.worldTransform, otherAabbMin, otherAabbMax)
                    AabbUtil.aabbExpand(otherAabbMin, otherAabbMax, selfAabbMin, selfAabbMax) // minkowski sum
                    hitLambda[0] = 1.0 // could use resultCallback.closestHitFraction, but needs testing
                    if (AabbUtil.rayAabb(
                            convexFromWorld.origin,
                            convexToWorld.origin,
                            otherAabbMin, otherAabbMax,
                            hitLambda, hitNormal
                        )
                    ) {
                        objectQuerySingle(
                            selfShape, convexFromWorld, convexToWorld,
                            other, otherShape,
                            other.worldTransform,
                            resultCallback, allowedPenetration
                        )
                    }
                    Stack.reset(stackPos)
                }
            }

            Stack.subDoublePtr(1)
            Stack.subVec(4)
        }

        @JvmStatic
        fun rayTestSingle(
            rayFromTrans: Transform, rayToTrans: Transform,
            collisionObject: CollisionObject,
            collisionShape: CollisionShape,
            colObjWorldTransform: Transform,
            resultCallback: RayResultCallback
        ) {
            val pointShape = SphereShape(0.0)
            pointShape.margin = 0.0

            if (collisionShape is ConvexShape) {
                val castResult = Stack.newCastResult()
                castResult.fraction = resultCallback.closestHitFraction

                val simplexSolver = Stack.newVSS()

                //#define USE_SUBSIMPLEX_CONVEX_CAST 1
                //#ifdef USE_SUBSIMPLEX_CONVEX_CAST
                val convexCaster = SubSimplexConvexCast(pointShape, collisionShape, simplexSolver)

                //#else
                //btGjkConvexCast	convexCaster(castShape,convexShape,&simplexSolver);
                //btContinuousConvexCollision convexCaster(castShape,convexShape,&simplexSolver,0);
                //#endif //#USE_SUBSIMPLEX_CONVEX_CAST
                if (convexCaster.calcTimeOfImpact(
                        rayFromTrans, rayToTrans,
                        colObjWorldTransform, colObjWorldTransform, castResult
                    )
                ) {
                    //add hit
                    if (castResult.normal.lengthSquared() > 0.0001f) {
                        if (castResult.fraction < resultCallback.closestHitFraction) {
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
                            resultCallback.addSingleResult(localRayResult, normalInWorldSpace)
                        }
                    }
                }

                Stack.subVSS(1)
                Stack.subCastResult(1)
            } else {
                if (collisionShape is ConcaveShape) {

                    val worldToCollisionObject = Stack.newTrans()
                    worldToCollisionObject.setInverse(colObjWorldTransform)

                    val rayFromLocal = Stack.newVec(rayFromTrans.origin)
                    worldToCollisionObject.transform(rayFromLocal)

                    val rayToLocal = Stack.newVec(rayToTrans.origin)
                    worldToCollisionObject.transform(rayToLocal)

                    val rcb = BridgeTriangleRaycastCallback(
                        rayFromLocal, rayToLocal,
                        resultCallback, collisionObject
                    )
                    rcb.hitFraction = resultCallback.closestHitFraction

                    if (collisionShape is BvhTriangleMeshShape) {
                        // optimized version for BvhTriangleMeshShape
                        collisionShape.performRaycast(rcb, rayFromLocal, rayToLocal)
                    } else {

                        val rayAabbMinLocal = Stack.newVec(rayFromLocal)
                        setMin(rayAabbMinLocal, rayToLocal)

                        val rayAabbMaxLocal = Stack.newVec(rayFromLocal)
                        setMax(rayAabbMaxLocal, rayToLocal)

                        collisionShape.processAllTriangles(rcb, rayAabbMinLocal, rayAabbMaxLocal)

                        Stack.subVec(2)
                    }

                    Stack.subVec(2)
                    Stack.subTrans(1)
                } else {
                    // todo: use AABB tree or other BVH acceleration structure!
                    if (collisionShape is CompoundShape) {
                        val childWorldTrans = Stack.newTrans()
                        val children = collisionShape.children
                        for (i in children.indices) {
                            val child = children[i]
                            val childCollisionShape = child.shape
                            childWorldTrans.setMul(colObjWorldTransform, child.transform)
                            // replace collision shape so that callback can determine the triangle
                            val saveCollisionShape = collisionObject.collisionShape
                            collisionObject.collisionShape = (childCollisionShape)
                            rayTestSingle(
                                rayFromTrans, rayToTrans,
                                collisionObject,
                                childCollisionShape,
                                childWorldTrans,
                                resultCallback
                            )
                            // restore
                            collisionObject.collisionShape = (saveCollisionShape)
                        }
                        Stack.subTrans(1)
                    }
                }
            }
        }

        /**
         * objectQuerySingle performs a collision detection query and calls the resultCallback. It is used internally by rayTest.
         */
        @JvmStatic
        fun objectQuerySingle(
            castShape: ConvexShape,
            convexFromTrans: Transform,
            convexToTrans: Transform,
            collisionObject: CollisionObject,
            collisionShape: CollisionShape,
            colObjWorldTransform: Transform,
            resultCallback: ConvexResultCallback,
            allowedPenetration: Double
        ) {
            if (collisionShape is ConvexShape) {
                val castResult = Stack.newCastResult()
                castResult.allowedPenetration = allowedPenetration
                castResult.fraction = 1.0 // ??

                // JAVA TODO: should be convexCaster1
                //ContinuousConvexCollision convexCaster1(castShape,convexShape,&simplexSolver,&gjkEpaPenetrationSolver);
                //btSubsimplexConvexCast convexCaster3(castShape,convexShape,&simplexSolver);
                val castPtr: ConvexCast = Stack.newGjkCC(castShape, collisionShape)
                if (castPtr.calcTimeOfImpact(
                        convexFromTrans,
                        convexToTrans,
                        colObjWorldTransform,
                        colObjWorldTransform,
                        castResult
                    )
                ) {
                    // add hit
                    if (castResult.normal.lengthSquared() > 0.0001f && castResult.fraction < resultCallback.closestHitFraction) {
                        castResult.normal.normalize()
                        val localConvexResult = LocalConvexResult(
                            collisionObject,
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
            } else if (collisionShape is TriangleMeshShape) {

                val worldToCollisionObject = Stack.newTrans()
                worldToCollisionObject.setInverse(colObjWorldTransform)

                val convexFromLocal = Stack.newVec()
                convexFromLocal.set(convexFromTrans.origin)
                worldToCollisionObject.transform(convexFromLocal)

                val convexToLocal = Stack.newVec()
                convexToLocal.set(convexToTrans.origin)
                worldToCollisionObject.transform(convexToLocal)

                // rotation of box in local mesh space = MeshRotation^-1 * ConvexToRotation
                val rotationXform = Stack.newTrans()
                val tmpMat = Stack.newMat()
                tmpMat.setMul(worldToCollisionObject.basis, convexToTrans.basis)
                rotationXform.set(tmpMat)

                val callback = BridgeTriangleConvexCastCallback(
                    castShape, convexFromTrans, convexToTrans, resultCallback,
                    collisionObject, collisionShape, colObjWorldTransform
                )
                callback.hitFraction = resultCallback.closestHitFraction

                val boxMinLocal = Stack.newVec()
                val boxMaxLocal = Stack.newVec()
                castShape.getBounds(rotationXform, boxMinLocal, boxMaxLocal)

                if (collisionShape is BvhTriangleMeshShape) {

                    callback.normalInWorldSpace = true

                    collisionShape.performConvexCast(callback, convexFromLocal, convexToLocal, boxMinLocal, boxMaxLocal)

                    Stack.subVec(4)
                    Stack.subTrans(2)
                    Stack.subMat(1)
                } else {

                    callback.normalInWorldSpace = false

                    val rayAabbMinLocal = Stack.newVec()
                    val rayAabbMaxLocal = Stack.newVec()

                    convexFromLocal.min(convexToLocal, rayAabbMinLocal).add(boxMinLocal)
                    convexFromLocal.max(convexToLocal, rayAabbMaxLocal).add(boxMaxLocal)

                    collisionShape.processAllTriangles(callback, rayAabbMinLocal, rayAabbMaxLocal)

                    Stack.subVec(6)
                    Stack.subTrans(2)
                    Stack.subMat(1)
                }
            } else {
                // todo: use AABB tree or other BVH acceleration structure!
                if (collisionShape is CompoundShape) {
                    val childWorldTrans = Stack.newTrans()
                    val children = collisionShape.children
                    for (i in children.indices) {
                        val child = children[i]
                        val childCollisionShape = child.shape
                        childWorldTrans.setMul(colObjWorldTransform, child.transform)
                        // replace collision shape so that callback can determine the triangle
                        val saveCollisionShape = collisionObject.collisionShape
                        collisionObject.collisionShape = childCollisionShape
                        objectQuerySingle(
                            castShape, convexFromTrans, convexToTrans,
                            collisionObject, childCollisionShape, childWorldTrans,
                            resultCallback, allowedPenetration
                        )
                        // restore
                        collisionObject.collisionShape = saveCollisionShape
                    }
                    Stack.subTrans(1)
                }
            }
        }
    }
}
