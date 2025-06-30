package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.callbacks.LongCallback
import java.util.function.LongPredicate
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Base of Long2LongOpenHashMap from https://github.com/Speiger/Primitive-Collections/,
 * Converted to Kotlin and trimmed down to my needs.
 * */
abstract class LongToHashMap<AV>(
    minCapacity: Int = 16,
    loadFactor: Float = 0.75f
) {

    abstract fun createArray(size: Int): AV
    abstract fun fillNulls(values: AV)
    abstract fun copyOver(
        dstValues: AV, dstIndex: Int,
        srcValues: AV, srcIndex: Int
    )

    abstract fun setNull(
        dstValues: AV, dstIndex: Int
    )

    fun isEmpty() = size == 0
    fun isNotEmpty() = !isEmpty()

    @InternalAPI
    var keys: LongArray

    @InternalAPI
    var values: AV

    // 0 is used as an empty-cell-marker and may be a key in this set
    @InternalAPI
    var containsNull: Boolean = false

    @InternalAPI
    var minCapacity = 0

    @InternalAPI
    var nullIndex = 0

    @InternalAPI
    var maxFill = 0

    @InternalAPI
    var mask = 0

    var size = 0

    val loadFactor: Float

    init {
        check(minCapacity >= 0) { "Minimum Capacity is negative. This is not allowed" }
        if (loadFactor in 1e-3f..1f) {
            this.loadFactor = loadFactor
            nullIndex = HashUtil.arraySize(minCapacity, loadFactor)
            this.minCapacity = nullIndex
            mask = nullIndex - 1
            maxFill = min(ceil((nullIndex.toFloat() * loadFactor).toDouble()).toInt(), nullIndex - 1)
            keys = LongArray(nullIndex + 1)
            values = createArray(nullIndex + 1)
        } else {
            throw IllegalStateException("Load Factor is not between 0 and 1")
        }
    }


    fun clear() {
        if (size != 0) {
            size = 0
            containsNull = false
            keys.fill(0L)
            fillNulls(values)
        }
    }

    fun trim(size: Int): Boolean {
        val request = max(
            minCapacity,
            HashUtil.nextPowerOfTwo(ceil((size.toFloat() / loadFactor).toDouble()).toInt())
        )
        if (request < nullIndex && this.size < min(
                ceil((request.toFloat() * loadFactor).toDouble()).toInt(),
                request - 1
            )
        ) {
            try {
                rehash(request)
                return true
            } catch (_: OutOfMemoryError) {
                return false
            }
        } else {
            return false
        }
    }

    @Suppress("unused")
    fun clearAndTrim(size: Int) {
        val request = max(
            minCapacity,
            HashUtil.nextPowerOfTwo(ceil((size.toFloat() / loadFactor).toDouble()).toInt())
        )
        if (request >= nullIndex) {
            clear()
        } else {
            nullIndex = request
            mask = request - 1
            maxFill = min(ceil((nullIndex.toFloat() * loadFactor).toDouble()).toInt(), nullIndex - 1)
            keys = LongArray(request + 1)
            values = createArray(request + 1)
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

    fun rehash(newSize: Int) {
        val newMask = newSize - 1
        val newKeys = LongArray(newSize + 1)
        val newValues = createArray(newSize + 1)
        var i = nullIndex

        var j = size - (if (containsNull) 1 else 0)
        while (j-- != 0) {
            --i
            if (i < 0) {
                throw ConcurrentModificationException("Map was modified during rehash")
            }

            var pos = 0
            if (keys[i] != 0L) {
                pos = HashUtil.mix(keys[i].hashCode()) and newMask
                if (newKeys[pos] != 0L) {
                    do {
                        pos = (pos + 1) and newMask
                    } while (newKeys[pos] != 0L)
                }

                newKeys[pos] = keys[i]
            }
            copyOver(newValues, pos, values, i)
        }

        copyOver(newValues, newSize, values, nullIndex)
        nullIndex = newSize
        mask = newMask
        maxFill = min(ceil((nullIndex.toFloat() * loadFactor).toDouble()).toInt(), nullIndex - 1)
        keys = newKeys
        values = newValues
    }

    fun shiftKeys(startPos: Int) {
        var startPos = startPos
        while (true) {
            val last = startPos
            startPos = (startPos + 1) and mask

            var current: Long
            while (true) {
                if ((keys[startPos].also { current = it }) == 0L) {
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

    /*fun valuesArray(): AV {
         val dstValues = createArray(size)
         var dstIndex = 0
         if (containsNull) copyOver(dstValues, dstIndex++, values, nullIndex)
         for (srcIndex in nullIndex - 1 downTo 0) {
             val key = keys[srcIndex]
             if (key != 0L) copyOver(dstValues, dstIndex++, values, srcIndex)
         }
         return dstValues
     }*/

    fun keysToHashSet(): LongHashSet {
        val justKeys = LongHashSet(0, loadFactor)
        justKeys.keys = keys.copyOf()
        justKeys.mask = mask
        justKeys.containsNull = containsNull
        justKeys.minCapacity = minCapacity
        justKeys.nullIndex = nullIndex
        justKeys.maxFill = maxFill
        justKeys.mask = mask
        justKeys.size = size
        return justKeys
    }
}