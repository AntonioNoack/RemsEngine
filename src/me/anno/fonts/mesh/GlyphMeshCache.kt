package me.anno.fonts.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.fonts.Codepoints
import me.anno.fonts.Font
import me.anno.fonts.IEmojiCache
import speiger.primitivecollections.IntToObjectHashMap

object GlyphMeshCache {
    private val meshCaches = HashMap<TriangleFontKey, IntToObjectHashMap<Mesh>>() // triangles of a

    private data class TriangleFontKey(
        val name: String,
        val size: Float,
        val isBold: Boolean,
        val isItalic: Boolean
    )

    fun getMeshCache(font: Font): IntToObjectHashMap<Mesh> {
        return synchronized(meshCaches) {
            val key = TriangleFontKey(font.name, font.size, font.isBold, font.isItalic)
            meshCaches.getOrPut(key) {
                IntToObjectHashMap()
            }
        }
    }

    fun getMesh(meshCache: IntToObjectHashMap<Mesh>, font: Font, codepoint: Int): Mesh {
       return synchronized(meshCache) {
            meshCache.getOrPut(codepoint) {
                if (Codepoints.isEmoji(codepoint)) {
                    val emojiId = Codepoints.getEmojiId(codepoint)
                    IEmojiCache.emojiCache.getEmojiMesh(emojiId).waitFor()!!
                } else { // normal character
                    TextMesh(font, codepoint).mesh
                }
            }
        }
    }
}