package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.CollisionAlgorithm
import com.bulletphysics.collision.broadphase.CollisionAlgorithmConstructionInfo
import com.bulletphysics.collision.broadphase.DispatcherInfo
import com.bulletphysics.collision.narrowphase.PersistentManifold

/**
 * Empty algorithm, used as fallback when no collision algorithm is found for given
 * shape type pair.
 *
 * @author jezek2
 */
class EmptyAlgorithm : CollisionAlgorithm() {

    override fun destroy() {
    }

    override fun processCollision(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ) {
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
    }

    /**///////////////////////////////////////////////////////////////////////// */
    class CreateFunc : CollisionAlgorithmCreateFunc() {
        override fun createCollisionAlgorithm(
            ci: CollisionAlgorithmConstructionInfo,
            body0: CollisionObject,
            body1: CollisionObject
        ): CollisionAlgorithm {
            return INSTANCE
        }

        override fun releaseCollisionAlgorithm(algo: CollisionAlgorithm) {
        }
    }

    companion object {
        private val INSTANCE = EmptyAlgorithm()
    }
}
