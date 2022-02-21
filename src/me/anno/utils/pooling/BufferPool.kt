package me.anno.utils.pooling

abstract class BufferPool<V>(val size: Int, val exactMatchesOnly: Boolean) {

    abstract fun clear(buffer: V, size: Int)
    abstract fun getSize(buffer: V): Int
    abstract fun createBuffer(size: Int): V

    private val available = arrayOfNulls<Any?>(size)
    operator fun get(size: Int, clear: Boolean): V {
        val maxSize = if (exactMatchesOnly) size else size * 2
        synchronized(this) {
            for (i in 0 until this.size) {
                val candidate = available.getOrNull(i)
                if (candidate != null) {
                    candidate as V
                    if (getSize(candidate) in size..maxSize) {
                        available[i] = null
                        if (clear) {
                            clear(candidate, size)
                        }
                        return candidate
                    }
                }
            }
        }
        return createBuffer(size)
    }

    fun returnBuffer(buffer: V?) {
        buffer ?: return
        synchronized(this) {
            for (i in 0 until size) {
                if (available[i] === buffer) return // mmh
            }
            for (i in 0 until size) {
                if (available[i] == null) {
                    available[i] = buffer
                    return
                }
            }
            val index = (Math.random() * size).toInt()
            available[index % size] = buffer
        }
    }

    operator fun plusAssign(buffer: V?) {
        returnBuffer(buffer)
    }

}