package me.anno.fonts

import me.anno.cache.AsyncCacheData
import me.anno.ecs.components.mesh.Mesh
import me.anno.fonts.signeddistfields.Contours
import me.anno.image.Image

interface IEmojiCache {

    fun contains(codepoint: Int): Boolean
    fun contains(codepoints: List<Int>): Boolean

    fun getEmojiImage(codepoints: List<Int>, fontSize: Int): AsyncCacheData<Image>
    fun getEmojiContour(codepoints: List<Int>, fontSize: Int): AsyncCacheData<Contours>
    fun getEmojiMesh(codepoints: List<Int>): AsyncCacheData<Mesh>

    companion object {

        val emojiPadding = 0.1f

        object NoEmojiSupport : IEmojiCache {
            override fun contains(codepoint: Int): Boolean = false
            override fun contains(codepoints: List<Int>): Boolean = false
            override fun getEmojiImage(codepoints: List<Int>, fontSize: Int): AsyncCacheData<Image> =
                AsyncCacheData.empty()

            override fun getEmojiContour(codepoints: List<Int>, fontSize: Int): AsyncCacheData<Contours> =
                AsyncCacheData.empty()

            override fun getEmojiMesh(codepoints: List<Int>): AsyncCacheData<Mesh> =
                AsyncCacheData.empty()
        }

        var emojiCache: IEmojiCache = NoEmojiSupport
    }
}