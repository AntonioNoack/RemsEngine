package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.LongLongCallback
import speiger.primitivecollections.callbacks.LongLongPredicate

/**
 * Long2LongOpenHashMap from https://github.com/Speiger/Primitive-Collections/,
 * Converted to Kotlin and trimmed down to my needs.
 * */
class LongToLongHashMap : LongToHashMap<LongArray> {

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
        base: LongToLongHashMap
    ) : super(base) {
        this.missingValue = missingValue
    }

    override fun createValues(size: Int): LongArray = LongArray(size)
    override fun fillNullValues(values: LongArray) {
        values.fill(0L)
    }

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

    operator fun set(key: Long, value: Long) {
        put(key, value)
    }

    inline fun getOrPut(key: Long, generateIfNull: () -> Long): Long {
        val slot = findSlot(key)
        if (slot >= 0) return values[slot]

        val newValue = generateIfNull()
        this[key] = newValue
        return newValue
    }

    fun getOrPut(key: Long, valueIfNull: Long): Long {
        val slot = findSlot(key)
        if (slot >= 0) return values[slot]

        insert(-slot - 1, key, valueIfNull)
        return valueIfNull
    }

    fun put(key: Long, value: Long): Long {
        val slot = findSlot(key)
        if (slot < 0) {
            insert(-slot - 1, key, value)
            return missingValue
        } else {
            val oldValue = values[slot]
            values[slot] = value
            return oldValue
        }
    }

    fun remove(key: Long): Long {
        val slot = findSlot(key)
        return if (slot < 0) missingValue else {
            val value = values[slot]
            if (removeIndex(slot)) value else missingValue
        }
    }

    operator fun get(key: Long): Long {
        val slot = findSlot(key)
        return if (slot < 0) missingValue else values[slot]
    }

    @InternalAPI
    fun insert(slot: Int, key: Long, value: Long) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        values[slot] = value
        size++
        growMaybe()
    }

    fun forEach(callback: LongLongCallback) {
        if (containsNull) callback.call(0L, values[nullIndex])
        for (i in nullIndex - 1 downTo 0) {
            val key = keys[i]
            if (key != 0L) callback.call(key, values[i])
        }
    }

    fun removeIf(predicate: LongLongPredicate): Int {
        return removeIfImpl { predicate.test(keys[it], values[it]) }
    }

    override fun clone() = LongToLongHashMap(missingValue, this)
}