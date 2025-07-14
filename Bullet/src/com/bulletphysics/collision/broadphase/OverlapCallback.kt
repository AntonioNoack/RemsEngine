package com.bulletphysics.collision.broadphase

/**
 * OverlapCallback is used when processing all overlapping pairs in broadphase.
 *
 * @author jezek2
 * @see OverlappingPairCache.processAllOverlappingPairs
 */
fun interface OverlapCallback {
    /**
     * return true for deletion of the pair
     */
    fun processOverlap(pair: BroadphasePair): Boolean
}
