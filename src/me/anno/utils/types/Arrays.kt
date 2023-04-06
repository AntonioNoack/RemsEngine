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
    @Suppress("unused")
    inline fun <reified V> Array<V>.subArray(i0: Int = 0, i1: Int = size): Array<V> {
        return Array(i1 - i0) { this[i0 + it] }
    }

    // if you need your own, message me 😄
    @JvmStatic
    fun ByteArray.subArray(i0: Int = 0, i1: Int = size) = ByteArray(i1 - i0) { this[i0 + it] }

    @JvmStatic
    fun IntArray.subArray(i0: Int = 0, i1: Int = size) = IntArray(i1 - i0) { this[i0 + it] }

    @JvmStatic
    fun <V> Array<V>.subList(i0: Int = 0, i1: Int = size): List<V> = asList().subList(i0, i1)

    @JvmStatic
    fun LongArray.rotateRight(shift: Int) {
        val end = LongArray(shift) { this[size - shift + it] }
        System.arraycopy(this, 0, this, shift, size - shift)
        System.arraycopy(end, 0, this, 0, shift)
    }

    @JvmStatic
    inline fun <reified V> Array<V>.rotateRight(shift: Int) {
        val end = Array(shift) { this[size - shift + it] }
        System.arraycopy(this, 0, this, shift, size - shift)
        System.arraycopy(end, 0, this, size - shift, shift)
    }

}