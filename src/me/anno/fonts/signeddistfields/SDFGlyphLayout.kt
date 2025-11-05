package me.anno.fonts.signeddistfields

import me.anno.cache.CacheSection
import me.anno.cache.Promise
import me.anno.fonts.Font
import me.anno.fonts.GlyphLayout
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField
import me.anno.gpu.FinalRendering.isFinalRendering
import me.anno.gpu.FinalRendering.onMissingResource
import me.anno.utils.hpc.ProcessingQueue
import kotlin.math.max
import kotlin.math.min

/**
 * Converts a font and text into an SDF-list for each glyph
 * */
class SDFGlyphLayout(
    font: Font, text: CharSequence,
    relativeWidthLimit: Float, maxNumLines: Int,
    var roundCorners: Boolean = false
) : GlyphLayout(font, text, relativeWidthLimit, maxNumLines) {

    private fun isTextureValid(textSDF: TextSDF?): Boolean {
        val texture = textSDF?.texture
        return texture != null && texture.isCreated()
    }

    fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawSDFCallback) {
        val roundCorners = roundCorners
        for (glyphIndex in max(startIndex, 0) until min(endIndex, size)) {
            val codepoint = getCodepoint(glyphIndex)
            val x0 = getX0(glyphIndex) * baseScale
            val x1 = getX1(glyphIndex) * baseScale
            val y = getY(glyphIndex, font) * baseScale
            val lineWidth = getLineWidth(glyphIndex) * baseScale
            val textSDF = getTextSDF(font, codepoint, roundCorners)
                .waitFor()?.content ?: continue
            for (i in textSDF.indices) {
                val textSDF = textSDF[i]
                if (isTextureValid(textSDF)) {
                    if (drawBuffer.draw(textSDF, x0, x1, y, lineWidth, glyphIndex)) break
                } else if (isFinalRendering) {
                    onMissingResource("TextSDFGroup", codepoint)
                    return
                }
            }
        }
    }

    companion object {

        private const val SDF_TIMEOUT_MILLIS = 30_000L

        private val queue = ProcessingQueue("SDFText")
        private val sdfCharTex = CacheSection<SDFCharKey, TextSDFList>("SDFCharTex")

        fun getTextSDF(font: Font, codepoint: Int, roundCorners: Boolean): Promise<TextSDFList> {
            val key = SDFCharKey(font, codepoint, roundCorners)
            return sdfCharTex.getEntry(key, SDF_TIMEOUT_MILLIS, queue) { key2, result ->
                val textures = SignedDistanceField.createTextures(key2.font, key2.codepoint, key2.roundCorners)
                result.value = TextSDFList(textures)
            }
        }
    }
}