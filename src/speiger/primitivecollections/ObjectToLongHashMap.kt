package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.ObjectLongCallback
import speiger.primitivecollections.callbacks.ObjectLongPredicate

/**
 * Adjusted from LongToLongHashMap
 * */
class ObjectToLongHashMap<K> : ObjectToHashMap<K, LongArray> {

    val missingValue: Long

    constructor(
        missingValue: Long,
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR
    ) : super(minCapacity, loadFactor) {
        this.missingValue = missingValue
    }

    constructor(
        missingValue: Long,
        base: ObjectToLongHashMap<K>
    ) : super(base) {
        this.missingValue = missingValue
    }

    override fun createValues(size: Int): LongArray = LongArray(size)
    override fun fillNullValues(values: LongArray) = values.fill(0L)

    override fun copyOver(
        dstValues: LongArray, dstIndex: Int,
        srcValues: LongArray, srcIndex: Int
    ) {
        dstValues[dstIndex] = srcValues[srcIndex]
    }

    override fun copyOver(dstValues: LongArray, srcValues: LongArray) {
        srcValues.copyInto(dstValues)
    }

    override fun setNull(dstValues: LongArray, dstIndex: Int) {
        dstValues[dstIndex] = missingValue
    }

    operator fun set(key: K, value: Long) {
        put(key, value)
    }

    inline fun getOrPut(key: K, generateIfNull: () -> Long): Long {
        val slot = findIndex(key)
        if (slot >= 0) return values[slot]

        val newValue = generateIfNull()
        this[key] = newValue
        return newValue
    }

    fun getOrPut(key: K, valueIfNull: Long): Long {
        val slot = findIndex(key)
        if (slot >= 0) return values[slot]

        insert(-slot - 1, key, valueIfNull)
        return valueIfNull
    }

    fun put(key: K, value: Long): Long {
        val slot = findIndex(key)
        if (slot < 0) {
            insert(-slot - 1, key, value)
            return missingValue
        } else {
            val oldValue = values[slot]
            values[slot] = value
            return oldValue
        }
    }

    fun remove(key: K): Long {
        val slot = findIndex(key)
        return if (slot < 0) missingValue else {
            val value = values[slot]
            if (removeIndex(slot)) value else missingValue
        }
    }

    operator fun get(key: K): Long {
        val slot = findIndex(key)
        return if (slot < 0) missingValue else values[slot]
    }

    @InternalAPI
    fun insert(slot: Int, key: K, value: Long) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        values[slot] = value
        size++
        growMaybe()
    }

    fun forEach(callback: ObjectLongCallback<K>) {
        @Suppress("UNCHECKED_CAST")
        if (containsNull) callback.callback(null as K, values[nullIndex])
        for (i in nullIndex - 1 downTo 0) {
            val key = keys[i]
            @Suppress("UNCHECKED_CAST")
            if (key != null) callback.callback(key as K, values[i])
        }
    }

    fun removeIf(predicate: ObjectLongPredicate<K>): Int {
        @Suppress("UNCHECKED_CAST")
        return removeIfImpl { predicate.test(keys[it] as K, values[it]) }
    }

    override fun clone(): ObjectToLongHashMap<K> = ObjectToLongHashMap(missingValue, this)
}