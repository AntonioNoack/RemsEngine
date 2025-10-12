package speiger.primitivecollections

import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.LongCallback
import speiger.primitivecollections.callbacks.LongPredicate

/**
 * Space-efficient LongToHashMap without any values
 * */
class LongHashSet : LongToHashMap<Unit> {

    constructor(
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR
    ) : super(minCapacity, loadFactor)

    constructor(base: LongHashSet) : super(base)

    override fun createValues(size: Int) {}
    override fun fillNullValues(values: Unit) {}
    override fun copyOver(dstValues: Unit, dstIndex: Int, srcValues: Unit, srcIndex: Int) {}
    override fun copyOver(dstValues: Unit, srcValues: Unit) {}
    override fun setNull(dstValues: Unit, dstIndex: Int) {}

    fun add(key: Long): Boolean {
        val slot = findSlot(key)
        if (slot < 0) {
            insert(-slot - 1, key)
            return true
        } else return false
    }

    fun remove(key: Long): Boolean {
        val slot = findSlot(key)
        if (slot >= 0) removeIndex(slot)
        return slot >= 0
    }

    operator fun contains(key: Long): Boolean {
        return containsKey(key)
    }

    private fun insert(slot: Int, key: Long) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        size++
        growMaybe()
    }

    fun forEach(callback: LongCallback) =
        forEachKey(callback)

    fun removeIf(predicate: LongPredicate) =
        removeIfImpl { predicate.test(keys[it]) }

    override fun clone(): LongHashSet = LongHashSet(this)
}