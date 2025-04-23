package me.anno.utils.structures.arrays

@Suppress("unused")
open class ShortArrayList(initCapacity: Int) : NativeArrayList {

    override var size = 0

    var values: ShortArray = ShortArray(initCapacity)
    override val capacity: Int get() = values.size

    fun add(value: Short) = plusAssign(value)

    fun addAll(src: ShortArray?, startIndex: Int, length: Int) {
        ensureExtra(length)
        addUnsafe(src, startIndex, length)
    }

    fun addUnsafe(src: ShortArray?, startIndex: Int, length: Int) {
        src?.copyInto(values, size, startIndex, startIndex + length)
        size += length
    }

    operator fun set(index: Int, value: Short) {
        values[index] = value
    }

    fun toShortArray(): ShortArray = values.copyOf(size)

    override fun resize(newSize: Int) {
        values = values.copyOf(newSize)
    }

    operator fun get(index: Int) = values[index]
    operator fun plusAssign(value: Short) {
        ensureExtra(1)
        values[size++] = value
    }
}