package speiger.primitivecollections

import java.util.Arrays
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Long2ObjectOpenHashMap from https://github.com/Speiger/Primitive-Collections/,
 * Converted to Kotlin and trimmed down to my needs.
 *
 * This improves our smooth normal calculation from 54ms for 110k triangles down to 32ms (1.68x speedup).
 * */
class LongToObjectHashMap<V>(minCapacity: Int = 16, loadFactor: Float = 0.75f) {

    private var keys: LongArray
    private var values: Array<V?>

    // 0 is used as an empty-cell-marker and may be a key in this set
    private var containsNull: Boolean = false
    private var minCapacity = 0
    private var nullIndex = 0
    private var maxFill = 0
    private var mask = 0

    private var size = 0
    private val loadFactor: Float

    init {
        check(minCapacity >= 0) { "Minimum Capacity is negative. This is not allowed" }
        if (loadFactor in 1e-3f..1f) {
            this.loadFactor = loadFactor
            nullIndex = HashUtil.arraySize(minCapacity, loadFactor)
            this.minCapacity = nullIndex
            mask = nullIndex - 1
            maxFill = min(ceil((nullIndex.toFloat() * loadFactor).toDouble()).toInt(), nullIndex - 1)
            keys = LongArray(nullIndex + 1)
            @Suppress("UNCHECKED_CAST")
            values = (arrayOfNulls<Any>(nullIndex + 1)) as Array<V?>
        } else {
            throw IllegalStateException("Load Factor is not between 0 and 1")
        }
    }

    operator fun set(key: Long, value: V?) {
        put(key, value)
    }

    inline fun getOrPut(hash: Long, generateIfNull: () -> V): V {
        val value = this[hash]
        if (value != null) return value
        val newValue = generateIfNull()
        this[hash] = newValue
        return newValue
    }

    fun put(key: Long, value: V?): V? {
        val slot = findIndex(key)
        if (slot < 0) {
            insert(-slot - 1, key, value)
            return null
        } else {
            val oldValue = values[slot]
            values[slot] = value
            return oldValue
        }
    }

    fun remove(key: Long): V? {
        val slot = findIndex(key)
        return if (slot < 0) null else removeIndex(slot)
    }

    fun remove(key: Long, value: V?): Boolean {
        if (key == 0L) {
            if (containsNull && value == values[nullIndex]) {
                removeNullIndex()
                return true
            } else {
                return false
            }
        } else {
            var pos = HashUtil.mix(key.hashCode()) and mask
            var current = keys[pos]
            if (current == 0L) {
                return false
            } else if (current == key && value == values[pos]) {
                removeIndex(pos)
                return true
            } else {
                do {
                    pos = (pos + 1) and mask
                    current = keys[pos]
                    if (current == 0L) {
                        return false
                    }
                } while (current != key || value != values[pos])

                removeIndex(pos)
                return true
            }
        }
    }

    operator fun get(key: Long): V? {
        val slot = findIndex(key)
        return if (slot < 0) null else values[slot]
    }

    fun replace(key: Long, oldValue: V?, newValue: V?): Boolean {
        val index = findIndex(key)
        if (index >= 0 && values[index] === oldValue) {
            values[index] = newValue
            return true
        } else {
            return false
        }
    }

    fun replace(key: Long, value: V?): V? {
        val index = findIndex(key)
        if (index < 0) {
            return null
        } else {
            val oldValue = values[index]
            values[index] = value
            return oldValue
        }
    }

    fun size(): Int {
        return size
    }

    fun clear() {
        if (size != 0) {
            size = 0
            containsNull = false
            Arrays.fill(keys, 0L)
            Arrays.fill(values, null)
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
            @Suppress("UNCHECKED_CAST")
            values = (arrayOfNulls<Any>(request + 1)) as Array<V?>
            this.size = 0
            containsNull = false
        }
    }

    private fun findIndex(key: Long): Int {
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

    private fun removeIndex(pos: Int): V? {
        if (pos == nullIndex) {
            return if (containsNull) removeNullIndex() else null
        } else {
            val value = values[pos]
            keys[pos] = 0L
            values[pos] = null
            --size
            shiftKeys(pos)
            if (nullIndex > minCapacity && size < maxFill / 4 && nullIndex > 16) {
                rehash(nullIndex / 2)
            }

            return value
        }
    }

    private fun removeNullIndex(): V? {
        val value = values[nullIndex]
        containsNull = false
        keys[nullIndex] = 0L
        values[nullIndex] = null
        --size
        if (nullIndex > minCapacity && size < maxFill / 4 && nullIndex > 16) {
            rehash(nullIndex / 2)
        }

        return value
    }

    private fun insert(slot: Int, key: Long, value: V?) {
        if (slot == nullIndex) {
            containsNull = true
        }

        keys[slot] = key
        values[slot] = value
        if (size++ >= maxFill) {
            rehash(HashUtil.arraySize(size + 1, loadFactor))
        }
    }

    private fun rehash(newSize: Int) {
        val newMask = newSize - 1
        val newKeys = LongArray(newSize + 1)

        @Suppress("UNCHECKED_CAST")
        val newValues = (arrayOfNulls<Any>(newSize + 1)) as Array<V?>
        var i = nullIndex
        var pos = 0

        var j = size - (if (containsNull) 1 else 0)
        while (j-- != 0) {
            --i
            if (i < 0) {
                throw ConcurrentModificationException("Map was modified during rehash")
            }

            if (keys[i] != 0L) {
                if (newKeys[(HashUtil.mix(keys[i].hashCode()) and newMask).also {
                        pos = it
                    }] != 0L) {
                    do {
                        pos = (pos + 1) and newMask
                    } while (newKeys[pos] != 0L)
                }

                newKeys[pos] = keys[i]
            }
            newValues[pos] = values[i]
        }

        newValues[newSize] = values[nullIndex]
        nullIndex = newSize
        mask = newMask
        maxFill = min(ceil((nullIndex.toFloat() * loadFactor).toDouble()).toInt(), nullIndex - 1)
        keys = newKeys
        values = newValues
    }

    private fun shiftKeys(startPos: Int) {
        var startPos = startPos
        while (true) {
            val last = startPos
            startPos = (startPos + 1) and mask

            var current: Long
            while (true) {
                if ((keys[startPos].also { current = it }) == 0L) {
                    keys[last] = 0L
                    values[last] = null
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
            values[last] = values[startPos]
        }
    }
}