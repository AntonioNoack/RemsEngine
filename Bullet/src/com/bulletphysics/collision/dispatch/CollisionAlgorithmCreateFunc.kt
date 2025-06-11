package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.CollisionAlgorithm
import com.bulletphysics.collision.broadphase.CollisionAlgorithmConstructionInfo

/**
 * Used by the CollisionDispatcher to register and create instances for CollisionAlgorithm.
 *
 * @author jezek2
 */
abstract class CollisionAlgorithmCreateFunc {

    @JvmField
    var swapped: Boolean = false

    abstract fun createCollisionAlgorithm(
        ci: CollisionAlgorithmConstructionInfo,
        body0: CollisionObject,
        body1: CollisionObject
    ): CollisionAlgorithm

    // JAVA NOTE: added
    abstract fun releaseCollisionAlgorithm(algo: CollisionAlgorithm)
}
