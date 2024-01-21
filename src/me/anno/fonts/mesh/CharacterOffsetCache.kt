package me.anno.fonts.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.fonts.FontManager
import me.anno.maths.Maths
import me.anno.ui.base.Font
import me.anno.utils.types.Strings.joinChars
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout

class CharacterOffsetCache(val font: Font) {

    private val charDistance = HashMap<Long, Double>()// |a| = |ab| - |b|
    private val charWidth = HashMap<Int, Double>()// |a|
    val charMesh = HashMap<Int, Mesh>() // triangles of a

    private val awtFont = FontManager.getFont(font).awtFont
    private val ctx = FontRenderContext(null, true, true)
    fun getOffset(previous: Int, current: Int): Double {

        fun getLength(str: String): Double {
            if (str.isEmpty()) return 0.0
            if (' ' in str) {
                val lengthWithoutSpaces = getLength(str.replace(" ", ""))
                val spacesLength = str.count { it == ' ' } * Maths.clamp(
                    getLength("x"),
                    1.0,
                    awtFont.size.toDouble()
                ) * 0.667
                return lengthWithoutSpaces + spacesLength
            }
            val bounds = TextLayout(str, awtFont, ctx).bounds
            return bounds.maxX
        }

        fun getCharLength(char: Int): Double {
            return charWidth.getOrPut(char) {
                getLength(char.joinChars().toString())
            }
        }

        return synchronized(this) {
            charDistance.getOrPut(pair(previous, current)) {
                val bLength = getCharLength(current)
                val abLength = getLength(previous.joinChars().toString() + current.joinChars().toString())
                abLength - bLength
            }
        }
    }

    private fun pair(a: Int, b: Int): Long {
        return a.toLong().shl(32) or b.toLong().and(0xffffffffL)
    }

    companion object {
        private val caches = HashMap<Font, CharacterOffsetCache>()
        fun getOffsetCache(font: Font): CharacterOffsetCache {
            return synchronized(caches) {
                caches.getOrPut(font) {
                    CharacterOffsetCache(font)
                }
            }
        }
    }
}
