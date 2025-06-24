package com.bulletphysics.collision.broadphase

import com.bulletphysics.collision.narrowphase.PersistentManifold

/**
 * Construction information for collision algorithms.
 *
 * @author jezek2
 */
class CollisionAlgorithmConstructionInfo {
	lateinit var dispatcher1: Dispatcher
    @JvmField
	var manifold: PersistentManifold? = null
}
