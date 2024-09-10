package me.anno.io.xml

import me.anno.utils.assertions.assertContains
import kotlin.math.max

class ComparableStringBuilder(init: Int = 16) : CharSequence {

    constructor(str: String) : this(str.length) {
        append(str)
    }

    var value = CharArray(init)

    override var length: Int = 0
    override fun get(index: Int): Char {
        assertContains(index, indices)
        return value[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return value.concatToString(startIndex, endIndex)
    }

    override fun toString(): String {
        return value.concatToString(0, length)
    }

    fun clear() {
        length = 0
        hash = 0
    }

    fun append(c: Char): ComparableStringBuilder {
        if (length >= value.size) ensureCapacity(value.size * 2)
        value[length++] = c
        hash = 0
        return this
    }

    fun append(str: String): ComparableStringBuilder {
        if (length + str.length > value.size) ensureCapacity(max(value.size * 2, length + str.length))
        str.toCharArray().copyInto(value, length)
        length += str.length
        hash = 0
        return this
    }

    fun append(str: Any?): ComparableStringBuilder {
        append(str.toString())
        return this
    }

    fun ensureCapacity(size: Int) {
        if (size < value.size) return
        value = value.copyOf(size)
    }

    private var hash = 0
    override fun hashCode(): Int {
        var h = hash
        if (h == 0 && isNotEmpty()) {
            val value = value
            for (i in indices) {
                h = 31 * h + value[i].code
            }
            hash = h
        }
        return h
    }

    override fun equals(other: Any?): Boolean {
        return if (other is CharSequence) {
            if (length != other.length) return false
            val value = value
            for (i in indices) {
                if (value[i] != other[i])
                    return false
            }
            return true
        } else false
    }
}