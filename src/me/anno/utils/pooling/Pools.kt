package me.anno.utils.pooling

object Pools {

    @JvmField
    val byteBufferPool = ByteBufferPool(64)

    @JvmField
    val byteArrayPool = ByteArrayPool(64)

    @JvmField
    val intArrayPool = IntArrayPool(64)

    @JvmField
    val floatArrayPool = FloatArrayPool(64)

    @JvmField
    val arrayPool = ArrayPool(64)

    @JvmStatic
    fun freeUnusedEntries() {
        byteBufferPool.freeUnusedEntries()
        byteArrayPool.freeUnusedEntries()
        intArrayPool.freeUnusedEntries()
        floatArrayPool.freeUnusedEntries()
        arrayPool.freeUnusedEntries()
    }

    @JvmStatic
    fun gc() {
        byteBufferPool.gc()
        byteArrayPool.gc()
        intArrayPool.gc()
        floatArrayPool.gc()
        arrayPool.gc()
    }
}