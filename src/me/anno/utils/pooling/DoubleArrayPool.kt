package me.anno.utils.pooling

open class DoubleArrayPool(size: Int) : BufferPool<DoubleArray>(size, 4) {
    override fun createBuffer(size: Int) = DoubleArray(size)
    override fun getSize(buffer: DoubleArray) = buffer.size
    override fun clear(buffer: DoubleArray, size: Int) {
        buffer.fill(0.0, 0, size)
    }

    override fun copy(src: DoubleArray, dst: DoubleArray, size: Int) {
        src.copyInto(dst, 0, 0, size)
    }
}