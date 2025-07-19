package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.callbacks.LongLongCallback

/**
 * Long2LongOpenHashMap from https://github.com/Speiger/Primitive-Collections/,
 * Converted to Kotlin and trimmed down to my needs.
 * */
class LongToLongHashMap(
    val missingValue: Long,
    minCapacity: Int = 16,
    loadFactor: Float = 0.75f
) : LongToHashMap<LongArray>(minCapacity, loadFactor) {

    override fun createArray(size: Int): LongArray = LongArray(size)
    override fun fillNulls(values: LongArray) {
        values.fill(0L)
    }

    override fun copyOver(
        dstValues: LongArray, dstIndex: Int,
        srcValues: LongArray, srcIndex: Int
    ) {
        dstValues[dstIndex] = srcValues[srcIndex]
    }

    override fun setNull(dstValues: LongArray, dstIndex: Int) {
        dstValues[dstIndex] = missingValue
    }

    operator fun set(key: Long, value: Long) {
        put(key, value)
    }

    inline fun getOrPut(key: Long, generateIfNull: () -> Long): Long {
        val slot = findIndex(key)
        if (slot >= 0) return values[slot]

        val newValue = generateIfNull()
        this[key] = newValue
        return newValue
    }

    fun getOrPut(key: Long, valueIfNull: Long): Long {
        val slot = findIndex(key)
        if (slot >= 0) return values[slot]

        insert(-slot - 1, key, valueIfNull)
        return valueIfNull
    }

    fun put(key: Long, value: Long): Long {
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

    fun remove(key: Long): Long {
        val slot = findIndex(key)
        return if (slot < 0) missingValue else removeIndex(slot)
    }

    fun remove(key: Long, value: Long): Boolean {
        if (key == 0L) {
            if (containsNull && value == values[nullIndex]) {
                removeNullIndex()
                return true
            } else {
                return false
            }
        } else {
            var pos = HashUtil.mix(key.hashCode()) and mask
            var current = keys[pos]
            if (current == 0L) {
                return false
            } else if (current == key && value == values[pos]) {
                removeIndex(pos)
                return true
            } else {
                do {
                    pos = (pos + 1) and mask
                    current = keys[pos]
                    if (current == 0L) {
                        return false
                    }
                } while (current != key || value != values[pos])

                removeIndex(pos)
                return true
            }
        }
    }

    operator fun get(key: Long): Long {
        val slot = findIndex(key)
        return if (slot < 0) missingValue else values[slot]
    }

    fun replace(key: Long, oldValue: Long, newValue: Long): Boolean {
        val index = findIndex(key)
        if (index >= 0 && values[index] == oldValue) {
            values[index] = newValue
            return true
        } else return false
    }

    fun replace(key: Long, value: Long): Long {
        val index = findIndex(key)
        if (index < 0) {
            return missingValue
        } else {
            val oldValue = values[index]
            values[index] = value
            return oldValue
        }
    }

    private fun removeIndex(pos: Int): Long {
        if (pos == nullIndex) {
            return if (containsNull) removeNullIndex() else missingValue
        } else {
            val value = values[pos]
            keys[pos] = 0L
            values[pos] = missingValue
            --size
            shiftKeys(pos)
            shrinkMaybe()
            return value
        }
    }

    private fun removeNullIndex(): Long {
        val value = values[nullIndex]
        containsNull = false
        keys[nullIndex] = 0L
        values[nullIndex] = missingValue
        --size
        shrinkMaybe()
        return value
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
        if (containsNull) callback.callback(0L, values[nullIndex])
        for (i in nullIndex - 1 downTo 0) {
            val key = keys[i]
            if (key != 0L) callback.callback(key, values[i])
        }
    }
}