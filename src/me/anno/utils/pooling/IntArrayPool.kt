package me.anno.utils.pooling

open class IntArrayPool(size: Int) : BufferPool<IntArray>(size, 4) {
    override fun implCreateBuffer(size: Int) = IntArray(size)
    override fun implGetSize(buffer: IntArray) = buffer.size
    override fun implClear(buffer: IntArray, size: Int) {
        buffer.fill(0, 0, size)
    }

    override fun implCopyTo(src: IntArray, dst: IntArray, size: Int) {
        src.copyInto(dst, 0, 0, size)
    }
}