package me.anno.utils.types

import me.anno.utils.structures.lists.Lists.createArrayList

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
    fun LongArray.rotateRight(shift: Int) {
        val wrapAround = LongArray(shift)
        copyInto(wrapAround, 0, size - shift, size)
        copyInto(this, shift, 0, size - shift)
        wrapAround.copyInto(this)
    }

    @JvmStatic
    fun <V> ArrayList<V>.rotateRight(shift: Int) {
        val wrapAround = createArrayList(shift) { this[size - shift + it] }
        copyInto(this, shift, 0, size - shift)
        wrapAround.copyInto(this)
    }

    @JvmStatic
    fun <V> ArrayList<V>.copyInto(dst: MutableList<V>, dstI0: Int, srcI: Int, srcEndI: Int) {
        assert(this !== dst)
        var dstI = dstI0
        for (i in srcI until srcEndI) {
            dst[dstI++] = this[i]
        }
    }

    @JvmStatic
    fun <V> ArrayList<V>.copyInto(dst: MutableList<V>) {
        copyInto(dst, 0, 0, size)
    }

    fun ByteArray.startsWith(other: ByteArray, i: Int): Boolean {
        if (i < 0 || i > size - other.size) return false
        for (j in other.indices) {
            if (this[i + j] != other[j]) {
                return false
            }
        }
        return true
    }

    fun ByteArray.indexOf2(other: ByteArray, i0: Int): Int {
        for (i in i0 until size - other.size) {
            if (startsWith(other, i)) {
                return i
            }
        }
        return -1
    }

}