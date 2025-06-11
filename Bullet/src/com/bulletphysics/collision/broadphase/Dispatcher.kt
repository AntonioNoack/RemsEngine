package com.bulletphysics.collision.broadphase

import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.narrowphase.PersistentManifold

/**
 * Dispatcher can be used in combination with broadphase to dispatch
 * calculations for overlapping pairs. For example for pairwise collision detection,
 * calculating contact points stored in [PersistentManifold] or user callbacks
 * (game logic).
 *
 * @author jezek2
 */
interface Dispatcher {

    fun findAlgorithm(body0: CollisionObject, body1: CollisionObject): CollisionAlgorithm? {
        return findAlgorithm(body0, body1, null)
    }

    fun findAlgorithm(
        body0: CollisionObject,
        body1: CollisionObject,
        sharedManifold: PersistentManifold?
    ): CollisionAlgorithm?

    fun getNewManifold(body0: Any, body1: Any): PersistentManifold

    fun releaseManifold(manifold: PersistentManifold)

    fun clearManifold(manifold: PersistentManifold)

    fun needsCollision(body0: CollisionObject, body1: CollisionObject): Boolean

    fun needsResponse(body0: CollisionObject, body1: CollisionObject): Boolean

    fun dispatchAllCollisionPairs(
        pairCache: OverlappingPairCache,
        dispatchInfo: DispatcherInfo,
        dispatcher: Dispatcher
    )

	val numManifolds: Int

    fun getManifoldByIndexInternal(index: Int): PersistentManifold

    fun freeCollisionAlgorithm(algo: CollisionAlgorithm)
}
