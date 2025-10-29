package me.anno.image.aseprite

import me.anno.image.ImageDrawing.mixRGB
import me.anno.image.raw.IntImage
import me.anno.maths.Maths.clamp
import me.anno.utils.Color.a
import me.anno.utils.Color.a01

object AsepriteToImage {

    fun AseSprite.createImages(): List<IntImage> {
        return frames.map { frame -> frameToImage(frame) }
    }

    fun AseSprite.frameToImage(frame: AseFrame): IntImage {
        val width = header.width
        val height = header.height
        val result = IntImage(width, height, true)

        // Sort cels by (layerIndex + zIndex), as per NOTE.5
        val sortedCels = frame.cels.sortedWith(
            compareBy({ it.layerIndex + it.zIndex }, { it.zIndex })
        )

        for (cel in sortedCels) {
            val pixels = cel.imageData ?: continue
            val layer = layers.getOrNull(cel.layerIndex) ?: continue
            if (!layer.isVisible) continue

            val opacity = clamp(cel.opacity * layer.opacity / 255f)
            val w = pixels.width
            val h = pixels.height

            for (cy in 0 until h) {
                val y = cel.y + cy
                if (y !in 0..<height) continue
                for (cx in 0 until w) {
                    val x = cel.x + cx
                    if (x !in 0..<width) continue

                    val src = pixels.getRGB(cx, cy)
                    if (src.a() == 0) continue

                    result.mixRGB(x, y, src, opacity * src.a01())
                }
            }
        }

        return result
    }
}