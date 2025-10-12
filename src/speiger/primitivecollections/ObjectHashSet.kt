package speiger.primitivecollections

import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY

/**
 * Space-efficient ObjectToHashMap without any values.
 * Advantage over java.util.HashSet: only uses linear array, no node objects, so fewer overall memory usage and number of allocations
 * */
class ObjectHashSet<K> : ObjectToHashMap<K, Unit> {

    constructor(
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR
    ) : super(minCapacity, loadFactor)

    constructor(base: ObjectHashSet<K>) : super(base)

    override fun createValues(size: Int) {}
    override fun fillNullValues(values: Unit) {}
    override fun copyOver(dstValues: Unit, dstIndex: Int, srcValues: Unit, srcIndex: Int) {}
    override fun copyOver(dstValues: Unit, srcValues: Unit) {}
    override fun setNull(dstValues: Unit, dstIndex: Int) {}

    fun add(key: K): Boolean {
        val slot = findSlot(key)
        if (slot < 0) {
            insert(-slot - 1, key)
            return true
        } else return false
    }

    fun remove(key: K): Boolean {
        val slot = findSlot(key)
        if (slot >= 0) removeIndex(slot)
        return slot >= 0
    }

    operator fun contains(key: K): Boolean {
        return containsKey(key)
    }

    private fun insert(slot: Int, key: K) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        size++
        growMaybe()
    }

    fun forEach(callback: (K) -> Unit) = forEachKey(callback)

    override fun clone(): ObjectHashSet<K> = ObjectHashSet(this)

    fun removeIf(predicate: (K) -> Boolean): Int {
        @Suppress("UNCHECKED_CAST")
        return removeIfImpl { predicate(keys[it] as K) }
    }
}