package me.anno.utils.types

object Arrays {

    @JvmStatic
    fun ByteArray?.resize(size: Int) =
        if (this == null || this.size != size) ByteArray(size) else this

    @JvmStatic
    fun IntArray?.resize(size: Int) =
        if (this == null || this.size != size) IntArray(size) else this

    @JvmStatic
    fun FloatArray?.resize(size: Int) =
        if (this == null || this.size != size) FloatArray(size) else this

    @JvmStatic
    @Suppress("unused")
    fun <V> Array<V>.joinToCompress(separator: String = ", ", prefix: String = "[", suffix: String = "]"): String {
        val builder = StringBuilder(prefix)
        if (isNotEmpty()) {
            var prev = this[0]
            var ctr = 1
            for (i in 1 until size) {
                val curr = this[i]
                if (prev != curr) {
                    // append prev
                    if (ctr > 1) builder.append(ctr).append("x ")
                    builder.append(prev)
                    builder.append(separator)
                    ctr = 1
                    prev = curr
                } else ctr++
            }
            // append prev
            if (ctr > 1) builder.append(ctr).append("x ")
            builder.append(prev)
        }
        builder.append(suffix)
        return builder.toString()
    }

    @JvmStatic
    fun LongArray.rotateRight(shift: Int) {
        val wrapAround = LongArray(shift)
        copyInto(wrapAround, 0, size - shift, size)
        copyInto(this, shift, 0, size - shift)
        wrapAround.copyInto(this)
    }

    @JvmStatic
    inline fun <reified V> Array<V>.rotateRight(shift: Int) {
        val wrapAround = Array(shift) { this[size - shift + it] }
        copyInto(this, shift, 0, size - shift)
        wrapAround.copyInto(this)
    }
}