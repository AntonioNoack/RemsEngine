package me.anno.fonts.signeddistfields

import me.anno.fonts.Font
import me.anno.fonts.TextGroup
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField
import me.anno.gpu.FinalRendering.isFinalRendering
import me.anno.gpu.FinalRendering.onMissingResource
import me.anno.gpu.texture.TextureCache
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
    override fun draw(
        startIndex: Int, endIndex: Int,
        drawBuffer: DrawBufferCallback
    ) {
        if (codepoints.isEmpty()) return
        if (charByChar || startIndex > 0 || endIndex < codepoints.size) {
            drawSlowly(startIndex, endIndex, drawBuffer)
        } else {
            val roundCorners = roundCorners
            val key = SDFStringKey(font, text, roundCorners)
            val textSDF = TextureCache.getEntry(key, SDF_TIMEOUT_MILLIS, queue) {
                SignedDistanceField.createTexture(font, text, roundCorners)
            } as? TextSDF
            if (isFinalRendering && textSDF == null) {
                return onMissingResource("TextSDFGroup", text)
            }
            val texture = textSDF?.texture
            if (texture != null && texture.isCreated()) {
                drawBuffer.draw(null, textSDF, 0f)
            } else {
                drawBuffer.draw(null, null, 0f)
            }
        }
    }

    private fun drawSlowly(
        startIndex: Int, endIndex: Int,
        drawBuffer: DrawBufferCallback
    ) {
        val roundCorners = roundCorners
        for (index in startIndex until endIndex) {
            val codepoint = codepoints[index]
            val offset = (offsets[index] * baseScale).toFloat()
            val textSDF = getTextSDF(codepoint, font, roundCorners)
            if (isFinalRendering && textSDF == null) {
                return onMissingResource("TextSDFGroup", codepoint)
            }
            drawBuffer.draw(null, textSDF, offset)
        }
    }

    companion object {
        private const val SDF_TIMEOUT_MILLIS = 30_000L
        val queue = ProcessingQueue("SDFText")

        fun getTextSDF(codepoint: Int, font: Font, roundCorners: Boolean): TextSDF? {
            val key = SDFCharKey(font, codepoint, roundCorners)
            return TextureCache.getEntry(key, SDF_TIMEOUT_MILLIS, queue) { key2 ->
                val charAsText = key2.codePoint.joinChars()
                SignedDistanceField.createTexture(key2.font, charAsText, key2.roundCorners)
            } as? TextSDF
        }
    }
}