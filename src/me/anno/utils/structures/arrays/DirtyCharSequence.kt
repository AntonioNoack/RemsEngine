package me.anno.utils.structures.arrays

/**
 * a [CharSequence], which does no proper character conversion; it is means for
 * coloring programming code only, and should not be used for String generation
 * */
class DirtyCharSequence(val base: IntSequence, val i0: Int = 0, val i1: Int = base.length) : CharSequence {

    override val length: Int = i1 - i0

    override fun get(index: Int): Char = base[i0 + index].toChar()

    fun firstOrNull() = base.getOrNull(i0)?.toChar()

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return DirtyCharSequence(base, i0 + startIndex, i0 + endIndex)
    }

    override fun toString(): String {
        val builder = StringBuilder(length)
        // could be optimized
        for (index in i0 until i1) {
            builder.append(Character.toChars(base[index]))
        }
        return builder.toString()
    }

}