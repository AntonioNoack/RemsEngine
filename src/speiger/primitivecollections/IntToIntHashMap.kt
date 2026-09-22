package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.IntIntCallback
import speiger.primitivecollections.callbacks.IntIntPredicate

/**
 * Long2LongOpenHashMap from https://github.com/Speiger/Primitive-Collections/,
 * Converted to Kotlin and trimmed down to my needs.
 * */
class IntToIntHashMap : IntToHashMap<IntArray> {

    val missingValue: Int

    constructor(
        missingValue: Int,
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR,
    ) : super(minCapacity, loadFactor) {
        this.missingValue = missingValue
    }

    constructor(
        missingValue: Int,
        base: IntToIntHashMap,
    ) : super(base) {
        this.missingValue = missingValue
    }

    override fun createValues(size: Int): IntArray = IntArray(size)
    override fun fillNullValues(values: IntArray) {
        values.fill(0)
    }

    override fun copyOver(
        dstValues: IntArray, dstIndex: Int,
        srcValues: IntArray, srcIndex: Int,
    ) {
        dstValues[dstIndex] = srcValues[srcIndex]
    }

    override fun copyOver(dstValues: IntArray, srcValues: IntArray) {
        srcValues.copyInto(dstValues)
    }

    override fun setNull(dstValues: IntArray, dstIndex: Int) {
        dstValues[dstIndex] = missingValue
    }

    operator fun set(key: Int, value: Int) {
        put(key, value)
    }

    inline fun getOrPut(key: Int, generateIfNull: () -> Int): Int {
        val slot = findSlot(key)
        if (slot >= 0) return values[slot]

        val newValue = generateIfNull()
        this[key] = newValue
        return newValue
    }

    fun getOrPut(key: Int, valueIfNull: Int): Int {
        val slot = findSlot(key)
        if (slot >= 0) return values[slot]

        insert(-slot - 1, key, valueIfNull)
        return valueIfNull
    }

    fun put(key: Int, value: Int): Int {
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

    fun remove(key: Int): Int {
        val slot = findSlot(key)
        return if (slot < 0) missingValue else {
            val value = values[slot]
            if (removeIndex(slot)) value else missingValue
        }
    }

    operator fun get(key: Int): Int {
        val slot = findSlot(key)
        return if (slot < 0) missingValue else values[slot]
    }

    @InternalAPI
    fun insert(slot: Int, key: Int, value: Int) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        values[slot] = value
        size++
        growMaybe()
    }

    fun forEach(callback: IntIntCallback) {
        if (containsNull) callback.call(0, values[nullIndex])
        for (i in nullIndex - 1 downTo 0) {
            val key = keys[i]
            if (key != 0) callback.call(key, values[i])
        }
    }

    fun addAll(source: IntToIntHashMap) {
        source.forEach(this::put)
    }

    fun removeIf(predicate: IntIntPredicate): Int {
        return removeIfImpl { predicate.test(keys[it], values[it]) }
    }

    override fun clone() = IntToIntHashMap(missingValue, this)
}