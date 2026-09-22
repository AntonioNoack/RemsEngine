package speiger.primitivecollections

import me.anno.utils.InternalAPI
import me.anno.utils.assertions.assertEquals
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.HashUtil.getMaxFill
import speiger.primitivecollections.callbacks.IntCallback
import java.util.Random

/**
 * Base of Long2LongOpenHashMap from https://github.com/Speiger/Primitive-Collections/,
 * Converted to Kotlin and trimmed down to my needs.
 * */
abstract class IntToHashMap<AV> : BaseHashMap<IntArray, AV> {

    constructor(
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR,
    ) : super(minCapacity, loadFactor)

    constructor(
        loadFactor: Float,
        nullIndex: Int,
    ) : super(loadFactor, nullIndex)

    constructor(base: IntToHashMap<AV>) :
            super(base.loadFactor, base.nullIndex) {
        base.keys.copyInto(keys)
        copyOver(values, base.values)
        size = base.size
        containsNull = base.containsNull
    }

    override fun createKeys(size: Int): IntArray = IntArray(size)
    override fun fillNullKeys(keys: IntArray) = keys.fill(0)

    @InternalAPI
    fun findSlot(key: Int): Int {
        if (key == 0) {
            return if (containsNull) nullIndex else -(nullIndex + 1)
        } else {
            var pos = HashUtil.mix(key.hashCode()) and mask
            var current = keys[pos]
            if (current != 0) {
                if (current == key) {
                    return pos
                }

                while (true) {
                    pos = (pos + 1) and mask
                    current = keys[pos]
                    if (current == 0) {
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
            if (key != 0) {
                var dstIndex = HashUtil.mix(key.hashCode()) and newMask
                while (newKeys[dstIndex] != 0) {
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

    @InternalAPI
    override fun shiftKeys(removedSlot: Int) {
        var startPos = removedSlot
        while (true) {
            val last = startPos
            startPos = (startPos + 1) and mask

            var current: Int
            while (true) {
                current = keys[startPos]
                if (current == 0) {
                    keys[last] = 0
                    setNull(values, last)
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
        keys[slot] = 0
        setNull(values, slot)
    }

    override fun hasKey(slot: Int): Boolean {
        return keys[slot] != 0
    }

    fun containsKey(key: Int): Boolean {
        return findSlot(key) >= 0
    }

    fun forEachKey(callback: IntCallback) {
        if (containsNull) callback.call(0)
        for (i in 0 until nullIndex) {
            val key = keys[i]
            if (key != 0) callback.call(key)
        }
    }

    fun firstKey(ifEmpty: Int = -1): Int {
        if (containsNull) return 0
        for (i in 0 until nullIndex) {
            val key = keys[i]
            if (key != 0) return key
        }
        return ifEmpty
    }

    /**
     * Query a random key to avoid running into any O(n²) trouble.
     * */
    fun randomKey(random: Random, ifEmpty: Int): Int {
        val index0 = random.nextInt(nullIndex)
        for (i in index0 until nullIndex) {
            val key = keys[i]
            if (key != 0) return key
        }
        if (containsNull) return 0
        for (i in 0 until index0) {
            val key = keys[i]
            if (key != 0) return key
        }
        return ifEmpty
    }

    fun keysToHashSet(): IntHashSet {
        val dst = IntHashSet(0, loadFactor)
        dst.keys = keys.copyOf()
        copyBasePropertiesInto(dst)
        return dst
    }
}