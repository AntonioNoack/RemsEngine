package com.bulletphysics.collision.dispatch

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.CollisionAlgorithm
import com.bulletphysics.collision.broadphase.CollisionAlgorithmConstructionInfo
import com.bulletphysics.collision.broadphase.DispatcherInfo
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack
import me.anno.maths.Maths.sq
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Provides collision detection between two spheres.
 *
 * @author jezek2
 */
class SphereSphereCollisionAlgorithm : CollisionAlgorithm() {
    private var ownsManifold = false
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
            manifoldPtr = dispatcher.getNewManifold(col0, col1)
            ownsManifold = true
        }
    }

    override fun destroy() {
        if (ownsManifold) {
            if (manifoldPtr != null) {
                dispatcher.releaseManifold(manifoldPtr!!)
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

        val manifoldPtr = manifoldPtr ?: return
        resultOut.persistentManifold = manifoldPtr

        val sphere0 = body0.collisionShape as SphereShape
        val sphere1 = body1.collisionShape as SphereShape

        val diff = Stack.newVec3d()
        body0.worldTransform.origin
            .sub(body1.worldTransform.origin, diff)

        val lenSq = diff.lengthSquared()
        val radius0 = sphere0.radius
        val radius1 = sphere1.radius

        // if distance positive, don't generate a new contact
        if (lenSq > sq(radius0 + radius1)) {
            resultOut.refreshContactPoints()
            Stack.subVec3d(1)
            return
        }

        val len = sqrt(max(lenSq, 0.0))

        // distance (negative means penetration)
        val dist = (len - (radius0 + radius1)).toFloat()

        val normalOnSurfaceB = Stack.newVec3f()
        normalOnSurfaceB.set(1.0, 0.0, 0.0)
        if (len > BulletGlobals.FLT_EPSILON) {
            normalOnSurfaceB.set(diff).mul(1f / len.toFloat())
        }

        // point on A (world space)
        val pos0 = Stack.newVec3d()
            .set(normalOnSurfaceB)
            .mul(radius0.toDouble())
        body0.worldTransform.origin.sub(pos0, pos0)

        // point on B (world space)
        val pos1 = Stack.newVec3d()
            .set(normalOnSurfaceB)
            .mul(radius1.toDouble())
        body1.worldTransform.origin.add(pos1, pos1)

        // report a contact. internally this will be kept persistent, and contact reduction is done
        resultOut.addContactPoint(normalOnSurfaceB, pos1, dist)
        resultOut.refreshContactPoints()
        Stack.subVec3f(1)
        Stack.subVec3d(3)
    }

    override fun calculateTimeOfImpact(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ): Float = 1f

    override fun getAllContactManifolds(dst: ArrayList<PersistentManifold>) {
        val manifoldPtr = manifoldPtr
        if (manifoldPtr != null && ownsManifold) {
            dst.add(manifoldPtr)
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
