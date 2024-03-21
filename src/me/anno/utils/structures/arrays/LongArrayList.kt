package me.anno.utils.structures.arrays

@Suppress("unused")
open class LongArrayList(initCapacity: Int) : NativeArrayList {

    override var size = 0

    var values: LongArray = LongArray(initCapacity)
    override val capacity: Int get() = values.size

    fun add(value: Long) = plusAssign(value)
    operator fun set(index: Int, value: Long) {
        values[index] = value
    }

    fun toArray(): LongArray = values.copyOf(size)

    override fun resize(newSize: Int) {
        values = values.copyOf(newSize)
    }

    operator fun get(index: Int) = values[index]
    operator fun plusAssign(value: Long) {
        ensureExtra(1)
        values[size++] = value
    }
}