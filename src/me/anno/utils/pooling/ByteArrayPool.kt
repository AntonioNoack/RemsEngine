package me.anno.utils.pooling

open class ByteArrayPool(size: Int, exactMatchesOnly: Boolean) : BufferPool<ByteArray>(size, exactMatchesOnly) {

    override fun createBuffer(size: Int) = ByteArray(size)

    override fun getSize(buffer: ByteArray) = buffer.size

    override fun clear(buffer: ByteArray, size: Int) {
        buffer.fill(0, 0, size)
    }

}