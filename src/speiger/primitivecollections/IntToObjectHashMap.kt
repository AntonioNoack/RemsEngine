package speiger.primitivecollections

import me.anno.utils.InternalAPI
import me.anno.utils.types.Booleans.toInt
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.IntObjectCallback
import speiger.primitivecollections.callbacks.IntObjectPredicate

/**
 * Long2ObjectOpenHashMap from https://github.com/Speiger/Primitive-Collections/,
 * Converted to Kotlin and trimmed down to my needs. Parts have been moved to LongToHashMap.
 *
 * This improves our smooth normal calculation from 54ms for 110k triangles down to 32ms (1.68x speedup).
 * */
class IntToObjectHashMap<V> : IntToHashMap<Array<V?>> {

    constructor(
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR,
    ) : super(minCapacity, loadFactor)

    constructor(base: IntToObjectHashMap<V>) : super(base)

    override fun createValues(size: Int): Array<V?> {
        @Suppress("UNCHECKED_CAST")
        return arrayOfNulls<Any>(size) as Array<V?>
    }

    override fun fillNullValues(values: Array<V?>) {
        values.fill(null)
    }

    override fun copyOver(
        dstValues: Array<V?>, dstIndex: Int,
        srcValues: Array<V?>, srcIndex: Int,
    ) {
        dstValues[dstIndex] = srcValues[srcIndex]
    }

    override fun copyOver(dstValues: Array<V?>, srcValues: Array<V?>) {
        srcValues.copyInto(dstValues)
    }

    override fun setNull(dstValues: Array<V?>, dstIndex: Int) {
        dstValues[dstIndex] = null
    }

    operator fun set(key: Int, value: V) {
        put(key, value)
    }

    inline fun getOrPut(key: Int, generateIfNull: () -> V): V {
        val slot = findSlot(key)
        @Suppress("UNCHECKED_CAST")
        if (slot >= 0) return values[slot] as V

        val newValue = generateIfNull()
        // don't remember slot, because map might have changed in-between
        put(key, newValue)
        return newValue
    }

    fun put(key: Int, value: V): V? {
        val slot = findSlot(key)
        if (slot < 0) {
            insert(-slot - 1, key, value)
            return null
        } else {
            val oldValue = values[slot]
            values[slot] = value
            return oldValue
        }
    }

    fun remove(key: Int): V? {
        val slot = findSlot(key)
        return if (slot < 0) null else {
            val value = values[slot]
            if (removeIndex(slot)) value else null
        }
    }

    operator fun get(key: Int): V? {
        val slot = findSlot(key)
        return if (slot < 0) null else values[slot]
    }

    @InternalAPI
    fun insert(slot: Int, key: Int, value: V) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        values[slot] = value
        size++
        growMaybe()
    }

    fun forEach(callback: IntObjectCallback<V>) {
        @Suppress("UNCHECKED_CAST")
        if (containsNull) callback.call(0, values[nullIndex] as V)

        for (i in nullIndex - 1 downTo 0) {
            val key = keys[i]
            @Suppress("UNCHECKED_CAST")
            if (key != 0) callback.call(key, values[i] as V)
        }
    }

    fun addAll(source: IntToObjectHashMap<V>) {
        source.forEach(this::put)
    }

    fun removeIf(predicate: IntObjectPredicate<V>): Int {
        @Suppress("UNCHECKED_CAST")
        return removeIfImpl { predicate.test(keys[it], values[it] as V) }
    }

    fun count(predicate: IntObjectPredicate<V>): Int {
        var found = 0
        forEach { k, v -> found = predicate.test(k, v).toInt() }
        return found
    }

    fun any(predicate: IntObjectPredicate<V>): Boolean {
        var found = false
        forEach { k, v ->
            found = found || predicate.test(k, v)
        }
        return found
    }

    override fun clone(): IntToObjectHashMap<V> = IntToObjectHashMap(this)
}