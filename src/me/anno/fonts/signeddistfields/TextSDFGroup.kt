package me.anno.fonts.signeddistfields

import me.anno.cache.CacheSection
import me.anno.fonts.Font
import me.anno.fonts.TextGroup
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField
import me.anno.gpu.FinalRendering.isFinalRendering
import me.anno.gpu.FinalRendering.onMissingResource
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.types.Strings.joinChars

class TextSDFGroup(font: Font, text: CharSequence, charSpacing: Double) :
    TextGroup(font, text, charSpacing) {

    // are draw-calls always expensive??
    // or buffer creation?
    // very long strings just are displayed char by char (you must be kidding me ;))
    var charByChar = true
    var roundCorners = false

    // the performance could be improved
    // still its initialization time should be much faster than FontMesh
    override fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawBufferCallback) {
        if (startIndex >= endIndex) return
        if (shouldRunSlowly(startIndex, endIndex)) {
            drawSlowly(startIndex, endIndex, drawBuffer)
        } else {
            val textSDF = getTextSDF(font, text, roundCorners)
            if (textSDF == TextSDF.empty) return
            if (isTextureValid(textSDF)) {
                drawBuffer.draw(null, textSDF, 0f)
            } else if (isFinalRendering) {
                onMissingResource("TextSDFGroup", text)
            }
        }
    }

    private fun shouldRunSlowly(startIndex: Int, endIndex: Int): Boolean {
        return charByChar || startIndex > 0 || endIndex < codepoints.size
    }

    private fun isTextureValid(textSDF: TextSDF?): Boolean {
        val texture = textSDF?.texture
        return texture != null && texture.isCreated()
    }

    private fun drawSlowly(startIndex: Int, endIndex: Int, drawBuffer: DrawBufferCallback) {
        val roundCorners = roundCorners
        for (index in startIndex until endIndex) {
            val codepoint = codepoints[index]
            val offset = (offsets[index] * baseScale).toFloat()
            val textSDF = getTextSDF(font, codepoint, roundCorners)
            if (textSDF == TextSDF.empty) continue
            if (isTextureValid(textSDF)) {
                drawBuffer.draw(null, textSDF, offset)
            } else if (isFinalRendering) {
                onMissingResource("TextSDFGroup", codepoint)
                return
            }
        }
    }

    companion object {
        private const val SDF_TIMEOUT_MILLIS = 30_000L
        val queue = ProcessingQueue("SDFText")

        private val sdfStringTex = CacheSection<SDFStringKey, TextSDF>("SDFStringTex")
        private val sdfCharTex = CacheSection<SDFCharKey, TextSDF>("SDFCharTex")

        fun getTextSDF(font: Font, text: CharSequence, roundCorners: Boolean): TextSDF? {
            val key = SDFStringKey(font, text, roundCorners)
            return sdfStringTex.getEntry(key, SDF_TIMEOUT_MILLIS, queue) { key2, result ->
                result.value = SignedDistanceField.createTexture(key2.font, key2.text, key2.roundCorners)
            }.waitFor()
        }

        fun getTextSDF(font: Font, codepoint: Int, roundCorners: Boolean): TextSDF? {
            val key = SDFCharKey(font, codepoint, roundCorners)
            return sdfCharTex.getEntry(key, SDF_TIMEOUT_MILLIS, queue) { key2, result ->
                val charAsText = key2.codePoint.joinChars()
                result.value = SignedDistanceField.createTexture(key2.font, charAsText, key2.roundCorners)
            }.waitFor()
        }
    }
}