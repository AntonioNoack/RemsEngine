package me.anno.ui.base.components

import me.anno.image.ImageScale
import org.joml.Vector2i

enum class StretchModes(val id: Int) {
    OVERFLOW(0),
    PADDING(1),
    STRETCH(2);

    fun stretch(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Vector2i {
        return when (this) {
            OVERFLOW -> ImageScale.scaleMin(srcW, srcH, dstW, dstH)
            PADDING -> ImageScale.scaleMax(srcW, srcH, dstW, dstH)
            STRETCH -> Vector2i(dstW, dstH)
        }
    }
}
