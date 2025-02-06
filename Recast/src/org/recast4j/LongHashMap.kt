package org.recast4j


class LongHashMap<V>(private var capacity: Int = 16) {

    companion object {

        private const val loadFactor = 0.75f

        private val entryCache = ArrayList<Entry<Any?>>()
        private fun <V> createEntry(k: Long, v: V): Entry<V> = synchronized(entryCache) {
            @Suppress("UNCHECKED_CAST")
            val e = entryCache.removeLastOrNull() as? Entry<V>
            if (e != null) {
                e.key = k
                e.value = v
                e
            } else Entry(k, v)
        }

        private val bucketCache = ArrayList<ArrayList<Entry<Any?>>>()
        private fun <V> createBucket(): ArrayList<Entry<V>> = synchronized(bucketCache) {
            @Suppress("UNCHECKED_CAST")
            bucketCache.removeLastOrNull() as? ArrayList<Entry<V>> ?: ArrayList()
        }

    }

    private class Entry<V>(var key: Long, var value: V)

    private var buckets: Array<ArrayList<Entry<V>>?> = arrayOfNulls(capacity)
    private var size = 0
    private var maxSize = (loadFactor * capacity).toInt()
    private var mask = capacity - 1

    init {
        if (capacity < 8 || capacity.and(mask) != 0)
            throw IllegalArgumentException("Expected power-of-two capacity, >= 8")
    }

    private fun mixHash(value: Long): Int {
        var x = value
        x = ((x ushr 16) xor x) * 0x45d9f3b
        x = ((x ushr 16) xor x) * 0x45d9f3b
        x = ((x ushr 16) xor x) * 0x45d9f3b
        x = ((x ushr 16) xor x) * 0x45d9f3b
        x = ((x ushr 16) xor x)
        return x.toInt()
    }

    private fun getBucketIndex(key: Long): Int {
        return mixHash(key) and mask
    }

    private fun requestBucket(idx: Int): ArrayList<Entry<V>> {
        var bucket = buckets[idx]
        if (bucket == null) {
            bucket = createBucket()
            buckets[idx] = bucket
        }
        return bucket
    }

    private fun requestBucket(key: Long) = requestBucket(getBucketIndex(key))

    private fun putUnsafe(entry: Entry<V>) {
        requestBucket(entry.key).add(entry)
    }

    operator fun set(key: Long, value: V): V? {
        val bucket = requestBucket(key)
        for (i in bucket.indices) {
            val entry = bucket[i]
            if (entry.key == key) {
                val prev = entry.value
                entry.value = value
                return prev
            }
        }
        if (size > maxSize) doubleCapacity()
        bucket.add(createEntry(key, value))
        size++
        return null
    }

    private fun doubleCapacity() {
        // resize
        maxSize *= 2
        capacity *= 2
        mask = capacity - 1
        val oldBuckets = buckets
        buckets = arrayOfNulls(capacity)
        for (bucket in oldBuckets) {
            for (oldEntry in bucket ?: continue) {
                putUnsafe(oldEntry)
            }
            bucket.clear()
            synchronized(bucketCache) {
                @Suppress("UNCHECKED_CAST")
                bucketCache.add(bucket as ArrayList<Entry<Any?>>)
            }
        }
    }

    operator fun get(key: Long): V? {
        val idx = getBucketIndex(key)
        val bucket = buckets[idx] ?: return null
        for (i in bucket.indices) {
            val entry = bucket[i]
            if (entry.key == key) {
                return entry.value
            }
        }
        return null
    }

    fun getOrPut(key: Long, generator: () -> V): V {
        val bucket = requestBucket(key)
        for (i in bucket.indices) {
            val entry = bucket[i]
            if (entry.key == key) return entry.value
        }
        val value = generator()
        bucket.add(createEntry(key, value))
        return value
    }

    fun clear() {
        // keep entries and buckets in cache
        for (bucket in buckets) {
            bucket ?: continue
            @Suppress("UNCHECKED_CAST")
            bucket as ArrayList<Entry<Any?>>
            synchronized(entryCache) {
                for (entry in bucket) {
                    entry.key = 0L
                    entry.value = null
                }
                entryCache.addAll(bucket)
            }
            synchronized(bucketCache) {
                bucket.clear()
                bucketCache.add(bucket)
            }
        }
        // empty state
        buckets.fill(null)
        size = 0
    }

    fun size() = size

    val values
        get(): List<V> {
            if (size == 0) return emptyList()
            val result = ArrayList<V>(size)
            for (bucket in buckets) {
                if (bucket != null) {
                    for (i in bucket.indices) {
                        val entry = bucket[i]
                        result.add(entry.value)
                    }
                }
            }
            return result
        }

    fun forEachValue(run: (V) -> Unit) {
        for (bucket in buckets) {
            if (bucket != null) {
                for (i in bucket.indices) {
                    val entry = bucket[i]
                    run.invoke(entry.value)
                }
            }
        }
    }

}