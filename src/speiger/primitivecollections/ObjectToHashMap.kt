package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Converted from LongToHashMap
 * */
abstract class ObjectToHashMap<K, AV>(
    minCapacity: Int = 16,
    loadFactor: Float = 0.75f
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
    var keys: Array<Any?>

    @InternalAPI
    var values: AV

    // null is used as an empty-cell-marker and may be a key in this set
    @InternalAPI
    var containsNull: Boolean = false

    // first size when being created
    @InternalAPI
    var minCapacity = 0

    /**
     * Where the key null is stored.
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
        keys = arrayOfNulls(nullIndex + 1)
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
            keys.fill(null)
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
            keys = arrayOfNulls(request + 1)
            values = createArray(request + 1)
            this.size = 0
            containsNull = false
        }
    }

    @InternalAPI
    fun findIndex(key: K): Int {
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

    fun rehash(newSize: Int) {

        val newMask = newSize - 1
        val newKeys = arrayOfNulls<Any?>(newSize + 1)
        val newValues = createArray(newSize + 1)

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

            var current: Any?
            while (true) {
                current = keys[startPos]
                if (current == null) {
                    keys[last] = null
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

    fun containsKey(key: K): Boolean {
        return findIndex(key) >= 0
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

    /*fun valuesArray(): AV {
         val dstValues = createArray(size)
         var dstIndex = 0
         if (containsNull) copyOver(dstValues, dstIndex++, values, nullIndex)
         for (srcIndex in nullIndex - 1 downTo 0) {
             val key = keys[srcIndex]
             if (key != null) copyOver(dstValues, dstIndex++, values, srcIndex)
         }
         return dstValues
     }*/

    fun keysToHashSet(): HashSet<K> {
        val justKeys = HashSet<K>(size)
        @Suppress("UNCHECKED_CAST")
        if (containsNull) justKeys.add(null as K)
        for (i in nullIndex - 1 downTo 0) {
            val key = keys[i]
            @Suppress("UNCHECKED_CAST")
            if (key != null) justKeys.add(key as K)
        }
        return justKeys
    }
}