package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.LongCallback
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Base of Long2LongOpenHashMap from https://github.com/Speiger/Primitive-Collections/,
 * Converted to Kotlin and trimmed down to my needs.
 * */
abstract class LongToHashMap<AV>(
    minCapacity: Int = DEFAULT_MIN_CAPACITY,
    loadFactor: Float = DEFAULT_LOAD_FACTOR
) : PrimitiveCollection {

    abstract fun createArray(size: Int): AV
    abstract fun fillNulls(values: AV)
    abstract fun copyOver(
        dstValues: AV, dstIndex: Int,
        srcValues: AV, srcIndex: Int
    )

    abstract fun setNull(
        dstValues: AV, dstIndex: Int
    )

    @InternalAPI
    var keys: LongArray

    @InternalAPI
    var values: AV

    // 0 is used as an empty-cell-marker and may be a key in this set
    @InternalAPI
    var containsNull: Boolean = false

    // first size when being created
    @InternalAPI
    var minCapacity = 0

    /**
     * Where the key 0L is stored.
     * It is stored at the very last index.
     * For size 16, an array of size 17 is created, and nullIndex = 16.
     * */
    @InternalAPI
    var nullIndex = 0

    @InternalAPI
    override var maxFill = 0

    @InternalAPI
    var mask = 0

    override var size = 0

    val loadFactor: Float

    init {
        val loadFactor = if (loadFactor in 0.1f..1f) loadFactor else DEFAULT_LOAD_FACTOR
        this.loadFactor = loadFactor
        nullIndex = HashUtil.arraySize(minCapacity, loadFactor)
        this.minCapacity = nullIndex
        mask = nullIndex - 1
        maxFill = min(sizeToCapacity(nullIndex), nullIndex - 1)
        keys = LongArray(nullIndex + 1)
        values = createArray(nullIndex + 1)
    }

    val minFill get() = maxFill shr 2

    fun shrinkMaybe() {
        if (nullIndex > minCapacity && size < minFill && nullIndex > 16) {
            rehash(nullIndex shr 1)
        }
    }

    fun growMaybe() {
        if (size >= maxFill) {
            rehash(HashUtil.arraySize(size + 1, loadFactor))
        }
    }

    private fun sizeToCapacity(size: Int): Int {
        return ceil(size.toDouble() * loadFactor).toInt()
    }

    private fun sizeToPower2Capacity(size: Int): Int {
        return HashUtil.nextPowerOfTwo(sizeToCapacity(size))
    }

    override fun clear() {
        if (size != 0) {
            size = 0
            containsNull = false
            keys.fill(0L)
            fillNulls(values)
        }
    }

    fun trim(size: Int): Boolean {
        val request = max(minCapacity, sizeToPower2Capacity(size))
        return if (request < nullIndex && this.size < min(sizeToCapacity(request), request - 1)) {
            try {
                rehash(request)
                true
            } catch (_: OutOfMemoryError) {
                false
            }
        } else false
    }

    @Suppress("unused")
    fun clearAndTrim(size: Int) {
        val request = max(minCapacity, sizeToPower2Capacity(size))
        if (request >= nullIndex) {
            clear()
        } else {
            nullIndex = request
            mask = request - 1
            maxFill = min(ceil(nullIndex.toDouble() * loadFactor).toInt(), nullIndex - 1)
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
        maxFill = min(sizeToCapacity(nullIndex), nullIndex - 1)
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