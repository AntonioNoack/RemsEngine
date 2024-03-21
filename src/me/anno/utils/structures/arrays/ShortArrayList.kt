package me.anno.utils.structures.arrays

@Suppress("unused")
open class ShortArrayList(initCapacity: Int) : NativeArrayList {

    override var size = 0

    var values: ShortArray = ShortArray(initCapacity)
    override val capacity: Int get() = values.size

    fun add(value: Short) = plusAssign(value)
    operator fun set(index: Int, value: Short) {
        values[index] = value
    }

    fun toArray(): ShortArray = values.copyOf(size)

    override fun resize(newSize: Int) {
        values = values.copyOf(newSize)
    }

    operator fun get(index: Int) = values[index]
    operator fun plusAssign(value: Short) {
        ensureExtra(1)
        values[size++] = value
    }
}