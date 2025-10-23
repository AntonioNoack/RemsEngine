package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.CollisionAlgorithm
import com.bulletphysics.collision.broadphase.CollisionAlgorithmConstructionInfo
import com.bulletphysics.collision.broadphase.DispatcherInfo
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.narrowphase.SubSimplexConvexCast
import com.bulletphysics.collision.shapes.ConcaveShape
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.collision.shapes.TriangleCallback
import com.bulletphysics.collision.shapes.TriangleShape
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.setMax
import com.bulletphysics.linearmath.VectorUtil.setMin
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * ConvexConcaveCollisionAlgorithm supports collision between convex shapes
 * and (concave) trianges meshes.
 *
 * @author jezek2
 */
class ConvexConcaveCollisionAlgorithm : CollisionAlgorithm() {

    private var isSwapped = false
    private var triangleCallback: ConvexTriangleCallback? = null

    fun init(
        ci: CollisionAlgorithmConstructionInfo,
        body0: CollisionObject, body1: CollisionObject,
        isSwapped: Boolean
    ) {
        super.init(ci)
        this.isSwapped = isSwapped
        this.triangleCallback = ConvexTriangleCallback(dispatcher!!, body0, body1, isSwapped)
    }

    override fun destroy() {
        triangleCallback!!.destroy()
    }

    override fun processCollision(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ) {
        val convexBody = if (isSwapped) body1 else body0
        val triBody = if (isSwapped) body0 else body1

        val concaveShape = triBody.collisionShape as? ConcaveShape ?: return
        val callback = triangleCallback ?: return
        if (convexBody.collisionShape is ConvexShape) {

            val collisionMarginTriangle = concaveShape.margin

            resultOut.persistentManifold = callback.manifoldPtr
            callback.setTimeStepAndCounters(collisionMarginTriangle, dispatchInfo, resultOut)

            // Disable persistency. previously, some older algorithm calculated all contacts in one go, so you can clear it here.
            //m_dispatcher->clearManifold(m_btConvexTriangleCallback.m_manifoldPtr);
            callback.manifoldPtr.setBodies(convexBody, triBody)

            concaveShape.processAllTriangles(
                callback,
                callback.getAabbMin(Stack.newVec()),
                callback.getAabbMax(Stack.newVec())
            )

            resultOut.refreshContactPoints()
        }
    }

    override fun calculateTimeOfImpact(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ): Double {

        val convexBody = if (isSwapped) body1 else body0
        val concaveBody = if (isSwapped) body0 else body1

        // quick approximation using raycast, todo: hook up to the continuous collision detection (one of the btConvexCast)

        // only perform CCD above a certain threshold, this prevents blocking on the long run
        // because object in a blocked ccd state (hitfraction<1) get their linear velocity halved each frame...
        val squareMot0 = convexBody.interpolationWorldTransform.origin
            .distanceSquared(convexBody.worldTransform.origin)
        if (squareMot0 < convexBody.ccdSquareMotionThreshold) {
            return 1.0
        }

        //const btVector3& from = convexbody->m_worldTransform.getOrigin();
        //btVector3 to = convexbody->m_interpolationWorldTransform.getOrigin();
        //todo: only do if the motion exceeds the 'radius'
        val triInv = concaveBody.getWorldTransform(Stack.newTrans())
        triInv.inverse()

        val convexFromLocal = Stack.newTrans()
        convexFromLocal.setMul(triInv, convexBody.worldTransform)

        val convexToLocal = Stack.newTrans()
        convexToLocal.setMul(triInv, convexBody.interpolationWorldTransform)

        val concaveShape = concaveBody.collisionShape
        var result = 1.0
        if (concaveShape is ConcaveShape) {
            val rayAabbMin = Stack.newVec(convexFromLocal.origin)
            setMin(rayAabbMin, convexToLocal.origin)

            val rayAabbMax = Stack.newVec(convexFromLocal.origin)
            setMax(rayAabbMax, convexToLocal.origin)

            val ccdRadius0 = convexBody.ccdSweptSphereRadius
            rayAabbMin.sub(ccdRadius0)
            rayAabbMax.add(ccdRadius0)

            val curHitFraction = 1.0 // is this available?
            val raycastCallback = LocalTriangleSphereCastCallback(
                convexFromLocal, convexToLocal,
                convexBody.ccdSweptSphereRadius, curHitFraction
            )

            raycastCallback.hitFraction = convexBody.hitFraction

            concaveShape.processAllTriangles(raycastCallback, rayAabbMin, rayAabbMax)
            Stack.subVec(2) // rayAabbMin, rayAabbMax

            if (raycastCallback.hitFraction < convexBody.hitFraction) {
                convexBody.hitFraction = raycastCallback.hitFraction
                result = raycastCallback.hitFraction
            }
        }

        Stack.subTrans(3)
        return result
    }

    override fun getAllContactManifolds(dst: ArrayList<PersistentManifold>) {
        dst.add(triangleCallback!!.manifoldPtr)
    }

    /** ///////////////////////////////////////////////////////////////////////// */
    private class LocalTriangleSphereCastCallback(
        from: Transform,
        to: Transform,
        ccdSphereRadius: Double,
        hitFraction: Double
    ) : TriangleCallback {
        val ccdSphereFromTrans: Transform = Transform()
        val ccdSphereToTrans: Transform = Transform()

        var ccdSphereRadius: Double
        var hitFraction: Double

        private val identity = Transform()

        init {
            this.ccdSphereFromTrans.set(from)
            this.ccdSphereToTrans.set(to)
            this.ccdSphereRadius = ccdSphereRadius
            this.hitFraction = hitFraction

            // JAVA NOTE: moved here from processTriangle
            identity.setIdentity()
        }

        override fun processTriangle(triangle: Array<Vector3d>, partId: Int, triangleIndex: Int) {
            // do a swept sphere for now

            //btTransform ident;
            //ident.setIdentity();

            val castResult = Stack.newCastResult()
            castResult.fraction = hitFraction
            val pointShape = SphereShape(ccdSphereRadius)
            val triShape = TriangleShape(triangle[0], triangle[1], triangle[2])
            val simplexSolver = Stack.newVSS()
            val convexCaster = SubSimplexConvexCast(pointShape, triShape, simplexSolver)

            //GjkConvexCast	convexCaster(&pointShape,convexShape,&simplexSolver);
            //ContinuousConvexCollision convexCaster(&pointShape,convexShape,&simplexSolver,0);
            //local space?
            if (convexCaster.calcTimeOfImpact(ccdSphereFromTrans, ccdSphereToTrans, identity, identity, castResult)) {
                if (hitFraction > castResult.fraction) {
                    hitFraction = castResult.fraction
                }
            }

            Stack.subVSS(1)
            Stack.subCastResult(1)
        }
    }

    /** ///////////////////////////////////////////////////////////////////////// */
    class CreateFunc : CollisionAlgorithmCreateFunc() {
        private val pool = ObjectPool.Companion.get(ConvexConcaveCollisionAlgorithm::class.java)

        override fun createCollisionAlgorithm(
            ci: CollisionAlgorithmConstructionInfo,
            body0: CollisionObject,
            body1: CollisionObject
        ): CollisionAlgorithm {
            val algo = pool.get()
            algo.init(ci, body0, body1, swapped)
            return algo
        }

        override fun releaseCollisionAlgorithm(algo: CollisionAlgorithm) {
            pool.release(algo as ConvexConcaveCollisionAlgorithm)
        }
    }
}
