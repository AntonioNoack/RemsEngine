package me.anno.utils.structures.maps


/**
 * a map that initializes the values only when needed;
 * if you know that you need all values from the start, use a HashMap instead!;
 * not 100% thread-safe
 * */
open class LazyMap<K, V>(
    val generator: (K) -> V,
    val nullsAreValid: Boolean = false,
    initialCapacity: Int = 16
) : Map<K, V> {

    constructor(generator: (K) -> V) :
            this(generator, false)

    constructor(generator: (K) -> V, initialCapacity: Int) :
            this(generator, false, initialCapacity)

    private val cache = HashMap<K, V>(initialCapacity)

    override fun containsKey(key: K) = true
    override fun containsValue(value: V) = false // not really supported

    override fun get(key: K): V {
        return synchronized(cache) {
            cache.getOrPut(key) { generator(key) }
        }
    }

    fun getOrNull(key: K): V? {
        return synchronized(cache) {
            cache[key]
        }
    }

    override val entries
        get() = if (nullsAreValid) {
            cache.entries
        } else {
            cache.entries
                .filter { it.value != null }
                .toSet()
        }

    override val keys: Set<K>
        get() = cache.keys

    override val size: Int
        get() = cache.size

    override val values: Collection<V>
        get() = cache.values

    override fun isEmpty() = cache.isEmpty()

    fun putAll(values: Map<K, V>): LazyMap<K, V> {
        synchronized(cache) {
            cache.putAll(values)
        }
        return this
    }

    fun clear() {
        cache.clear()
    }
}