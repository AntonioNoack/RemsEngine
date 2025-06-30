package speiger.primitivecollections

import speiger.primitivecollections.callbacks.LongCallback

/**
 * Space-efficient LongToHashMap without any values
 * */
class LongHashSet(minCapacity: Int = 16, loadFactor: Float = 0.75f) :
    LongToHashMap<Unit>(minCapacity, loadFactor) {

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
            if (nullIndex > minCapacity && size < maxFill / 4 && nullIndex > 16) {
                rehash(nullIndex / 2)
            }
        }
    }

    private fun removeNullIndex() {
        containsNull = false
        keys[nullIndex] = 0L
        --size
        if (nullIndex > minCapacity && size < maxFill / 4 && nullIndex > 16) {
            rehash(nullIndex / 2)
        }
    }

    private fun insert(slot: Int, key: Long) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        if (size++ >= maxFill) {
            rehash(HashUtil.arraySize(size + 1, loadFactor))
        }
    }

    fun forEach(callback: LongCallback) {
        forEachKey(callback)
    }
}