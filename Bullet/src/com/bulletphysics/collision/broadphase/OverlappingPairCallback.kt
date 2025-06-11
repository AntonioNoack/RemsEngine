package com.bulletphysics.collision.broadphase

/**
 * OverlappingPairCallback is an additional optional broadphase user callback
 * for adding/removing overlapping pairs, similar interface to [OverlappingPairCache].
 *
 * @author jezek2
 */
interface OverlappingPairCallback {
    fun addOverlappingPair(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy): BroadphasePair?
    fun removeOverlappingPair(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy, dispatcher: Dispatcher): Any?
    fun removeOverlappingPairsContainingProxy(proxy0: BroadphaseProxy, dispatcher: Dispatcher)
}
