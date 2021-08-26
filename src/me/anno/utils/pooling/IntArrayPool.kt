package me.anno.utils.pooling

open class IntArrayPool(val size: Int, val exactMatchesOnly: Boolean) {

    val available = arrayOfNulls<IntArray>(size)
    operator fun get(size: Int, clear: Boolean): IntArray {
        val maxSize = if (exactMatchesOnly) size else size * 2
        synchronized(this) {
            for (i in 0 until this.size) {
                val candidate = available[i]
                if (candidate != null) {
                    if (candidate.size in size..maxSize) {
                        available[i] = null
                        if (clear) {
                            for (j in 0 until size) {
                                candidate[j] = 0
                            }
                        }
                        return candidate
                    }
                }
            }
        }
        return IntArray(size)
    }

    fun returnBuffer(buffer: IntArray?) {
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

    operator fun plusAssign(buffer: IntArray?) {
        returnBuffer(buffer)
    }

}