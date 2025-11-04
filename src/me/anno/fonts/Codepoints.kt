package me.anno.fonts

import me.anno.utils.InternalAPI
import me.anno.utils.hpc.threadLocal

/**
 * Extracting characters from UTF-16 strings, because Java and Kotlin chose them, and we want emoji support;
 * however Kotlin doesn't support .codePoints(), so we had to add it ourselves;
 * also, this class now supports multi-character emojis
 * */
object Codepoints {

    const val EMOJI_OFFSET = 0x110000

    const val VARIATION_SELECTOR_0 = 0xFE00
    const val VARIATION_SELECTOR_16 = 0xFE0F

    @JvmStatic
    fun isEmoji(codepoint: Int): Boolean {
        return codepoint >= EMOJI_OFFSET
    }

    @JvmStatic
    fun getEmojiId(codepoint: Int): Int {
        return codepoint - EMOJI_OFFSET
    }

    @JvmStatic
    fun isVariationSelector(codepoint: Int): Boolean {
        return codepoint in VARIATION_SELECTOR_0..VARIATION_SELECTOR_16
    }

    @JvmStatic
    fun emojiIdToCodepoint(emojiId: Int): Int {
        return emojiId + EMOJI_OFFSET
    }

    // join multi-char emojis, too? -> that seems very complicated
    @JvmStatic
    fun isHighSurrogate(high: Char): Boolean {
        return high.code in 0xd800..0xdbff
    }

    @JvmStatic
    fun isLowSurrogate(low: Char): Boolean {
        return low.code in 0xdc00..0xdfff
    }

    /**
     * Surrogate pairs encode the codepoints 0x10000 to 0x10FFFF
     * */
    @JvmStatic
    fun isSurrogatePair(high: Char, low: Char): Boolean {
        return isHighSurrogate(high) and isLowSurrogate(low)
    }

    /**
     * Surrogate pairs encode the codepoints 0x10000 to 0x10FFFF
     * */
    @JvmStatic
    fun toCodepoint(high: Char, low: Char): Int {
        return (high.code shl 10) + low.code - 0x35fdc00
    }

    @JvmStatic
    private val tmpChain = threadLocal { ArrayList<Int>() }

    @InternalAPI
    fun getTmpChain(): ArrayList<Int> = tmpChain.get()

    @JvmStatic
    fun CharSequence.countCodepoints(joinEmojis: Boolean = true): Int {
        var counter = 0
        forEachCodepoint(joinEmojis) { counter++ }
        return counter
    }

    @JvmStatic
    inline fun CharSequence.forEachUTF8Codepoint(callback: (Int) -> Unit) {
        forEachCodepoint(false, callback)
    }

    /**
     * joinEmojis = true -> for text rendering, cursor positions, etc.
     * joinEmojis = false -> for writing strings, just returning UTF-8 codepoints like the original method.
     * */
    @JvmStatic
    inline fun CharSequence.forEachCodepoint(joinEmojis: Boolean, callback: (Int) -> Unit) {
        val currEmoji = getTmpChain()
        val emojiCache = IEmojiCache.emojiCache
        var i = 0
        while (i < length) {

            // decode next codepoint
            val char = this[i++]
            var codepoint = char.code
            if (i < length && isSurrogatePair(char, this[i])) {
                codepoint = toCodepoint(char, this[i])
                i++
            }

            // todo can we handle variation selectors inside text?
            if (joinEmojis && currEmoji.isNotEmpty()) {
                currEmoji.add(codepoint)
                if (emojiCache.contains(currEmoji)) continue // emoji continues
                currEmoji.remove(codepoint) // emoji ends here
                val emojiId = emojiCache.getEmojiId(currEmoji)
                callback(emojiIdToCodepoint(emojiId))
                currEmoji.clear()
                // codepoint will be handled normally
            }

            when {
                !joinEmojis -> callback(codepoint)
                // variation selector outside an emoji -> can be immediately ignored
                isVariationSelector(codepoint) -> {}
                // keycap emoji with exactly two symbols ->
                // can be handled exactly like emojis, but emojiCache.contains would return false
                (i < length && emojiCache.isKeycapEmoji(codepoint, this[i].code)) ||
                        emojiCache.contains(codepoint) -> {
                    // an emoji starts here
                    currEmoji.add(codepoint)
                }
                (i + 1 < length && this[i].code == VARIATION_SELECTOR_16 &&
                        emojiCache.isKeycapEmoji(codepoint, this[i + 1].code)) -> {
                    // keycap with 0xfe0f
                    currEmoji.add(codepoint)
                    i++ // skip VARIATION_SELECTOR_16
                }
                else -> callback(codepoint)
            }
        }

        if (currEmoji.isNotEmpty()) {
            val emojiId = emojiCache.getEmojiId(currEmoji)
            callback(emojiIdToCodepoint(emojiId))
            currEmoji.clear()
        }
    }

    @JvmStatic
    fun CharSequence.codepoints(joinEmojis: Boolean = true, dstLength: Int = countCodepoints()): IntArray {
        val result = IntArray(dstLength)
        var writeIndex = 0
        forEachCodepoint(joinEmojis) { codepoint -> result[writeIndex++] = codepoint }
        return result
    }

    @JvmStatic
    inline fun Int.forEachChar(callback: (Char) -> Unit) {
        if (isEmoji(this)) {
            val emojiId = getEmojiId(this)
            val string = IEmojiCache.emojiCache.getEmojiString(emojiId)
            for (i in string.indices) callback(string[i])
        } else if (Character.isBmpCodePoint(this)) {
            callback(toChar())
        } else {
            callback(Character.highSurrogate(this))
            callback(Character.lowSurrogate(this))
        }
    }
}