package me.anno.image

import me.anno.maths.Maths.roundDiv
import me.anno.utils.structures.tuples.IntPair
import kotlin.math.max
import kotlin.math.min

object ImageScale {

    /**
     * cuts off excess of the image
     * */
    @JvmStatic
    fun scaleMin(imageWidth: Int, imageHeight: Int, minSize: Int): IntPair {
        return scaleMin(imageWidth, imageHeight, minSize, minSize)
    }

    /**
     * cuts off excess of the image
     * */
    @JvmStatic
    fun scaleMin(imageWidth: Int, imageHeight: Int, minWidth: Int, minHeight: Int): IntPair {
        return if (imageWidth * minHeight < imageHeight * minWidth) {
            // width is the limit
            IntPair(minWidth, max(1, (imageHeight * minWidth + imageWidth / 2) / imageWidth))
        } else {
            // height is the limit
            IntPair(max(1, (imageWidth * minHeight + imageHeight / 2) / imageHeight), minHeight)
        }
    }

    /**
     * adds bars on left+right/top+bottom
     * */
    @JvmStatic
    fun scaleMax(imageWidth: Int, imageHeight: Int, maxSize: Int): IntPair {
        return scaleMax(imageWidth, imageHeight, maxSize, maxSize)
    }

    /**
     * adds bars on left+right/top+bottom
     * */
    @JvmStatic
    fun scaleMax(imageWidth: Int, imageHeight: Int, maxWidth: Int, maxHeight: Int): IntPair {
        return if (imageWidth * maxHeight > imageHeight * maxWidth) {
            // width is the limit
            IntPair(maxWidth, max(1, roundDiv(imageHeight * maxWidth, imageWidth)))
        } else {
            // height is the limit
            IntPair(max(1, roundDiv(imageWidth * maxHeight, imageHeight)), maxHeight)
        }
    }

    @JvmStatic
    fun scaleMaxPreview(imageWidth: Int, imageHeight: Int, w: Int, h: Int, maxAspectRatio: Int): IntPair {
        return when {
            // not too tall or too wide
            max(imageWidth, imageHeight) < maxAspectRatio * min(imageWidth, imageHeight) -> {
                scaleMax(imageWidth, imageHeight, w, h)
            }
            // wide
            imageWidth > imageHeight -> {
                scaleMax(maxAspectRatio, 1, w, h)
            }
            // tall
            else -> {
                scaleMax(1, maxAspectRatio, w, h)
            }
        }
    }
}