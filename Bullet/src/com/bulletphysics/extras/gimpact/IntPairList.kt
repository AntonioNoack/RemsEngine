package com.bulletphysics.extras.gimpact

import com.bulletphysics.util.Packing
import kotlin.math.max
import kotlin.math.min

/**
 * @author jezek2
 */
class IntPairList {

    private var content = LongArray(32)

    @JvmField
    var size: Int = 0

    fun clear() {
        size = 0
    }

    fun size(): Int {
        return size
    }

    fun getFirst(index: Int): Int {
        if (index >= size) throw IndexOutOfBoundsException()
        return Packing.unpackHigh(content[index])
    }

    fun getSecond(index: Int): Int {
        if (index >= size) throw IndexOutOfBoundsException()
        return Packing.unpackLow(content[index])
    }

    fun getQuick(index: Int): Long {
        return content[index]
    }

    fun setQuick(index: Int, value: Long) {
        content[index] = value
    }

    private fun expand() {
        resize(content.size shl 1)
    }

    fun resize(newLength: Int) {
        var newLength = newLength
        if (newLength > content.size) {
            // scale up to avoid resizing many times
            newLength = max(newLength, content.size shl 1)
        } else if (content.size < newLength * 2) {
            // good enough
            return
        }
        val newArray = LongArray(newLength)
        System.arraycopy(content, 0, newArray, 0, min(content.size, newLength))
        content = newArray
    }

    fun pushPair(first: Int, second: Int) {
        if (size == content.size) {
            expand()
        }
        content[size] = Packing.pack(first, second)
        size++
    }

    fun setPair(index: Int, first: Int, second: Int) {
        content[index] = Packing.pack(first, second)
    }
}
