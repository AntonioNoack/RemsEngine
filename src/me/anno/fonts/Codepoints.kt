package me.anno.fonts

/**
 * extracting characters from UTF-16 strings, because Java and Kotlin chose them,
 * and we want emoji support; however Kotlin doesn't support .codePoints(), so we had to add it ourselves
 * */
object Codepoints {

    // todo join multi-char emojis, too?
    @JvmStatic
    private fun isHighSurrogate(high: Char): Boolean {
        return high.code in 0xd800..0xdbff
    }

    @JvmStatic
    private fun isLowSurrogate(low: Char): Boolean {
        return low.code in 0xdc00..0xdfff
    }

    @JvmStatic
    private fun isSurrogatePair(high: Char, low: Char): Boolean {
        return isHighSurrogate(high) and isLowSurrogate(low)
    }

    @JvmStatic
    private fun toCodepoint(high: Char, low: Char): Int {
        return (high.code shl 10) + low.code - 0x35fdc00
    }

    @JvmStatic
    @Suppress("unused")
    fun CharSequence.needsCodepoints(): Boolean {
        for (i in 0 until length - 1) {
            if (isSurrogatePair(this[i], this[i + 1])) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun CharSequence.countCodepoints(): Int {
        var result = length
        for (i in 0 until length - 1) {
            if (isSurrogatePair(this[i], this[i + 1])) {
                result--
            }
        }
        return result
    }

    @JvmStatic
    fun CharSequence.codepoints(): IntArray {
        val result = IntArray(countCodepoints())
        var readIndex = 0
        for (writeIndex in result.indices) {
            val high = this[readIndex]
            val low = this[kotlin.math.min(readIndex + 1, length - 1)]
            val isPair = isSurrogatePair(high, low)
            result[writeIndex] = if (isPair) toCodepoint(high, low) else high.code
            readIndex += if (isPair) 2 else 1
        }
        return result
    }
}