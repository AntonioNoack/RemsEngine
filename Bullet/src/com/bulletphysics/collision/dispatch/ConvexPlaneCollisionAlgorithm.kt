package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.CollisionAlgorithm
import com.bulletphysics.collision.broadphase.CollisionAlgorithmConstructionInfo
import com.bulletphysics.collision.broadphase.DispatcherInfo
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.collision.shapes.StaticPlaneShape
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack

/**
 * ConvexPlaneCollisionAlgorithm provides convex/plane collision detection.
 *
 * @author jezek2
 */
class ConvexPlaneCollisionAlgorithm : CollisionAlgorithm() {
    private var ownsManifold = false
    private var manifoldPtr: PersistentManifold? = null
    private var isSwapped = false

    fun init(
        mf: PersistentManifold?,
        ci: CollisionAlgorithmConstructionInfo,
        col0: CollisionObject,
        col1: CollisionObject,
        isSwapped: Boolean
    ) {
        super.init(ci)
        this.ownsManifold = false
        this.manifoldPtr = mf
        this.isSwapped = isSwapped

        val convexObj = if (isSwapped) col1 else col0
        val planeObj = if (isSwapped) col0 else col1

        if (manifoldPtr == null && dispatcher.needsCollision(convexObj, planeObj)) {
            manifoldPtr = dispatcher.getNewManifold(convexObj, planeObj)
            ownsManifold = true
        }
    }

    override fun destroy() {
        if (ownsManifold) {
            val manifoldPtr = manifoldPtr
            if (manifoldPtr != null) {
                dispatcher.releaseManifold(manifoldPtr)
            }
            this.manifoldPtr = null
        }
    }

    override fun processCollision(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ) {
        val manifoldPtr = manifoldPtr
        if (manifoldPtr == null) {
            return
        }

        val tmpTrans = Stack.newTrans()

        val convexObj = if (isSwapped) body1 else body0
        val planeObj = if (isSwapped) body0 else body1

        val convexShape = convexObj.collisionShape as ConvexShape
        val planeShape = planeObj.collisionShape as StaticPlaneShape

        val planeNormal = Stack.newVec().set(planeShape.planeNormal)
        val planeConstant = planeShape.planeConstant

        val planeInConvex = Stack.newTrans()
        convexObj.getWorldTransform(planeInConvex)
        planeInConvex.inverse()
        planeInConvex.mul(planeObj.worldTransform)

        val convexInPlaneTrans = Stack.newTrans()
        convexInPlaneTrans.setInverse(planeObj.worldTransform)
        convexInPlaneTrans.mul(convexObj.worldTransform)

        val tmp = Stack.newVec()
        planeNormal.negate(tmp)
        planeInConvex.basis.transform(tmp)

        val vtx = convexShape.localGetSupportingVertex(tmp, Stack.newVec())
        val vtxInPlane = Stack.newVec(vtx)
        convexInPlaneTrans.transformPosition(vtxInPlane)

        val distance = (planeNormal.dot(vtxInPlane) - planeConstant)

        val vtxInPlaneProjected = Stack.newVec()
        planeNormal.mul(distance, tmp)
        vtxInPlane.sub(tmp, vtxInPlaneProjected)

        val vtxInPlaneWorld = Stack.newVec(vtxInPlaneProjected)
        planeObj.getWorldTransform(tmpTrans).transformPosition(vtxInPlaneWorld)

        val hasCollision = distance < manifoldPtr.contactBreakingThreshold
        resultOut.persistentManifold = manifoldPtr
        if (hasCollision) {
            // report a contact. internally this will be kept persistent, and contact reduction is done
            val normalOnSurfaceB = Stack.newVec(planeNormal)
            planeObj.getWorldTransform(tmpTrans).basis.transform(normalOnSurfaceB)

            val pOnB = Stack.newVec(vtxInPlaneWorld)
            resultOut.addContactPoint(normalOnSurfaceB, pOnB, distance)
            Stack.subVec(2)
        }
        if (ownsManifold) {
            if (manifoldPtr.numContacts != 0) {
                resultOut.refreshContactPoints()
            }
        }

        Stack.subTrans(3)
        Stack.subVec(6)
    }

    override fun calculateTimeOfImpact(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ): Double {
        // not yet
        return 1.0
    }

    override fun getAllContactManifolds(dst: ArrayList<PersistentManifold>) {
        val manifoldPtr = manifoldPtr
        if (manifoldPtr != null && ownsManifold) {
            dst.add(manifoldPtr)
        }
    }

    /** ///////////////////////////////////////////////////////////////////////// */
    class CreateFunc : CollisionAlgorithmCreateFunc() {
        private val pool = ObjectPool.Companion.get(ConvexPlaneCollisionAlgorithm::class.java)

        override fun createCollisionAlgorithm(
            ci: CollisionAlgorithmConstructionInfo,
            body0: CollisionObject,
            body1: CollisionObject
        ): CollisionAlgorithm {
            val algo = pool.get()
            algo.init(null, ci, body0, body1, swapped)
            return algo
        }

        override fun releaseCollisionAlgorithm(algo: CollisionAlgorithm) {
            pool.release(algo as ConvexPlaneCollisionAlgorithm)
        }
    }
}
