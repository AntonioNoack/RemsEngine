package me.anno.utils

object Tabs {

    class ConstantCharSequence(
        val char: Char,
        override val length: Int,
        val suffix: CharSequence
    ) : CharSequence {

        val repLength = length - suffix.length

        override fun get(index: Int): Char {
            return if (index < repLength) char
            else suffix[index - repLength]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return if (suffix.isEmpty()) {
                ConstantCharSequence(char, endIndex - startIndex, "")
            } else toString().subSequence(startIndex, endIndex)
        }

        override fun toString(): String {
            val data = CharArray(length)
            data.fill(char, 0, repLength)
            for (i in suffix.indices) {
                data[i + repLength] = suffix[i]
            }
            return String(data)
        }

        operator fun plus(other: CharSequence): CharSequence {
            return if (suffix.isEmpty()) ConstantCharSequence(char, length + other.length, other)
            else toString() + other
        }

    }

    @JvmStatic
    fun spaces(ctr: Int) = ConstantCharSequence(' ', ctr, "")

    @JvmStatic
    fun tabs(ctr: Int) = ConstantCharSequence('\t', ctr, "")

}