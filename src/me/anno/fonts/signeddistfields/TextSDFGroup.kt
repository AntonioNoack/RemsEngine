package me.anno.fonts.signeddistfields

import me.anno.cache.Cache
import me.anno.fonts.TextGroup
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.buffer.StaticBuffer
import me.anno.video.MissingFrameException
import java.awt.Font
import java.io.File

/**
 * custom character-character alignment maps by font for faster calculation
 * */
class TextSDFGroup(
    font: Font, text: String,
    charSpacing: Float,
    forceVariableBuffer: Boolean
) : TextGroup(
    font, text, charSpacing
) {

    // are draw-calls always expensive??
    // or buffer creation?
    // very long strings just are displayed char by char (you must be kidding me ;))
    var charByChar = true
    var roundCorners = false

    // the performance could be improved
    // still its initialization time should be much faster than FontMesh
    override fun draw(drawBuffer: (StaticBuffer?, TextSDF?, offset: Float) -> Unit) {
        if (codepoints.isEmpty()) return
        if (charByChar) {
            drawSlowly(drawBuffer)
        } else {
            val roundCorners = roundCorners
            val tc = Cache.getEntry(Triple(font, text, roundCorners), sdfTimeout, !isFinalRendering) {
                SignedDistanceField.create(font, text, roundCorners)
            } as? TextSDF
            val t = tc?.texture
            if (t?.isCreated == true) {
                drawBuffer(null, tc, 0f)
            } else {
                drawBuffer(null, null, 0f)
            }
        }
    }

    fun drawSlowly(drawBuffer: (StaticBuffer?, TextSDF?, offset: Float) -> Unit) {
        val roundCorners = roundCorners
        codepoints.forEachIndexed { index, codePoint ->
            val offset = (offsets[index] * baseScale).toFloat()
            val texture = Cache.getEntry(Triple(font, codePoint, roundCorners), sdfTimeout, !isFinalRendering) {
                SignedDistanceField.create(font, String(Character.toChars(codePoint)), roundCorners)
            } as? TextSDF
            drawBuffer(null, texture, offset)
        }
    }

    companion object {
        val sdfTimeout = 30_000L
    }

}