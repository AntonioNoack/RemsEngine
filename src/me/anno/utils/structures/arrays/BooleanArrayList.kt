package me.anno.utils.structures.arrays

import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.types.Booleans.withFlag
import kotlin.math.max
import kotlin.math.min

/**
 * more efficient Boolean Array,
 * up to 8x more efficient
 * slower access
 * less thread-save
 *
 * like BitSet; (that doesn't exist with Kotlin -> always use this)
 * */
class BooleanArrayList(var size: Int) : Saveable() {

    constructor() : this(0)

    private var values = LongArray((size + 63) shr 6)
    operator fun get(index: Int): Boolean {
        if (index < 0 || index >= size) return false
        return getUnsafe(index)
    }

    fun getUnsafe(index: Int): Boolean {
        return values[index shr 6].and(1L shl (index and 63)) != 0L
    }

    operator fun set(index: Int, value: Boolean) {
        val arrIndex = index shr 6
        val subIndex = index and 63
        ensureRawCapacity(arrIndex + 1)
        values[arrIndex] = values[arrIndex].withFlag(1L shl subIndex, value)
    }

    fun set(index: Int) {
        set(index, true)
    }

    fun clear(index: Int) {
        set(index, false)
    }

    fun clear() {
        fill(false)
    }

    fun fill(value: Boolean) {
        values.fill(if (value) -1 else 0)
    }

    private fun ensureRawCapacity(newSize: Int) {
        if (values.size < newSize) {
            val newSize1 = max(newSize, max(values.size.shl(1), 16))
            values = values.copyOf(newSize1)
        }
    }

    fun and(other: BooleanArrayList) {
        val ov = other.values
        val si = min(ov.size, values.size)
        for (i in 0 until si) {
            values[i] = values[i] and ov[i]
        }
        values.fill(0L, si, values.size)
    }

    fun or(other: BooleanArrayList) {
        val ov = other.values
        ensureRawCapacity(ov.size)
        for (i in ov.indices) {
            values[i] = values[i] or ov[i]
        }
    }

    val isEmpty: Boolean
        get() {
            return values.all { it == 0L }
        }

    fun nextSetBit(fromIndex: Int): Int {
        return findNextSetBit(fromIndex, 0)
    }

    fun nextClearBit(fromIndex: Int): Int {
        return findNextSetBit(fromIndex, -1)
    }

    private fun findNextSetBit(fromIndex: Int, readMask: Long): Int {
        val arrayLength = values.size
        val bitIndex = fromIndex and 63
        var wordIndex = fromIndex ushr 6

        // Check in the current word
        if (wordIndex < arrayLength) {
            // Zero out all bits before 'bitIndex'
            val word = (values[wordIndex] xor readMask) ushr bitIndex
            if (word != 0L) {
                return fromIndex + word.countTrailingZeroBits()
            }
            wordIndex++
        }

        // Check subsequent words
        while (wordIndex < arrayLength) {
            val word = values[wordIndex] xor readMask
            if (word != 0L) {
                return wordIndex * 64 + word.countTrailingZeroBits()
            }
            wordIndex++
        }

        // If no set bit is found
        return -1
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("size", size)
        writer.writeLongArray("values", values)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "size" -> size = value as? Int ?: return
            "values" -> values = value as? LongArray ?: return
            else -> super.setProperty(name, value)
        }
    }

    fun last(): Boolean = get(size - 1)
    fun peek(): Boolean = last()

    fun push(v: Boolean) {
        ensureRawCapacity((size + 64) ushr 6)
        set(size++, v)
    }

    fun pop(): Boolean {
        return getUnsafe(--size)
    }

    override val className: String get() = "BoolArray"
    override val approxSize get() = 1

    companion object {
        fun valueOf(v: LongArray): BooleanArrayList {
            val r = BooleanArrayList(v.size * 64)
            for (i in v.indices) {
                r.values[i] = v[i]
            }
            return r
        }
    }
}