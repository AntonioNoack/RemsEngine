package me.anno.utils.pooling

open class FloatArrayPool(size: Int) : BufferPool<FloatArray>(size, 4) {
    override fun createBuffer(size: Int) = FloatArray(size)
    override fun getSize(buffer: FloatArray) = buffer.size
    override fun clear(buffer: FloatArray, size: Int) {
        buffer.fill(0f, 0, size)
    }

    override fun copy(src: FloatArray, dst: FloatArray, size: Int) {
        src.copyInto(dst, 0, 0, size)
    }
}