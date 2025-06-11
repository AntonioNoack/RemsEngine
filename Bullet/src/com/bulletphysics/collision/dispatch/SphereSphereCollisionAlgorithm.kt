package com.bulletphysics.collision.dispatch

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.CollisionAlgorithm
import com.bulletphysics.collision.broadphase.CollisionAlgorithmConstructionInfo
import com.bulletphysics.collision.broadphase.DispatcherInfo
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setSub

/**
 * Provides collision detection between two spheres.
 *
 * @author jezek2
 */
class SphereSphereCollisionAlgorithm : CollisionAlgorithm() {
    private var ownManifold = false
    private var manifoldPtr: PersistentManifold? = null

    fun init(
        mf: PersistentManifold?,
        ci: CollisionAlgorithmConstructionInfo,
        col0: CollisionObject,
        col1: CollisionObject
    ) {
        super.init(ci)
        manifoldPtr = mf

        if (manifoldPtr == null) {
            manifoldPtr = dispatcher!!.getNewManifold(col0, col1)
            ownManifold = true
        }
    }

    override fun destroy() {
        if (ownManifold) {
            if (manifoldPtr != null) {
                dispatcher!!.releaseManifold(manifoldPtr!!)
            }
            manifoldPtr = null
        }
    }

    override fun processCollision(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ) {
        if (manifoldPtr == null) {
            return
        }

        val tmpTrans1 = Stack.newTrans()
        val tmpTrans2 = Stack.newTrans()

        resultOut.persistentManifold = manifoldPtr!!

        val sphere0 = body0.collisionShape as SphereShape?
        val sphere1 = body1.collisionShape as SphereShape?

        val diff = Stack.newVec()
        diff.setSub(body0.getWorldTransform(tmpTrans1).origin, body1.getWorldTransform(tmpTrans2).origin)

        val len = diff.length()
        val radius0 = sphere0!!.radius
        val radius1 = sphere1!!.radius

        //#ifdef CLEAR_MANIFOLD
        //manifoldPtr.clearManifold(); // don't do this, it disables warmstarting
        //#endif

        // if distance positive, don't generate a new contact
        if (len > (radius0 + radius1)) {
            //#ifndef CLEAR_MANIFOLD
            resultOut.refreshContactPoints()
            //#endif //CLEAR_MANIFOLD
            return
        }
        // distance (negative means penetration)
        val dist = len - (radius0 + radius1)

        val normalOnSurfaceB = Stack.newVec()
        normalOnSurfaceB.set(1.0, 0.0, 0.0)
        if (len > BulletGlobals.FLT_EPSILON) {
            normalOnSurfaceB.setScale(1.0 / len, diff)
        }

        val tmp = Stack.newVec()

        // point on A (worldspace)
        val pos0 = Stack.newVec()
        tmp.setScale(radius0, normalOnSurfaceB)
        pos0.setSub(body0.getWorldTransform(tmpTrans1).origin, tmp)

        // point on B (worldspace)
        val pos1 = Stack.newVec()
        tmp.setScale(radius1, normalOnSurfaceB)
        pos1.setAdd(body1.getWorldTransform(tmpTrans2).origin, tmp)

        // report a contact. internally this will be kept persistent, and contact reduction is done
        resultOut.addContactPoint(normalOnSurfaceB, pos1, dist)

        //#ifndef CLEAR_MANIFOLD
        resultOut.refreshContactPoints()
        //#endif //CLEAR_MANIFOLD
    }

    override fun calculateTimeOfImpact(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ): Double {
        return 1.0
    }

    override fun getAllContactManifolds(manifoldArray: ArrayList<PersistentManifold>) {
        val manifoldPtr = manifoldPtr
        if (manifoldPtr != null && ownManifold) {
            manifoldArray.add(manifoldPtr)
        }
    }

    class CreateFunc : CollisionAlgorithmCreateFunc() {
        private val pool: ObjectPool<SphereSphereCollisionAlgorithm> =
            ObjectPool.get(SphereSphereCollisionAlgorithm::class.java)

        override fun createCollisionAlgorithm(
            ci: CollisionAlgorithmConstructionInfo,
            body0: CollisionObject,
            body1: CollisionObject
        ): CollisionAlgorithm {
            val algo = pool.get()
            algo.init(null, ci, body0, body1)
            return algo
        }

        override fun releaseCollisionAlgorithm(algo: CollisionAlgorithm) {
            pool.release(algo as SphereSphereCollisionAlgorithm)
        }
    }
}
