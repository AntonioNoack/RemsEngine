package me.anno.utils.structures.maps


/**
 * a map that initializes the values only when needed;
 * if you know that you need all values from the start, use a HashMap instead!;
 * not thread-safe
 * */
open class LazyMap<K, V>(
    val generator: (K) -> V?,
    val nullsAreValid: Boolean = false,
    initialCapacity: Int = 16
) : Map<K, V> {

    constructor(generator: (K) -> V?) :
            this(generator, false)

    constructor(generator: (K) -> V?, initialCapacity: Int) :
            this(generator, false, initialCapacity)

    val cache = HashMap<K, V?>(initialCapacity)

    override fun containsKey(key: K) = true

    override fun containsValue(value: V) = false

    override fun get(key: K): V? =
        cache.getOrPut(key) { generator(key) }

    @Suppress("unchecked_cast")
    override val entries
        get() = if (nullsAreValid) {
            cache.entries as Set<Map.Entry<K, V>>
        } else {
            cache.entries
                .filter { it.value != null }
                .toSet() as Set<Map.Entry<K, V>>
        }

    override val keys: Set<K>
        get() = cache.keys

    override val size: Int
        get() = cache.size

    override val values: Collection<V>
        get() = emptySet()

    override fun isEmpty() = cache.isEmpty()

    fun putAll(values: Map<K, V>): LazyMap<K, V> {
        cache.putAll(values)
        return this
    }

}