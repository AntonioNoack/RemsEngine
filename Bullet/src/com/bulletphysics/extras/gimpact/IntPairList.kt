package com.bulletphysics.extras.gimpact

import me.anno.maths.Packing.pack64
import me.anno.maths.Packing.unpackHighFrom64
import me.anno.maths.Packing.unpackLowFrom64
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
        return unpackHighFrom64(content[index])
    }

    fun getSecond(index: Int): Int {
        if (index >= size) throw IndexOutOfBoundsException()
        return unpackLowFrom64(content[index])
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
        content[size] = pack64(first, second)
        size++
    }

    fun setPair(index: Int, first: Int, second: Int) {
        content[index] = pack64(first, second)
    }
}
