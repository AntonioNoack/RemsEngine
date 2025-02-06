package me.anno.utils.pooling

open class LongArrayPool(size: Int) : BufferPool<LongArray>(size, 4) {
    override fun createBuffer(size: Int) = LongArray(size)
    override fun getSize(buffer: LongArray) = buffer.size
    override fun clear(buffer: LongArray, size: Int) {
        buffer.fill(0L, 0, size)
    }

    override fun copy(src: LongArray, dst: LongArray, size: Int) {
        src.copyInto(dst, 0, 0, size)
    }
}