package me.anno.image

import me.anno.maths.Maths.roundDiv
import org.joml.Vector2i
import kotlin.math.max
import kotlin.math.min

/**
 * When rendering images, the present aspect ratio is often different from the canvas.
 * This class provides utility functions for scaling images such that their aspect ratio remains intact,
 * and they either fit the canvas OR the overlap gets cut off and everything is filled.
 * */
object ImageScale {

    /**
     * cuts off excess of the image
     * */
    @JvmStatic
    fun scaleMin(imageWidth: Int, imageHeight: Int, minSize: Int): Vector2i {
        return scaleMin(imageWidth, imageHeight, minSize, minSize)
    }

    /**
     * cuts off excess of the image
     * */
    @JvmStatic
    fun scaleMin(imageWidth: Int, imageHeight: Int, minWidth: Int, minHeight: Int): Vector2i {
        return if (imageWidth * minHeight < imageHeight * minWidth) {
            // width is the limit
            Vector2i(minWidth, max(1, (imageHeight * minWidth + imageWidth / 2) / imageWidth))
        } else {
            // height is the limit
            Vector2i(max(1, (imageWidth * minHeight + imageHeight / 2) / imageHeight), minHeight)
        }
    }

    /**
     * adds bars on left+right/top+bottom
     * */
    @JvmStatic
    fun scaleMax(imageWidth: Int, imageHeight: Int, maxSize: Int): Vector2i {
        return scaleMax(imageWidth, imageHeight, maxSize, maxSize)
    }

    /**
     * adds bars on left+right/top+bottom
     * */
    @JvmStatic
    fun scaleMax(imageWidth: Int, imageHeight: Int, maxWidth: Int, maxHeight: Int): Vector2i {
        return if (imageWidth * maxHeight > imageHeight * maxWidth) {
            // width is the limit
            Vector2i(maxWidth, max(1, roundDiv(imageHeight * maxWidth, imageWidth)))
        } else {
            // height is the limit
            Vector2i(max(1, roundDiv(imageWidth * maxHeight, imageHeight)), maxHeight)
        }
    }

    @JvmStatic
    fun scaleMaxPreview(imageWidth: Int, imageHeight: Int, w: Int, h: Int, maxAspectRatio: Int): Vector2i {
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