package speiger.primitivecollections

import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.IntCallback
import speiger.primitivecollections.callbacks.IntPredicate

/**
 * Space-efficient IntToHashMap without any values
 * */
class IntHashSet : IntToHashMap<Unit> {

    constructor(
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR,
    ) : super(minCapacity, loadFactor)

    constructor(base: IntHashSet) : super(base)

    override fun createValues(size: Int) {}
    override fun fillNullValues(values: Unit) {}
    override fun copyOver(dstValues: Unit, dstIndex: Int, srcValues: Unit, srcIndex: Int) {}
    override fun copyOver(dstValues: Unit, srcValues: Unit) {}
    override fun setNull(dstValues: Unit, dstIndex: Int) {}

    fun add(key: Int): Boolean {
        val slot = findSlot(key)
        if (slot < 0) {
            insert(-slot - 1, key)
            return true
        } else return false
    }

    fun remove(key: Int): Boolean {
        val slot = findSlot(key)
        if (slot >= 0) removeIndex(slot)
        return slot >= 0
    }

    operator fun contains(key: Int): Boolean {
        return containsKey(key)
    }

    private fun insert(slot: Int, key: Int) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        size++
        growMaybe()
    }

    fun addAll(source: IntHashSet) {
        source.forEach(this::add)
    }

    fun forEach(callback: IntCallback) =
        forEachKey(callback)

    fun removeIf(predicate: IntPredicate) =
        removeIfImpl { predicate.test(keys[it]) }

    override fun clone(): IntHashSet = IntHashSet(this)
}