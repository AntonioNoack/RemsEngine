package me.anno.utils.pooling

open class FloatArrayPool(size: Int, exactMatchesOnly: Boolean) : BufferPool<FloatArray>(size, exactMatchesOnly) {

    override fun createBuffer(size: Int) = FloatArray(size)

    override fun getSize(buffer: FloatArray) = buffer.size

    override fun clear(buffer: FloatArray, size: Int) {
        buffer.fill(0f, 0, size)
    }

}