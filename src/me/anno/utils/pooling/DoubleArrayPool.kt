package me.anno.utils.pooling

open class DoubleArrayPool(size: Int) : BufferPool<DoubleArray>(size, 4) {
    override fun implCreateBuffer(size: Int) = DoubleArray(size)
    override fun implGetSize(buffer: DoubleArray) = buffer.size
    override fun implClear(buffer: DoubleArray, size: Int) {
        buffer.fill(0.0, 0, size)
    }

    override fun implCopyTo(src: DoubleArray, dst: DoubleArray, size: Int) {
        src.copyInto(dst, 0, 0, size)
    }
}