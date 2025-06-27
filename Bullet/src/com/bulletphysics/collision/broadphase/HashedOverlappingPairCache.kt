package com.bulletphysics.collision.broadphase

import com.bulletphysics.BulletStats
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.collides
import com.bulletphysics.linearmath.MiscUtil.resize
import com.bulletphysics.util.IntArrayList
import com.bulletphysics.util.ObjectArrayList
import cz.advel.stack.Stack

/**
 * Hash-space based [OverlappingPairCache].
 *
 * @author jezek2
 */
class HashedOverlappingPairCache : OverlappingPairCache {

    // must be ObjectArrayList, so we can access capacity
    override val overlappingPairs = ObjectArrayList<BroadphasePair?>()

    override var overlapFilterCallback: OverlapFilterCallback? = null

    private val hashTable = IntArrayList()
    private val next = IntArrayList()
    var ghostPairCallback: OverlappingPairCallback? = null

    init {
        growTables()
    }

    /**
     * Add a pair and return the new pair. If the pair already exists,
     * no new pair is created and the old one is returned.
     */
    override fun addOverlappingPair(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy): BroadphasePair? {
        BulletStats.addedPairs++
        if (!needsBroadphaseCollision(proxy0, proxy1)) {
            return null
        }
        return internalAddPair(proxy0, proxy1)
    }

    override fun removeOverlappingPair(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy, dispatcher: Dispatcher): Any? {
        var proxy0 = proxy0
        var proxy1 = proxy1
        BulletStats.removedPairs++
        if (proxy0.uid > proxy1.uid) {
            val tmp = proxy0
            proxy0 = proxy1
            proxy1 = tmp
        }
        val proxyId1 = proxy0.uid
        val proxyId2 = proxy1.uid

        val hash = getHash(proxyId1, proxyId2) and (overlappingPairs.capacity() - 1)

        val pair = internalFindPair(proxy0, proxy1, hash)
        if (pair == null) {
            return null
        }

        cleanOverlappingPair(pair, dispatcher)

        val userData = pair.userInfo

        assert(pair.proxy0!!.uid == proxyId1)
        assert(pair.proxy1!!.uid == proxyId2)

        val pairIndex = overlappingPairs.indexOf(pair)
        assert(pairIndex != -1)

        assert(pairIndex < overlappingPairs.size)

        // Remove the pair from the hash table.
        var index = hashTable.get(hash)
        assert(index != NULL_PAIR)

        var previous: Int = NULL_PAIR
        while (index != pairIndex) {
            previous = index
            index = next.get(index)
        }

        if (previous != NULL_PAIR) {
            assert(next.get(previous) == pairIndex)
            next.set(previous, next.get(pairIndex))
        } else {
            hashTable.set(hash, next.get(pairIndex))
        }

        // We now move the last pair into spot of the
        // pair being removed. We need to fix the hash
        // table indices to support the move.
        val lastPairIndex = overlappingPairs.lastIndex

        if (ghostPairCallback != null) {
            ghostPairCallback!!.removeOverlappingPair(proxy0, proxy1, dispatcher)
        }

        // If the removed pair is the last pair, we are done.
        if (lastPairIndex == pairIndex) {
            overlappingPairs.removeQuick(overlappingPairs.lastIndex)
            return userData
        }

        // Remove the last pair from the hash table.
        val last = overlappingPairs.getQuick(lastPairIndex)!!
        /* missing swap here too, Nat. */
        val lastHash = getHash(last.proxy0!!.uid, last.proxy1!!.uid) and (overlappingPairs.capacity() - 1)

        index = hashTable.get(lastHash)
        assert(index != NULL_PAIR)

        previous = NULL_PAIR
        while (index != lastPairIndex) {
            previous = index
            index = next.get(index)
        }

        if (previous != NULL_PAIR) {
            assert(next.get(previous) == lastPairIndex)
            next.set(previous, next.get(lastPairIndex))
        } else {
            hashTable.set(lastHash, next.get(lastPairIndex))
        }

        // Copy the last pair into the remove pair's spot.
        overlappingPairs.getQuick(pairIndex)!!.set(overlappingPairs.getQuick(lastPairIndex)!!)

        // Insert the last pair into the hash table
        next.set(pairIndex, hashTable.get(lastHash))
        hashTable.set(lastHash, pairIndex)

        overlappingPairs.removeQuick(overlappingPairs.lastIndex)

        return userData
    }

