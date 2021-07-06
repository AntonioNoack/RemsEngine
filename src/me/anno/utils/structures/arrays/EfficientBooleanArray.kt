package me.anno.utils.structures.arrays

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter

/**
 * more efficient Boolean Array,
 * up to 8x more efficient
 * slower access
 * less thread-save
 * */
class EfficientBooleanArray(var size: Int) : Saveable() {

    constructor() : this(0)

    constructor(size: Int, value: Boolean): this(size){
        if(value){
            for (i in values.indices) {
                values[i] = -1
            }
        }
    }

    constructor(size: Int, filler: (index: Int) -> Boolean) : this(size) {
        for (i in 0 until size) {
            if(filler(i)){
                this[i] = true
            }
        }
    }

    private var values = IntArray((size + 31) shr 5)
    operator fun get(index: Int) = values[index shr 5].and(1 shl (index and 31)) != 0
    operator fun set(index: Int, value: Boolean) {
        val arrIndex = index shr 5
        val subIndex = index and 31
        if (value) {
            values[arrIndex] = values[arrIndex] or (1 shl subIndex)
        } else {
            values[arrIndex] = values[arrIndex] and (1 shl subIndex).inv()
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("size", size)
        writer.writeIntArray("values", values)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "size" -> size = value
            else -> super.readInt(name, value)
        }
    }

    override fun readIntArray(name: String, values: IntArray) {
        when (name) {
            "values" -> this.values = values
            else -> super.readIntArray(name, values)
        }
    }

    override fun isDefaultValue(): Boolean = false
    override val className get() = "BoolArray"
    override val approxSize get() = 1

}