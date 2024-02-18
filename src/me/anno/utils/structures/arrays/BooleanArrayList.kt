package me.anno.utils.structures.arrays

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter

/**
 * more efficient Boolean Array,
 * up to 8x more efficient
 * slower access
 * less thread-save
 *
 * todo how does it compare to BitSet?
 * */
class BooleanArrayList(var size: Int) : Saveable() {

    constructor() : this(0)

    constructor(size: Int, value: Boolean) : this(size) {
        if (value) {
            for (i in values.indices) {
                values[i] = -1
            }
        }
    }

    constructor(size: Int, filler: (index: Int) -> Boolean) : this(size) {
        for (i in 0 until size) {
            if (filler(i)) {
                this[i] = true
            }
        }
    }

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
}