    fun needsBroadphaseCollision(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy): Boolean {
        val filterCallback = overlapFilterCallback
        if (filterCallback != null) {
            return filterCallback.needBroadphaseCollision(proxy0, proxy1)
        }
        return collides(proxy0.collisionFilter, proxy1.collisionFilter)
    }

    override fun processAllOverlappingPairs(callback: OverlapCallback, dispatcher: Dispatcher) {
        var stackPos: IntArray? = null
        var numRemovedPairs = 0
        for (i in overlappingPairs.lastIndex downTo 0) {
            stackPos = Stack.getPosition(stackPos)
            val pair = overlappingPairs[i]!!
            if (callback.processOverlap(pair)) {
                removeOverlappingPair(pair.proxy0!!, pair.proxy1!!, dispatcher)
                numRemovedPairs++
            }
            Stack.reset(stackPos)
        }
        BulletStats.overlappingPairs -= numRemovedPairs
    }

    override fun removeOverlappingPairsContainingProxy(proxy0: BroadphaseProxy, dispatcher: Dispatcher) {
        processAllOverlappingPairs(RemovePairCallback(proxy0), dispatcher)
    }

    override fun cleanProxyFromPairs(proxy: BroadphaseProxy, dispatcher: Dispatcher) {
        processAllOverlappingPairs(CleanPairCallback(proxy, this, dispatcher), dispatcher)
    }

    override fun cleanOverlappingPair(pair: BroadphasePair, dispatcher: Dispatcher) {
        if (pair.algorithm != null) {
            //pair.algorithm.destroy();
            dispatcher.freeCollisionAlgorithm(pair.algorithm!!)
            pair.algorithm = null
        }
    }

    override fun findPair(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy): BroadphasePair? {
        var proxy0 = proxy0
        var proxy1 = proxy1
        BulletStats.findPairCalls++
        if (proxy0.uid > proxy1.uid) {
            val tmp = proxy0
            proxy0 = proxy1
            proxy1 = tmp // Antonio: fixed this... was a typo...
        }
        val proxyId1 = proxy0.uid
        val proxyId2 = proxy1.uid

        val hash = getHash(proxyId1, proxyId2) and (overlappingPairs.capacity() - 1)
        if (hash >= hashTable.size()) {
            return null
        }

        var index = hashTable.get(hash)
        while (index != NULL_PAIR && !equalsPair(overlappingPairs[index]!!, proxyId1, proxyId2)) {
            index = next.get(index)
        }

        if (index == NULL_PAIR) {
            return null
        }

        assert(index < overlappingPairs.size)
        return overlappingPairs[index]
    }

    override val numOverlappingPairs: Int
        get() = overlappingPairs.size

    override fun hasDeferredRemoval(): Boolean {
        return false
    }

    private fun internalAddPair(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy): BroadphasePair {
        var proxy0 = proxy0
        var proxy1 = proxy1
        if (proxy0.uid > proxy1.uid) {
            val tmp = proxy0
            proxy0 = proxy1
            proxy1 = tmp
        }
        val proxyId1 = proxy0.uid
        val proxyId2 = proxy1.uid

        var hash = getHash(proxyId1, proxyId2) and (overlappingPairs.capacity() - 1) // New hash value with new mask

        var pair = internalFindPair(proxy0, proxy1, hash)
        if (pair != null) {
            return pair
        }

        val count = overlappingPairs.size
        val oldCapacity = overlappingPairs.capacity()
        overlappingPairs.add(null)

        // this is where we add an actual pair, so also call the 'ghost'
        ghostPairCallback?.addOverlappingPair(proxy0, proxy1)

        val newCapacity = overlappingPairs.capacity()

        if (oldCapacity < newCapacity) {
            growTables()
            // hash with new capacity
            hash = getHash(proxyId1, proxyId2) and (overlappingPairs.capacity() - 1)
        }

        pair = BroadphasePair(proxy0, proxy1)
        pair.algorithm = null
        pair.userInfo = null

        overlappingPairs.setQuick(overlappingPairs.lastIndex, pair)

        next.set(count, hashTable.get(hash))
        hashTable.set(hash, count)

        return pair
    }

