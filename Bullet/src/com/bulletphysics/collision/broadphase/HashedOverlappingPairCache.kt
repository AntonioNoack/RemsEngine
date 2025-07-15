package com.bulletphysics.collision.broadphase

import com.bulletphysics.BulletStats
import com.bulletphysics.util.ObjectPool
import me.anno.ecs.components.collider.CollisionFilters.collides
import me.anno.maths.Packing.pack64
import me.anno.utils.assertions.assertNotEquals
import speiger.primitivecollections.LongToObjectHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * Hash-space based [OverlappingPairCache].
 *
 * @author jezek2, Antonio Noack
 */
class HashedOverlappingPairCache : OverlappingPairCache {

    companion object {
        private val pairPool = ObjectPool(BroadphasePair::class.java)
    }

    private val pairs = LongToObjectHashMap<BroadphasePair>()
    override var overlapFilterCallback: OverlapFilterCallback? = null
    var ghostPairCallback: OverlappingPairCallback? = null

    /**
     * Add a pair and return the new pair. If the pair already exists,
     * no new pair is created and the old one is returned.
     */
    override fun addOverlappingPair(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy): BroadphasePair? {
        BulletStats.addedPairs++
        if (!needsBroadphaseCollision(proxy0, proxy1)) {
            return null
        }

        val hash = hash(proxy0, proxy1)
        val oldPair = pairs[hash]
        if (oldPair != null) return oldPair

        val pair = pairPool.get()
        val swap = proxy0.uid > proxy1.uid
        pair.proxy0 = if (swap) proxy1 else proxy0
        pair.proxy1 = if (swap) proxy0 else proxy1
        pairs[hash] = pair
        ghostPairCallback?.addOverlappingPair(proxy0, proxy1)
        return pair
    }

    private fun hash(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy): Long {
        assertNotEquals(proxy0.uid, proxy1.uid)
        val minId = min(proxy0.uid, proxy1.uid)
        val maxId = max(proxy0.uid, proxy1.uid)
        return pack64(maxId, minId)
    }

    override fun removeOverlappingPair(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy, dispatcher: Dispatcher): Any? {
        BulletStats.removedPairs++
        val pair = pairs.remove(hash(proxy0, proxy1)) ?: return null
        BulletStats.overlappingPairs--
        onRemove(pair, dispatcher)
        return null
    }

    fun needsBroadphaseCollision(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy): Boolean {
        val filterCallback = overlapFilterCallback
        if (filterCallback != null) {
            return filterCallback.needBroadphaseCollision(proxy0, proxy1)
        }
        return collides(proxy0.collisionFilter, proxy1.collisionFilter)
    }

    private fun onRemove(pair: BroadphasePair, dispatcher: Dispatcher) {
        val proxy0 = pair.proxy0
        val proxy1 = pair.proxy1
        ghostPairCallback?.removeOverlappingPair(proxy0, proxy1, dispatcher)
        cleanOverlappingPair(pair, dispatcher)
        pairPool.release(pair)
    }

    override fun processAllOverlappingPairs(callback: OverlapCallback, dispatcher: Dispatcher) {
        BulletStats.overlappingPairs -= pairs.removeIf { _, pair ->
            if (callback.processOverlap(pair)) {
                onRemove(pair, dispatcher)
                true
            } else false
        }
    }

    override fun processAllOverlappingPairs(callback: (BroadphasePair) -> Unit) {
        pairs.forEach { _, pair -> callback(pair) }
    }

    override fun removeOverlappingPairsContainingProxy(proxy0: BroadphaseProxy, dispatcher: Dispatcher) {
        val obsoleteProxy = proxy0
        pairs.removeIf { _, pair ->
            pair.proxy0 === obsoleteProxy || pair.proxy1 === obsoleteProxy
        }
    }

    override fun cleanProxyFromPairs(proxy: BroadphaseProxy, dispatcher: Dispatcher) {
        pairs.forEach { _, pair ->
            cleanOverlappingPair(pair, dispatcher)
        }
    }

    private fun cleanOverlappingPair(pair: BroadphasePair, dispatcher: Dispatcher) {
        val algorithm = pair.algorithm
        if (algorithm != null) {
            dispatcher.freeCollisionAlgorithm(algorithm)
            pair.algorithm = null
        }
    }

    override fun findPair(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy): BroadphasePair? {
        return pairs[hash(proxy0, proxy1)]
    }

    override fun setInternalGhostPairCallback(ghostPairCallback: OverlappingPairCallback) {
        this.ghostPairCallback = ghostPairCallback
    }
}
