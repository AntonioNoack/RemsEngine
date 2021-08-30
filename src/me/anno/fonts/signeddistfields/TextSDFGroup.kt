package me.anno.fonts.signeddistfields

import me.anno.cache.CacheData
import me.anno.fonts.TextGroup
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.buffer.StaticBuffer
import me.anno.image.ImageGPUCache
import me.anno.utils.hpc.ProcessingQueue
import me.anno.video.MissingFrameException
import java.awt.Font

/**
 * custom character-character alignment maps by font for faster calculation
 * */
class TextSDFGroup(
    font: Font, text: String,
    charSpacing: Float
) : TextGroup(
    font, text, charSpacing.toDouble()
) {

    // are draw-calls always expensive??
    // or buffer creation?
    // very long strings just are displayed char by char (you must be kidding me ;))
    var charByChar = true
    var roundCorners = false

    // the performance could be improved
    // still its initialization time should be much faster than FontMesh
    override fun draw(
        startIndex: Int, endIndex: Int,
        drawBuffer: (StaticBuffer?, TextSDF?, offset: Float) -> Unit
    ) {
        if (codepoints.isEmpty()) return
        if (charByChar || startIndex > 0 || endIndex < codepoints.size) {
            drawSlowly(startIndex, endIndex, drawBuffer)
        } else {
            val roundCorners = roundCorners
            val key = SDFStringKey(font, text, roundCorners)
            val cacheData = ImageGPUCache.getEntry(key, sdfTimeout, queue) {
                CacheData(SignedDistanceField.createTexture(font, text, roundCorners))
            } as? CacheData<*>
            if (isFinalRendering && cacheData == null) throw MissingFrameException("")
            val textSDF = cacheData?.value as? TextSDF
            val texture = textSDF?.texture
            if (texture?.isCreated == true) {
                drawBuffer(null, textSDF, 0f)
            } else {
                drawBuffer(null, null, 0f)
            }
        }
    }

    private fun drawSlowly(
        startIndex: Int, endIndex: Int,
        drawBuffer: (StaticBuffer?, TextSDF?, offset: Float) -> Unit
    ) {
        val roundCorners = roundCorners
        for (index in startIndex until endIndex) {
            val codePoint = codepoints[index]
            val offset = (offsets[index] * baseScale).toFloat()
            val key = SDFCharKey(font, codePoint, roundCorners)
            val cacheData = ImageGPUCache.getEntry(key, sdfTimeout, queue) {
                val charAsText = String(Character.toChars(it.codePoint))
                val texture = SignedDistanceField.createTexture(it.font, charAsText, it.roundCorners)
                CacheData(texture)
            } as? CacheData<*>
            if (isFinalRendering && cacheData == null) throw MissingFrameException("")
            val textSDF = cacheData?.value as? TextSDF
            drawBuffer(null, textSDF, offset)
        }
    }

    companion object {
        val sdfTimeout = 30_000L
        val queue = ProcessingQueue("SDFText")
    }

}