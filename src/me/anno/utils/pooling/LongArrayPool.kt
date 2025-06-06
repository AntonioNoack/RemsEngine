package me.anno.utils.pooling

open class LongArrayPool(size: Int) : BufferPool<LongArray>(size, 4) {
    override fun implCreateBuffer(size: Int) = LongArray(size)
    override fun implGetSize(buffer: LongArray) = buffer.size
    override fun implClear(buffer: LongArray, size: Int) {
        buffer.fill(0L, 0, size)
    }

    override fun implCopyTo(src: LongArray, dst: LongArray, size: Int) {
        src.copyInto(dst, 0, 0, size)
    }
}