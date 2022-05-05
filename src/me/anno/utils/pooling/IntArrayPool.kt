package me.anno.utils.pooling

open class IntArrayPool(size: Int) : BufferPool<IntArray>(size) {

    override fun createBuffer(size: Int) = IntArray(size)

    override fun clear(buffer: IntArray, size: Int) {
        buffer.fill(0, 0, size)
    }

    override fun getSize(buffer: IntArray) = buffer.size

}