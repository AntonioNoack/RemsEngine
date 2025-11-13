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
        val manifoldPtr = manifoldPtr ?: return

        val convexObj = if (isSwapped) body1 else body0
        val planeObj = if (isSwapped) body0 else body1

        val convexShape = convexObj.collisionShape as ConvexShape
        val planeShape = planeObj.collisionShape as StaticPlaneShape

        val planeNormal = Stack.newVec3f().set(planeShape.planeNormal)
        val planeConstant = planeShape.planeConstant

        val planeInConvex = Stack.newTrans()
        convexObj.getWorldTransform(planeInConvex)
        planeInConvex.inverse()
        planeInConvex.mul(planeObj.worldTransform)

        val convexInPlaneTrans = Stack.newTrans()
        convexInPlaneTrans.setInverse(planeObj.worldTransform)
        convexInPlaneTrans.mul(convexObj.worldTransform)

        val tmp = Stack.newVec3f()
        planeNormal.negate(tmp)
        tmp.mul(planeInConvex.basis)

        val vtx = convexShape.localGetSupportingVertex(tmp, Stack.newVec3f())
        val vtxInPlane = Stack.newVec3f(vtx)
        convexInPlaneTrans.transformPosition(vtxInPlane)

        val distance = (planeNormal.dot(vtxInPlane) - planeConstant).toFloat()

        val vtxInPlaneProjected = Stack.newVec3f()
        planeNormal.mul(distance, tmp)
        vtxInPlane.sub(tmp, vtxInPlaneProjected)

        val vtxInPlaneWorld = Stack.newVec3f(vtxInPlaneProjected)
        planeObj.worldTransform.transformPosition(vtxInPlaneWorld)

        val hasCollision = distance < manifoldPtr.contactBreakingThreshold
        resultOut.persistentManifold = manifoldPtr
        if (hasCollision) {
            // report a contact. internally this will be kept persistent, and contact reduction is done
            val normalOnSurfaceB = Stack.newVec3f(planeNormal)
            normalOnSurfaceB.mul(planeObj.worldTransform.basis)

            val pOnB = Stack.newVec3d().set(vtxInPlaneWorld)
            resultOut.addContactPoint(normalOnSurfaceB, pOnB, distance)
            Stack.subVec3f(1)
            Stack.subVec3d(1)
        }
        if (ownsManifold) {
            if (manifoldPtr.numContacts != 0) {
                resultOut.refreshContactPoints()
            }
        }

        Stack.subTrans(2)
        Stack.subVec3f(6)
    }

    override fun calculateTimeOfImpact(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ): Float {
        // not yet
        return 1f
    }

    override fun getAllContactManifolds(dst: ArrayList<PersistentManifold>) {
        val manifoldPtr = manifoldPtr
        if (manifoldPtr != null && ownsManifold) {
            dst.add(manifoldPtr)
        }
    }

    /** ///////////////////////////////////////////////////////////////////////// */
    class CreateFunc : CollisionAlgorithmCreateFunc() {
        private val pool = ObjectPool.get(ConvexPlaneCollisionAlgorithm::class.java)

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
