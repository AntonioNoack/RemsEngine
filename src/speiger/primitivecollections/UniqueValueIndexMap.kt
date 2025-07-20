package speiger.primitivecollections

import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY

/**
 * Collects unique values, and gives each element an index
 * */
class UniqueValueIndexMap<K>(
    missingId: Int,
    minCapacity: Int = DEFAULT_MIN_CAPACITY,
    loadFactor: Float = DEFAULT_LOAD_FACTOR
) : PrimitiveCollection {

    private val content = ObjectToIntHashMap<K>(missingId, minCapacity, loadFactor)
    val values = ArrayList<K>()

    override val size: Int get() = content.size
    override val maxFill: Int get() = content.maxFill
    override fun clear() {
        content.clear()
        values.clear()
    }

    fun add(value: K): Int {
        val oldSize = content.size
        val id = content.getOrPut(value, oldSize)
        if (id == oldSize) {
            values.add(value)
        }
        return id
    }

    operator fun get(value: K): Int {
        return content[value]
    }
}