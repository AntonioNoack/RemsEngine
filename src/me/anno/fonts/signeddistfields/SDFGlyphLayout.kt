package me.anno.fonts.signeddistfields

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.fonts.signeddistfields.DrawSDFCallback
import me.anno.fonts.Font
import me.anno.fonts.GlyphLayout
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField
import me.anno.gpu.FinalRendering.isFinalRendering
import me.anno.gpu.FinalRendering.onMissingResource
import me.anno.utils.hpc.ProcessingQueue

class SDFGlyphLayout(
    font: Font, text: CharSequence,
    relativeWidthLimit: Float, maxNumLines: Int
) : GlyphLayout(font, text, relativeWidthLimit, maxNumLines) {

    // are draw-calls always expensive??
    // or buffer creation?

    var roundCorners = false

    // the performance could be improved
    // still its initialization time should be much faster than FontMesh

    private fun isTextureValid(textSDF: TextSDF?): Boolean {
        val texture = textSDF?.texture
        return texture != null && texture.isCreated()
    }

    fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawSDFCallback) {
        val roundCorners = roundCorners
        for (index in startIndex until endIndex) {
            val codepoint = getCodepoint(index)
            val x0 = getX0(index) * baseScale
            val x1 = getX1(index) * baseScale
            val y = getY(index) * baseScale
            val lineWidth = getLineWidth(index) * baseScale
            val textSDF = getTextSDF(font, codepoint, roundCorners)
                .waitFor()?.content ?: continue
            for (i in textSDF.indices) {
                val textSDF = textSDF[i]
                if (isTextureValid(textSDF)) {
                    if (drawBuffer.draw(textSDF, x0, x1, y, lineWidth)) break
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

        fun getTextSDF(font: Font, codepoint: Int, roundCorners: Boolean): AsyncCacheData<TextSDFList> {
            val key = SDFCharKey(font, codepoint, roundCorners)
            return sdfCharTex.getEntry(key, SDF_TIMEOUT_MILLIS, queue) { key2, result ->
                val textures = SignedDistanceField.createTextures(key2.font, key2.codepoint, key2.roundCorners)
                result.value = TextSDFList(textures)
            }
        }
    }
}