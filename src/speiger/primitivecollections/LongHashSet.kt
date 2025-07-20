package speiger.primitivecollections

import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.LongCallback

/**
 * Space-efficient LongToHashMap without any values
 * */
class LongHashSet(
    minCapacity: Int = DEFAULT_MIN_CAPACITY,
    loadFactor: Float = DEFAULT_LOAD_FACTOR
) : LongToHashMap<Unit>(minCapacity, loadFactor) {

    override fun createArray(size: Int) {
    }

    override fun fillNulls(values: Unit) {
    }

    override fun copyOver(
        dstValues: Unit, dstIndex: Int,
        srcValues: Unit, srcIndex: Int
    ) {
    }

    override fun setNull(dstValues: Unit, dstIndex: Int) {
    }

    fun add(key: Long): Boolean {
        val slot = findIndex(key)
        if (slot < 0) {
            insert(-slot - 1, key)
            return true
        } else return false
    }

    fun remove(key: Long): Boolean {
        val slot = findIndex(key)
        if (slot >= 0) removeIndex(slot)
        return slot >= 0
    }

    operator fun contains(key: Long): Boolean {
        return containsKey(key)
    }

    private fun removeIndex(pos: Int) {
        if (pos == nullIndex) {
            if (containsNull) removeNullIndex()
        } else {
            keys[pos] = 0L
            --size
            shiftKeys(pos)
            shrinkMaybe()
        }
    }

    private fun removeNullIndex() {
        containsNull = false
        keys[nullIndex] = 0L
        --size
        shrinkMaybe()
    }

    private fun insert(slot: Int, key: Long) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        size++
        growMaybe()
    }

    fun forEach(callback: LongCallback) {
        forEachKey(callback)
    }
}