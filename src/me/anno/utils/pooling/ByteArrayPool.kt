package me.anno.utils.pooling

open class ByteArrayPool(size: Int) : BufferPool<ByteArray>(size, 1) {
    override fun implCreateBuffer(size: Int) = ByteArray(size)
    override fun implGetSize(buffer: ByteArray) = buffer.size
    override fun implClear(buffer: ByteArray, size: Int) {
        buffer.fill(0, 0, size)
    }

    override fun implCopyTo(src: ByteArray, dst: ByteArray, size: Int) {
        src.copyInto(dst, 0, 0, size)
    }
}