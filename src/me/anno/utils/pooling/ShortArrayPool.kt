package me.anno.utils.pooling

open class ShortArrayPool(size: Int) : BufferPool<ShortArray>(size, 2) {

    override fun prepare(buffer: ShortArray, size: Int) {}

    override fun createBuffer(size: Int) = ShortArray(size)

    override fun getSize(buffer: ShortArray) = buffer.size

    override fun clear(buffer: ShortArray, size: Int) {
        buffer.fill(0, 0, size)
    }

    override fun destroy(buffer: ShortArray) {}

}