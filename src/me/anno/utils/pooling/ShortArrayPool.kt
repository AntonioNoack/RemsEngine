package me.anno.utils.pooling

open class ShortArrayPool(size: Int) : BufferPool<ShortArray>(size, 2) {
    override fun implCreateBuffer(size: Int) = ShortArray(size)
    override fun implGetSize(buffer: ShortArray) = buffer.size
    override fun implClear(buffer: ShortArray, size: Int) {
        buffer.fill(0, 0, size)
    }

    override fun implCopyTo(src: ShortArray, dst: ShortArray, size: Int) {
        src.copyInto(dst, 0, 0, size)
    }
}