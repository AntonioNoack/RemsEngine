package speiger.primitivecollections

import me.anno.utils.InternalAPI
import me.anno.utils.assertions.assertEquals
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.HashUtil.getMaxFill
import speiger.primitivecollections.HashUtil.sizeToPower2Capacity
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Converted from LongToHashMap
 * */
abstract class ObjectToHashMap<K, AV> : BaseHashMap<Array<Any?>, AV> {

    constructor(
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR
    ) : super(minCapacity, loadFactor)

    constructor(
        loadFactor: Float,
        nullIndex: Int
    ) : super(loadFactor, nullIndex)

    constructor(base: ObjectToHashMap<K, AV>) :
            super(base.loadFactor, base.nullIndex) {
        base.keys.copyInto(keys)
        copyOver(values, base.values)
        size = base.size
        containsNull = base.containsNull
    }

    override fun createKeys(size: Int): Array<Any?> = arrayOfNulls(size + 1)
    override fun fillNullKeys(keys: Array<Any?>) = keys.fill(null)

    override fun clearAndTrim(size: Int) {
        val request = max(minCapacity, sizeToPower2Capacity(size, loadFactor))
        if (request >= nullIndex) {
            clear()
        } else {
            nullIndex = request
            mask = request - 1
            maxFill = min(ceil(nullIndex.toDouble() * loadFactor).toInt(), nullIndex - 1)
            keys = createKeys(request + 1)
            values = createValues(request + 1)
            this.size = 0
            containsNull = false
        }
    }

    @InternalAPI
    fun findSlot(key: K): Int {
        if (key == null) {
            return if (containsNull) nullIndex else -(nullIndex + 1)
        } else {
            var pos = HashUtil.mix(key.hashCode()) and mask
            var current = keys[pos]
            if (current != null) {
                if (current == key) {
                    return pos
                }

                while (true) {
                    pos = (pos + 1) and mask
                    current = keys[pos]
                    if (current == null) {
                        break
                    }

                    if (current == key) {
                        return pos
                    }
                }
            }

            return -(pos + 1)
        }
    }

    override fun rehash(newSize: Int) {

        val newMask = newSize - 1
        val newKeys = createKeys(newSize + 1)
        val newValues = createValues(newSize + 1)

        var numRemainingItems = size - (if (containsNull) 1 else 0)
        for (srcIndex in 0 until nullIndex) {
            val key = keys[srcIndex]
            if (key != null) {
                var dstIndex = HashUtil.mix(key.hashCode()) and newMask
                while (newKeys[dstIndex] != null) {
                    dstIndex = (dstIndex + 1) and newMask
                }

                newKeys[dstIndex] = key
                copyOver(newValues, dstIndex, values, srcIndex)
                numRemainingItems--
            }
        }
        assertEquals(0, numRemainingItems, "Map was modified during rehash")

        copyOver(newValues, newSize, values, nullIndex)
        nullIndex = newSize

        mask = newMask
        maxFill = getMaxFill(nullIndex, loadFactor)
        keys = newKeys
        values = newValues
    }

    override fun shiftKeys(removedSlot: Int) {
        var startPos = removedSlot
        while (true) {
            val last = startPos
            startPos = (startPos + 1) and mask

            var current: Any?
            while (true) {
                current = keys[startPos]
                if (current == null) {
                    setEntryNull(last)
                    return
                }

                val slot = HashUtil.mix(current.hashCode()) and mask
                if (last <= startPos) {
                    if (slot !in (last + 1)..startPos) {
                        break
                    }
                } else if (slot in (startPos + 1)..last) {
                    break
                }

                startPos = (startPos + 1) and mask
            }

            keys[last] = current
            copyOver(values, last, values, startPos)
        }
    }

    override fun setEntryNull(slot: Int) {
        keys[slot] = null
        setNull(values, slot)
    }

    override fun hasKey(slot: Int): Boolean {
        return keys[slot] != null
    }

    fun containsKey(key: K): Boolean {
        return findSlot(key) >= 0
    }

    fun forEachKey(callback: (K) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        if (containsNull) callback(null as K)
        for (i in nullIndex - 1 downTo 0) {
            val key = keys[i]
            @Suppress("UNCHECKED_CAST")
            if (key != null) callback(key as K)
        }
    }

    fun firstKey(): K? {
        for (i in nullIndex - 1 downTo 0) {
            val key = keys[i]
            @Suppress("UNCHECKED_CAST")
            if (key != null) return key as K?
        }
        // whether not found, or null-key is contained,
        // we can handle both the same way: return null
        return null
    }

    fun keysToHashSet(): ObjectHashSet<K> {
        val dst = ObjectHashSet<K>(0, loadFactor)
        dst.keys = keys.copyOf()
        copyBasePropertiesInto(dst)
        return dst
    }
}