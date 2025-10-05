package me.anno.fonts

import me.anno.maths.Packing.pack64
import me.anno.utils.assertions.assertEquals

/**
 * extracting characters from UTF-16 strings, because Java and Kotlin chose them,
 * and we want emoji support; however Kotlin doesn't support .codePoints(), so we had to add it ourselves
 * */
object Codepoints {

    // join multi-char emojis, too? -> that seems very complicated
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
    fun CharSequence.codepoints(dstLength: Int = countCodepoints()): IntArray {
        val result = IntArray(dstLength)
        var readIndex = 0
        for (writeIndex in 0 until dstLength) {
            val high = this[readIndex]
            val low = this[kotlin.math.min(readIndex + 1, length - 1)]
            val isPair = isSurrogatePair(high, low)
            result[writeIndex] = if (isPair) toCodepoint(high, low) else high.code
            readIndex += if (isPair) 2 else 1
        }
        return result
    }

    @JvmStatic
    fun IntArray.markEmojisInCodepoints(): IntArray {
        var i = 0
        val cache = IEmojiCache.emojiCache
        val tmp = ArrayList<Int>()
        while (i < size - 1) {
            val c0 = this[i]
            val c1 = this[i + 1]
            tmp.clear()
            tmp.add(c0)
            tmp.add(c1)
            if (cache.contains(tmp)) {
                var j = i + 2
                while (j < size) {
                    tmp.add(this[j++])
                    if (!cache.contains(tmp)) {
                        tmp.removeLast()
                        j--
                        break
                    }
                }
                // i .. j is a sequence -> mark it as such by 2s-complementing the ints
                for (k in i until j - 1) {
                    this[k] = -1 - this[k]
                }
                i = j + 1 // next char is j+1
            } else i++
        }
        return this
    }

    @JvmStatic
    fun IntArray.getRangesFromMarkedEmojis(): LongArray {
        val dst = LongArray(countRangesFromMarkedEmojis())
        var i = 0
        var k = 0
        while (i < size) {
            val i0 = i
            if (this[i] < 0) {
                while (this[++i] < 0) {
                    // wait
                }
                // this[i] >= 0
            }
            println("adding($i0,$i) by ${toList()}")
            dst[k++] = pack64(i0, i)
            i++
        }
        assertEquals(dst.size, k)
        return dst
    }

    @JvmStatic
    fun IntArray.countRangesFromMarkedEmojis(): Int {
        return count { it >= 0 }
    }
}