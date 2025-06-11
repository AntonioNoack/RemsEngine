package com.bulletphysics.collision.broadphase

/**
 * BroadphasePair class contains a pair of AABB-overlapping objects.
 * [Dispatcher] can search a [CollisionAlgorithm] that performs
 * exact/narrowphase collision detection on the actual collision shapes.
 *
 * @author jezek2
 */
class BroadphasePair {
    @JvmField
    var proxy0: BroadphaseProxy? = null

    @JvmField
    var proxy1: BroadphaseProxy? = null

    @JvmField
    var algorithm: CollisionAlgorithm? = null

    @JvmField
    var userInfo: Any? = null

    constructor()

    constructor(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy) {
        this.proxy0 = proxy0
        this.proxy1 = proxy1
        this.algorithm = null
        this.userInfo = null
    }

    fun set(p: BroadphasePair) {
        proxy0 = p.proxy0
        proxy1 = p.proxy1
        algorithm = p.algorithm
        userInfo = p.userInfo
    }

    fun equals(p: BroadphasePair): Boolean {
        return proxy0 === p.proxy0 && proxy1 === p.proxy1
    }

    companion object {
        @JvmField
        val broadphasePairSortPredicate: Comparator<BroadphasePair> =
            Comparator
                .comparingInt<BroadphasePair> { it.proxy0!!.uid }
                .thenComparingInt { it.proxy1!!.uid }
                .reversed()
    }
}
