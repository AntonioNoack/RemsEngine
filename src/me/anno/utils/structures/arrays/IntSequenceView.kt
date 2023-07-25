package me.anno.utils.structures.arrays

import me.anno.utils.types.Strings.joinChars0

class IntSequenceView(val base: IntSequence, val i0: Int, val i1: Int) : IntSequence {

    override val length: Int = i1 - i0

    override fun get(index: Int): Int = base[index + i0]
    override fun getOrNull(index: Int): Int? = base.getOrNull(index + i0)

    override fun subSequence(startIndex: Int, endIndex: Int): IntSequence {
        if (startIndex == 0 && endIndex == length) return this
        return IntSequenceView(base, i0 + startIndex, i0 + endIndex)
    }

    override fun toString(): String {
        if (length <= 0) return ""
        val builder = StringBuilder(length)
        // could be optimized
        for (index in i0 until i1) {
            val char = base[index]
            builder.append(char.joinChars0())
        }
        return builder.toString()
    }
}