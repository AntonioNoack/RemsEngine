package me.anno.utils.structures.arrays

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
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
    operator fun get(index: Int) = values[index shr 6].and(1L shl (index and 63)) != 0L
    operator fun set(index: Int, value: Boolean) {
        val arrIndex = index shr 6
        val subIndex = index and 63
        if (value) {
            values[arrIndex] = values[arrIndex] or (1L shl subIndex)
        } else {
            values[arrIndex] = values[arrIndex] and (1L shl subIndex).inv()
        }
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
            values = values.copyOf(newSize)
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

    fun nextSetBit(index: Int): Int {
        // todo optimize this
        for (i in index until size) {
            if (this[i]) {
                return i
            }
        }
        return -1
    }

    fun nextClearBit(index: Int): Int {
        // todo optimize this
        for (i in index until size) {
            if (!this[i]) {
                return i
            }
        }
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