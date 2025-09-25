package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY

/**
 * Adjusted from LongToLongHashMap
 * */
class ObjectToObjectHashMap<K, V> : ObjectToHashMap<K, Array<Any?>> {

    val missingValue: V

    constructor(
        missingValue: V,
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR
    ) : super(minCapacity, loadFactor) {
        this.missingValue = missingValue
    }

    constructor(
        missingValue: V,
        base: ObjectToObjectHashMap<K, V>
    ) : super(base) {
        this.missingValue = missingValue
    }

    override fun createValues(size: Int): Array<Any?> = arrayOfNulls(size)
    override fun fillNullValues(values: Array<Any?>) = values.fill(0L)

    override fun copyOver(
        dstValues: Array<Any?>, dstIndex: Int,
        srcValues: Array<Any?>, srcIndex: Int
    ) {
        dstValues[dstIndex] = srcValues[srcIndex]
    }

    override fun copyOver(dstValues: Array<Any?>, srcValues: Array<Any?>) {
        srcValues.copyInto(dstValues)
    }

    override fun setNull(dstValues: Array<Any?>, dstIndex: Int) {
        dstValues[dstIndex] = missingValue
    }

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    inline fun getOrPut(key: K, generateIfNull: () -> V): V {
        val slot = findIndex(key)
        @Suppress("UNCHECKED_CAST")
        if (slot >= 0) return values[slot] as V

        val newValue = generateIfNull()
        this[key] = newValue
        return newValue
    }

    fun getOrPut(key: K, valueIfNull: V): V {
        val slot = findIndex(key)
        @Suppress("UNCHECKED_CAST")
        if (slot >= 0) return values[slot] as V

        insert(-slot - 1, key, valueIfNull)
        return valueIfNull
    }

    fun put(key: K, value: V): V {
        val slot = findIndex(key)
        if (slot < 0) {
            insert(-slot - 1, key, value)
            return missingValue
        } else {
            @Suppress("UNCHECKED_CAST")
            val oldValue = values[slot] as V
            values[slot] = value
            return oldValue
        }
    }

    fun remove(key: K): V {
        val slot = findIndex(key)
        return if (slot < 0) missingValue else {
            @Suppress("UNCHECKED_CAST")
            val value = values[slot] as V
            if (removeIndex(slot)) value else missingValue
        }
    }

    operator fun get(key: K): V {
        val slot = findIndex(key)
        return if (slot < 0) missingValue
        else {
            @Suppress("UNCHECKED_CAST")
            values[slot] as V
        }
    }

    @InternalAPI
    fun insert(slot: Int, key: K, value: V) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        values[slot] = value
        size++
        growMaybe()
    }

    fun forEach(callback: (K, V) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        if (containsNull) callback(null as K, values[nullIndex] as V)
        for (i in nullIndex - 1 downTo 0) {
            val key = keys[i]
            @Suppress("UNCHECKED_CAST")
            if (key != null) callback(key as K, values[i] as V)
        }
    }

    fun removeIf(predicate: (K, V) -> Boolean): Int {
        @Suppress("UNCHECKED_CAST")
        return removeIfImpl { predicate(keys[it] as K, values[it] as V) }
    }

    override fun clone(): ObjectToObjectHashMap<K, V> = ObjectToObjectHashMap(missingValue, this)
}