package me.anno.fonts

import me.anno.cache.Promise
import me.anno.ecs.components.mesh.Mesh
import me.anno.fonts.signeddistfields.Contours
import me.anno.image.Image

interface IEmojiCache {

    fun contains(codepoint: Int): Boolean
    fun contains(codepoints: List<Int>): Boolean = getEmojiId(codepoints) >= 0
    fun getEmojiId(codepoints: List<Int>): Int

    fun getEmojiImage(emojiId: Int, fontSize: Int): Promise<Image>
    fun getEmojiContours(emojiId: Int, fontSize: Int): Promise<Contours>
    fun getEmojiMesh(emojiId: Int): Promise<Mesh>

    fun isKeycapEmoji(cp0: Int, cp1: Int): Boolean {
        return (cp0 in '0'.code..'9'.code || cp0.toChar() in "*#") &&
                cp1 == KEYCAP_EMOJI &&
                contains(listOf(cp0, KEYCAP_EMOJI))
    }

    /**
     * Return the easiest (shortest) string representation of an emoji by its ID
     * */
    fun getEmojiString(emojiId: Int): String

    companion object {

        private const val KEYCAP_EMOJI = 0x20E3

        val emojiPadding = 0.1f

        object NoEmojiSupport : IEmojiCache {
            override fun contains(codepoint: Int): Boolean = false
            override fun contains(codepoints: List<Int>): Boolean = false
            override fun getEmojiId(codepoints: List<Int>): Int = -1
            override fun getEmojiString(emojiId: Int): String = "?"

            override fun getEmojiImage(emojiId: Int, fontSize: Int): Promise<Image> =
                Promise.empty()

            override fun getEmojiContours(emojiId: Int, fontSize: Int): Promise<Contours> =
                Promise.empty()

            override fun getEmojiMesh(emojiId: Int): Promise<Mesh> =
                Promise.empty()
        }

        var emojiCache: IEmojiCache = NoEmojiSupport
    }
}