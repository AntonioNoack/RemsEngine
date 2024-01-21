package me.anno.fonts.signeddistfields

import me.anno.cache.CacheData
import me.anno.fonts.TextGroup
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.texture.TextureCache
import me.anno.fonts.Font
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.types.Strings.joinChars
import me.anno.video.MissingFrameException

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
            val cacheData = TextureCache.getEntry(key, sdfTimeout, queue) {
                CacheData(SignedDistanceField.createTexture(font, text, roundCorners))
            } as? CacheData<*>
            if (isFinalRendering && cacheData == null) throw MissingFrameException("")
            val textSDF = cacheData?.value as? TextSDF
            val texture = textSDF?.texture
            if (texture?.wasCreated == true) {
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
            val codePoint = codepoints[index]
            val offset = (offsets[index] * baseScale).toFloat()
            val key = SDFCharKey(font, codePoint, roundCorners)
            val cacheData = TextureCache.getEntry(key, sdfTimeout, queue) { key2 ->
                val charAsText = key2.codePoint.joinChars()
                val texture = SignedDistanceField.createTexture(key2.font, charAsText, key2.roundCorners)
                CacheData(texture)
            } as? CacheData<*>
            if (isFinalRendering && cacheData == null) throw MissingFrameException("")
            val textSDF = cacheData?.value as? TextSDF
            drawBuffer.draw(null, textSDF, offset)
        }
    }

    companion object {
        val sdfTimeout = 30_000L
        val queue = ProcessingQueue("SDFText")
    }
}