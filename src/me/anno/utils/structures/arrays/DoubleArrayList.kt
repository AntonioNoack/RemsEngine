package me.anno.utils.structures.arrays

@Suppress("unused")
open class DoubleArrayList(initCapacity: Int) : NativeArrayList {

    override var size = 0

    var values: DoubleArray = DoubleArray(initCapacity)
    override val capacity: Int get() = values.size

    fun add(value: Double) = plusAssign(value)
    operator fun set(index: Int, value: Double) {
        values[index] = value
    }

    fun toArray(): DoubleArray = values.copyOf(size)
    fun toList(): List<Double> = (0 until size).map { this[it] }

    override fun resize(newSize: Int) {
        values = values.copyOf(newSize)
    }

    operator fun get(index: Int) = values[index]
    operator fun plusAssign(value: Double) {
        ensureExtra(1)
        values[size++] = value
    }

    override fun toString(): String = toList().toString()
}