package me.anno.utils.structures.arrays

import org.apache.logging.log4j.LogManager

@Suppress("unused")
open class ByteArrayList(initialCapacity: Int) : NativeArrayList {

    companion object {
        private val LOGGER = LogManager.getLogger(ByteArrayList::class)
    }

    override var size = 0
    var values: ByteArray = ByteArray(initialCapacity)
    override val capacity: Int get() = values.size

    fun add(value: Byte) = plusAssign(value)

    fun addAll(src: ByteArray?, startIndex: Int, length: Int) {
        ensureExtra(length)
        addUnsafe(src, startIndex, length)
    }

    operator fun set(index: Int, value: Byte) {
        values[index] = value
    }

    fun addUnsafe(src: ByteArray?, startIndex: Int, length: Int) {
        src?.copyInto(values, size, startIndex, startIndex + length)
        size += length
    }

    fun addUnsafe(src: Byte) {
        values[size++] = src
    }

    fun addUnsafe(src: ByteArrayList, startIndex: Int, length: Int) {
        addUnsafe(src.values, startIndex, length)
    }

    fun addAll(src: ByteArrayList, startIndex: Int, length: Int) {
        addAll(src.values, startIndex, length)
    }

    fun first() = values[0]
    fun last() = values[size - 1]

    operator fun get(index: Int): Byte = values[index]
    operator fun plusAssign(value: Byte) {
        ensureExtra(1)
        addUnsafe(value)
    }

    override fun skip(delta: Int) {
        ensureExtra(delta)
        size += delta
    }

    fun fill(value: Byte, index0: Int = 0, index1: Int = size) {
        values.fill(value, index0, index1)
    }

    fun toByteArray(dstSize: Int = size): ByteArray {
        val array = values
        if (size == array.size) return array
        return array.copyOf(dstSize)
    }

    override fun resize(newSize: Int) {
        values = values.copyOf(newSize)
    }

    fun decodeToString(dstSize: Int = size): String {
        return values.decodeToString(0, dstSize)
    }

    override fun appendValue(sb: StringBuilder, index: Int) {
        sb.append(values[index])
    }

    override fun toString(): String {
        return toStringImpl()
    }
}