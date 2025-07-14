package com.bulletphysics.collision.broadphase

/**
 * OverlappingPairCache provides an interface for overlapping pair management (add, remove, storage),
 * used by the [BroadphaseInterface] broadphases.
 *
 * @author jezek2
 */
interface OverlappingPairCache : OverlappingPairCallback {

    fun cleanProxyFromPairs(proxy: BroadphaseProxy, dispatcher: Dispatcher)

    var overlapFilterCallback: OverlapFilterCallback?

    fun processAllOverlappingPairs(callback: OverlapCallback, dispatcher: Dispatcher)

    fun processAllOverlappingPairs(callback: (BroadphasePair) -> Unit)

    fun findPair(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy): BroadphasePair?

    fun setInternalGhostPairCallback(ghostPairCallback: OverlappingPairCallback)
}