    private fun growTables() {
        val newCapacity = overlappingPairs.capacity()

        if (hashTable.size() < newCapacity) {
            // grow hashtable and next table
            val curHashtableSize = hashTable.size()

            resize(hashTable, newCapacity, 0)
            resize(next, newCapacity, 0)

            for (i in 0 until newCapacity) {
                hashTable.set(i, NULL_PAIR)
            }
            for (i in 0 until newCapacity) {
                next.set(i, NULL_PAIR)
            }

            for (i in 0 until curHashtableSize) {
                val pair = overlappingPairs.getQuick(i)!!
                val proxyId1 = pair.proxy0!!.uid
                val proxyId2 = pair.proxy1!!.uid
                // New hash value with new mask
                val hashValue = getHash(proxyId1, proxyId2) and (overlappingPairs.capacity() - 1)
                next.set(i, hashTable.get(hashValue))
                hashTable.set(hashValue, i)
            }
        }
    }

    private fun equalsPair(pair: BroadphasePair, proxyId1: Int, proxyId2: Int): Boolean {
        return pair.proxy0!!.uid == proxyId1 && pair.proxy1!!.uid == proxyId2
    }

    private fun getHash(proxyId1: Int, proxyId2: Int): Int {
        var key = (proxyId1) or (proxyId2 shl 16)

        // Thomas Wang's hash
        key += (key shl 15).inv()
        key = key xor (key ushr 10)
        key += (key shl 3)
        key = key xor (key ushr 6)
        key += (key shl 11).inv()
        key = key xor (key ushr 16)
        return key
    }

    private fun internalFindPair(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy, hash: Int): BroadphasePair? {
        val proxyId1 = proxy0.uid
        val proxyId2 = proxy1.uid

        //#if 0 // wrong, 'equalsPair' use unsorted uids, copy-past devil striked again. Nat.
        //if (proxyId1 > proxyId2)
        //	btSwap(proxyId1, proxyId2);
        //#endif
        var index = hashTable.get(hash)

        while (index != NULL_PAIR && !equalsPair(overlappingPairs.getQuick(index)!!, proxyId1, proxyId2)) {
            index = next.get(index)
        }

        if (index == NULL_PAIR) {
            return null
        }

        assert(index < overlappingPairs.size)

        return overlappingPairs.getQuick(index)
    }

    override fun setInternalGhostPairCallback(ghostPairCallback: OverlappingPairCallback) {
        this.ghostPairCallback = ghostPairCallback
    }

    /**///////////////////////////////////////////////////////////////////////// */
    private class RemovePairCallback(private val obsoleteProxy: BroadphaseProxy?) : OverlapCallback {
        override fun processOverlap(pair: BroadphasePair): Boolean {
            return pair.proxy0 === obsoleteProxy || pair.proxy1 === obsoleteProxy
        }
    }

    private class CleanPairCallback(
        private val cleanProxy: BroadphaseProxy?,
        private val pairCache: OverlappingPairCache,
        private val dispatcher: Dispatcher
    ) : OverlapCallback {
        override fun processOverlap(pair: BroadphasePair): Boolean {
            if (pair.proxy0 === cleanProxy || pair.proxy1 === cleanProxy) {
                pairCache.cleanOverlappingPair(pair, dispatcher)
            }
            return false
        }
    }

    companion object {
        private const val NULL_PAIR = -0x1
    }
}
