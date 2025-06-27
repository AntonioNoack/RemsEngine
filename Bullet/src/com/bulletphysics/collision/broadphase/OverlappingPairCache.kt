package com.bulletphysics.collision.broadphase

import com.bulletphysics.util.ObjectArrayList

/**
 * OverlappingPairCache provides an interface for overlapping pair management (add,
 * remove, storage), used by the [BroadphaseInterface] broadphases.
 *
 * @author jezek2
 */
interface OverlappingPairCache : OverlappingPairCallback {

    val overlappingPairs: ObjectArrayList<BroadphasePair?>

    fun cleanOverlappingPair(pair: BroadphasePair, dispatcher: Dispatcher)

    val numOverlappingPairs: Int

    fun cleanProxyFromPairs(proxy: BroadphaseProxy, dispatcher: Dispatcher)

    var overlapFilterCallback: OverlapFilterCallback?

    fun processAllOverlappingPairs(callback: OverlapCallback, dispatcher: Dispatcher)

    fun findPair(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy): BroadphasePair?

    fun hasDeferredRemoval(): Boolean

    fun setInternalGhostPairCallback(ghostPairCallback: OverlappingPairCallback)
}
