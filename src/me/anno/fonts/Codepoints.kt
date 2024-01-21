package me.anno.fonts

/**
 * extracting characters from UTF-16 strings, because Java and Kotlin chose them,
 * and we want emoji support; however Kotlin doesn't support .codePoints(), so we had to add it ourselves
 * */
object Codepoints {

    // todo join multi-char emojis, too?
    private fun isHighSurrogate(high: Char): Boolean {
        return high.code in 0xd800..0xdbff
    }

    private fun isLowSurrogate(low: Char): Boolean {
        return low.code in 0xdc00..0xdfff
    }

    private fun toCodepoint(high: Char, low: Char): Int {
        return (high.code shl 10) + low.code - 56613888
    }

    fun CharSequence.needsCodepoints(): Boolean {
        for (i in 0 until length - 1) {
            if (isHighSurrogate(this[i]) && isLowSurrogate(this[i + 1])) {
                return true
            }
        }
        return false
    }

    fun CharSequence.codepointsSize(): Int {
        var len = length
        for (i in 0 until length - 1) {
            if (isHighSurrogate(this[i]) && isLowSurrogate(this[i + 1])) {
                len--
            }
        }
        return len
    }

    fun CharSequence.codepoints(): IntArray {
        val result = IntArray(codepointsSize())
        var ri = 0
        for (wi in result.indices) {
            result[wi] = if (ri + 1 < length &&
                isHighSurrogate(this[ri]) &&
                isLowSurrogate(this[ri + 1])
            ) toCodepoint(this[ri++], this[ri++])
            else this[ri++].code
        }
        return result
    }
}