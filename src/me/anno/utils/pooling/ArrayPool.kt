package me.anno.utils.pooling

open class ArrayPool(size: Int) : BufferPool<Array<Any?>>(size, 4) {
    override fun implCreateBuffer(size: Int) = arrayOfNulls<Any>(size)
    override fun implGetSize(buffer: Array<Any?>) = buffer.size
    override fun implClear(buffer: Array<Any?>, size: Int) {
        buffer.fill(null, 0, size)
    }

    override fun implCopyTo(src: Array<Any?>, dst: Array<Any?>, size: Int) {
        src.copyInto(dst, 0, 0, size)
    }
}