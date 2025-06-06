package me.anno.utils.pooling

open class FloatArrayPool(size: Int) : BufferPool<FloatArray>(size, 4) {
    override fun implCreateBuffer(size: Int) = FloatArray(size)
    override fun implGetSize(buffer: FloatArray) = buffer.size
    override fun implClear(buffer: FloatArray, size: Int) {
        buffer.fill(0f, 0, size)
    }

    override fun implCopyTo(src: FloatArray, dst: FloatArray, size: Int) {
        src.copyInto(dst, 0, 0, size)
    }
}