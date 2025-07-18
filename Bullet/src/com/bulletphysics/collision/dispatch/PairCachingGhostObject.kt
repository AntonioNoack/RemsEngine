package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.BroadphaseProxy
import com.bulletphysics.collision.broadphase.Dispatcher
import com.bulletphysics.collision.broadphase.HashedOverlappingPairCache

/**
 * @author tomrbryn
 */
class PairCachingGhostObject : GhostObject() {

    val overlappingPairCache = HashedOverlappingPairCache()

    /**
     * This method is mainly for expert/internal use only.
     */
    override fun addOverlappingObjectInternal(otherProxy: BroadphaseProxy, thisProxy: BroadphaseProxy?) {
        val actualThisProxy = checkNotNull(thisProxy ?: broadphaseHandle)
        val otherObject = otherProxy.clientObject
        // if this linearSearch becomes too slow (too many overlapping objects) we should add a more appropriate data structure
        val index = overlappingPairs.indexOf(otherObject)
        if (index == -1) {
            overlappingPairs.add(otherObject)
            overlappingPairCache.addOverlappingPair(actualThisProxy, otherProxy)
        }
    }

    override fun removeOverlappingObjectInternal(
        otherProxy: BroadphaseProxy, dispatcher: Dispatcher,
        thisProxy: BroadphaseProxy?
    ) {
        val otherObject = otherProxy.clientObject
        val actualThisProxy = checkNotNull(thisProxy ?: broadphaseHandle)
        val index = overlappingPairs.indexOf(otherObject)
        if (index != -1) {
            overlappingPairs[index] = overlappingPairs[overlappingPairs.lastIndex]
            overlappingPairs.removeAt(overlappingPairs.lastIndex)
            overlappingPairCache.removeOverlappingPair(actualThisProxy, otherProxy, dispatcher)
        }
    }
}
