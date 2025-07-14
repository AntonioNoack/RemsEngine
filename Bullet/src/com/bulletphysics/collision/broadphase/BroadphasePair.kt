package com.bulletphysics.collision.broadphase

/**
 * BroadphasePair class contains a pair of AABB-overlapping objects.
 * [Dispatcher] can search a [CollisionAlgorithm] that performs
 * exact/narrowphase collision detection on the actual collision shapes.
 *
 * @author jezek2
 */
class BroadphasePair {

    lateinit var proxy0: BroadphaseProxy
    lateinit var proxy1: BroadphaseProxy

    @JvmField
    var algorithm: CollisionAlgorithm? = null

    fun set(p: BroadphasePair) {
        proxy0 = p.proxy0
        proxy1 = p.proxy1
        algorithm = p.algorithm
    }
}
