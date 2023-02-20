package me.anno.utils.types

object Arrays {

    fun ByteArray?.resize(size: Int) =
        if (this == null || this.size != size) ByteArray(size) else this

    fun IntArray?.resize(size: Int) =
        if (this == null || this.size != size) IntArray(size) else this

    fun FloatArray?.resize(size: Int) =
        if (this == null || this.size != size) FloatArray(size) else this

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
    inline fun <reified V> Array<V>.subArray(i0: Int = 0, i1: Int = size): Array<V> {
        return Array(i1 - i0) { this[i0 + it] }
    }

    @JvmStatic
    fun ByteArray.subArray(i0: Int = 0, i1: Int = size): ByteArray {
        return ByteArray(i1 - i0) { this[i0 + it] }
    }

    @JvmStatic
    fun <V> Array<V>.subList(i0: Int = 0, i1: Int = size): List<V> {
        val self = this
        return object : List<Any?> {
            override val size: Int = i1 - i0
            override fun get(index: Int) = self[i0 + index]
            override fun isEmpty() = i1 == i0
            override fun iterator() = listIterator()
            override fun listIterator() = listIterator(0)
            override fun listIterator(index: Int): ListIterator<Any?> {
                return object : ListIterator<Any?> {
                    var idx = i0 + index
                    override fun hasNext() = idx < i1
                    override fun hasPrevious() = idx > i0
                    override fun next() = self[idx++]
                    override fun nextIndex() = idx - i0
                    override fun previous() = self[--idx]
                    override fun previousIndex() = idx - i0 - 1
                }
            }

            override fun subList(fromIndex: Int, toIndex: Int): List<Any?> {
                return self.subList(i0 + fromIndex, i0 + toIndex)
            }

            override fun lastIndexOf(element: Any?): Int {
                for (i in i1 - 1 downTo i0) {
                    if (self[i] == element) return i - i0
                }
                return -1
            }

            override fun indexOf(element: Any?): Int {
                for (i in i0 until i1) {
                    if (self[i] == element) return i - i0
                }
                return -1
            }

            override fun containsAll(elements: Collection<Any?>): Boolean {
                for (element in elements) {
                    if (!contains(element)) return false
                }
                return true
            }

            override fun contains(element: Any?) = indexOf(element) >= 0

        } as List<V>
    }

    fun LongArray.rotateRight(shift: Int) {
        val end = LongArray(shift) { this[size - shift + it] }
        System.arraycopy(this, 0, this, shift, size - shift)
        System.arraycopy(end, 0, this, 0, shift)
    }

    inline fun <reified V> Array<V>.rotateRight(shift: Int) {
        val end = Array(shift) { this[size - shift + it] }
        System.arraycopy(this, 0, this, shift, size - shift)
        System.arraycopy(end, 0, this, size - shift, shift)
    }

}