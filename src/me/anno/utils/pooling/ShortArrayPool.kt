package me.anno.utils.pooling

open class ShortArrayPool(size: Int, exactMatchesOnly: Boolean) : BufferPool<ShortArray>(size, exactMatchesOnly) {

    override fun createBuffer(size: Int) = ShortArray(size)

    override fun getSize(buffer: ShortArray) = buffer.size

    override fun clear(buffer: ShortArray, size: Int) {
        buffer.fill(0, 0, size)
    }

}