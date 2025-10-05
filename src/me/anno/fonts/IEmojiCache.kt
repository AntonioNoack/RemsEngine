package me.anno.fonts

import me.anno.cache.AsyncCacheData
import me.anno.image.Image

interface IEmojiCache {

    fun contains(codepoint: Int): Boolean
    fun contains(codepoints: List<Int>): Boolean

    fun getEmojiImage(codepoints: List<Int>, fontSize: Int): AsyncCacheData<Image>

    companion object {
        val noEmojiSupport = object : IEmojiCache {
            override fun contains(codepoint: Int): Boolean = false
            override fun contains(codepoints: List<Int>): Boolean = false
            override fun getEmojiImage(codepoints: List<Int>, fontSize: Int): AsyncCacheData<Image> =
                AsyncCacheData.empty()
        }
    }
}