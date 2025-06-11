package com.bulletphysics.collision.broadphase

import com.bulletphysics.collision.narrowphase.PersistentManifold

/**
 * Construction information for collision algorithms.
 *
 * @author jezek2
 */
class CollisionAlgorithmConstructionInfo {
    @JvmField
	var dispatcher1: Dispatcher? = null
    @JvmField
	var manifold: PersistentManifold? = null
}
