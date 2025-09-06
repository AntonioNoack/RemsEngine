package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.HashUtil.getMaxFill
import speiger.primitivecollections.HashUtil.sizeToPower2Capacity
import speiger.primitivecollections.callbacks.LongCallback
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Base of Long2LongOpenHashMap from https://github.com/Speiger/Primitive-Collections/,
 * Converted to Kotlin and trimmed down to my needs.
 * */
abstract class LongToHashMap<AV> : BaseHashMap<LongArray, AV> {

    constructor(
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR
    ) : super(minCapacity, loadFactor)

    constructor(
        loadFactor: Float,
        nullIndex: Int
    ) : super(loadFactor, nullIndex)

    constructor(base: LongToHashMap<AV>) :
            super(base.loadFactor, base.nullIndex) {
        base.keys.copyInto(keys)
        copyOver(values, base.values)
        size = base.size
        containsNull = base.containsNull
    }

    override fun createKeys(size: Int): LongArray = LongArray(size)
    override fun fillNullKeys(keys: LongArray) = keys.fill(0L)

    override fun clearAndTrim(size: Int) {
        val request = max(minCapacity, sizeToPower2Capacity(size, loadFactor))
        if (request >= nullIndex) {
            clear()
        } else {
            nullIndex = request
            mask = request - 1
            maxFill = min(ceil(nullIndex.toDouble() * loadFactor).toInt(), nullIndex - 1)
            keys = LongArray(request + 1)
            values = createValues(request + 1)
            this.size = 0
            containsNull = false
        }
    }

    @InternalAPI
    fun findIndex(key: Long): Int {
        if (key == 0L) {
            return if (containsNull) nullIndex else -(nullIndex + 1)
        } else {
            var pos = HashUtil.mix(key.hashCode()) and mask
            var current = keys[pos]
            if (current != 0L) {
                if (current == key) {
                    return pos
                }

                while (true) {
                    pos = (pos + 1) and mask
                    current = keys[pos]
                    if (current == 0L) {
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
        val newKeys = LongArray(newSize + 1)
        val newValues = createValues(newSize + 1)

        var numRemainingItems = size - (if (containsNull) 1 else 0)
        for (srcIndex in 0 until nullIndex) {
            val key = keys[srcIndex]
            if (key != 0L) {
                var dstIndex = HashUtil.mix(key.hashCode()) and newMask
                while (newKeys[dstIndex] != 0L) {
                    dstIndex = (dstIndex + 1) and newMask
                }

                newKeys[dstIndex] = key
                copyOver(newValues, dstIndex, values, srcIndex)
                numRemainingItems--
            }
        }
        if (numRemainingItems != 0) {
            throw ConcurrentModificationException("Map was modified during rehash")
        }

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

            var current: Long
            while (true) {
                current = keys[startPos]
                if (current == 0L) {
                    keys[last] = 0L
                    setNull(values, last)
                    return
                }

                val slot = HashUtil.mix(current.hashCode()) and mask
                if (last <= startPos) {
                    if (last >= slot || slot > startPos) {
                        break
                    }
                } else if (last >= slot && slot > startPos) {
                    break
                }

                startPos = (startPos + 1) and mask
            }

            keys[last] = current
            copyOver(values, last, values, startPos)
        }
    }

    override fun setEntryNull(slot: Int) {
        keys[slot] = 0L
        setNull(values, slot)
    }

    override fun hasKey(slot: Int): Boolean {
        return keys[slot] != 0L
    }

    fun containsKey(key: Long): Boolean {
        return findIndex(key) >= 0
    }

    fun forEachKey(callback: LongCallback) {
        if (containsNull) callback.callback(0L)
        for (i in nullIndex - 1 downTo 0) {
            val key = keys[i]
            if (key != 0L) callback.callback(key)
        }
    }

    fun keysToHashSet(): LongHashSet {
        val dst = LongHashSet(0, loadFactor)
        dst.keys = keys.copyOf()
        copyBasePropertiesInto(dst)
        return dst
    }
}