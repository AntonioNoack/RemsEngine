package speiger.primitivecollections

import me.anno.utils.InternalAPI
import me.anno.utils.types.Booleans.toInt
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.LongObjectCallback
import speiger.primitivecollections.callbacks.LongObjectPredicate

/**
 * Long2ObjectOpenHashMap from https://github.com/Speiger/Primitive-Collections/,
 * Converted to Kotlin and trimmed down to my needs. Parts have been moved to LongToHashMap.
 *
 * This improves our smooth normal calculation from 54ms for 110k triangles down to 32ms (1.68x speedup).
 * */
@Suppress("UNCHECKED_CAST")
class LongToObjectHashMap<V> : LongToHashMap<Array<Any?>> {

    constructor(
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR,
    ) : super(minCapacity, loadFactor)

    constructor(base: LongToObjectHashMap<V>) : super(base)

    override fun createValues(size: Int): Array<Any?> {
        return arrayOfNulls(size)
    }

    override fun fillNullValues(values: Array<Any?>) {
        values.fill(null)
    }

    override fun copyOver(
        dstValues: Array<Any?>, dstIndex: Int,
        srcValues: Array<Any?>, srcIndex: Int,
    ) {
        dstValues[dstIndex] = srcValues[srcIndex]
    }

    override fun copyOver(dstValues: Array<Any?>, srcValues: Array<Any?>) {
        srcValues.copyInto(dstValues)
    }

    override fun setNull(dstValues: Array<Any?>, dstIndex: Int) {
        dstValues[dstIndex] = null
    }

    operator fun set(key: Long, value: V) {
        put(key, value)
    }

    inline fun getOrPut(key: Long, generateIfNull: () -> V): V {
        val slot = findSlot(key)
        if (slot >= 0) return values[slot] as V

        val newValue = generateIfNull()
        // don't remember slot, because map might have changed in-between
        put(key, newValue)
        return newValue
    }

    fun put(key: Long, value: V): V? {
        val slot = findSlot(key)
        if (slot < 0) {
            insert(-slot - 1, key, value)
            return null
        } else {
            val oldValue = values[slot]
            values[slot] = value
            return oldValue as? V
        }
    }

    fun remove(key: Long): V? {
        val slot = findSlot(key)
        return if (slot < 0) null else {
            val value = values[slot]
            if (removeIndex(slot)) value as? V else null
        }
    }

    operator fun get(key: Long): V? {
        val slot = findSlot(key)
        return if (slot < 0) null else values[slot] as? V
    }

    @InternalAPI
    fun insert(slot: Int, key: Long, value: V) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        values[slot] = value
        size++
        growMaybe()
    }

    fun forEach(callback: LongObjectCallback<V>) {
        if (containsNull) callback.call(0L, values[nullIndex] as V)

        for (i in nullIndex - 1 downTo 0) {
            val key = keys[i]
            if (key != 0L) callback.call(key, values[i] as V)
        }
    }

    fun addAll(source: LongToObjectHashMap<V>) {
        source.forEach(this::put)
    }

    fun removeIf(predicate: LongObjectPredicate<V>): Int {
        return removeIfImpl { predicate.test(keys[it], values[it] as V) }
    }

    fun count(predicate: LongObjectPredicate<V>): Int {
        var found = 0
        forEach { k, v -> found = predicate.test(k, v).toInt() }
        return found
    }

    fun any(predicate: LongObjectPredicate<V>): Boolean {
        var found = false
        forEach { k, v ->
            found = found || predicate.test(k, v)
        }
        return found
    }

    override fun clone(): LongToObjectHashMap<V> = LongToObjectHashMap(this)
}