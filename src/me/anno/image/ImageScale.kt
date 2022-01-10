package me.anno.image

import kotlin.math.max
import kotlin.math.min

object ImageScale {

    /**
     * cuts off excess of the image
     * */
    fun scaleMin(imageWidth: Int, imageHeight: Int, minSize: Int): Pair<Int, Int> {
        return if (imageWidth < imageHeight) {
            Pair(minSize, max(1, (imageHeight * minSize + imageWidth / 2) / imageWidth))
        } else {
            Pair(max(1, (imageWidth * minSize + imageHeight / 2) / imageHeight), minSize)
        }
    }

    /**
     * cuts off excess of the image
     * */
    fun scaleMin(imageWidth: Int, imageHeight: Int, minWidth: Int, minHeight: Int): Pair<Int, Int> {
        return if (imageWidth * minHeight < imageHeight * minWidth) {
            // width is the limit
            Pair(minWidth, max(1, (imageHeight * minWidth + imageWidth / 2) / imageWidth))
        } else {
            // height is the limit
            Pair(max(1, (imageWidth * minHeight + imageHeight / 2) / imageHeight), minHeight)
        }
    }

    /**
     * adds bars on left+right/top+bottom
     * */
    fun scaleMax(imageWidth: Int, imageHeight: Int, maxSize: Int): Pair<Int, Int> {
        return if (imageWidth > imageHeight) {
            Pair(maxSize, max(1, (imageHeight * maxSize + imageWidth / 2) / imageWidth))
        } else {
            Pair(max(1, (imageWidth * maxSize + imageHeight / 2) / imageHeight), maxSize)
        }
    }

    /**
     * adds bars on left+right/top+bottom
     * */
    fun scaleMax(imageWidth: Int, imageHeight: Int, maxWidth: Int, maxHeight: Int): Pair<Int, Int> {
        return if (imageWidth * maxHeight > imageHeight * maxWidth) {
            // width is the limit
            Pair(maxWidth, max(1, (imageHeight * maxWidth + imageWidth / 2) / imageWidth))
        } else {
            // height is the limit
            Pair(max(1, (imageWidth * maxHeight + imageHeight / 2) / imageHeight), maxHeight)
        }
    }

    fun scaleMaxPreview(imageWidth: Int, imageHeight: Int, w: Int, h: Int, maxAspectRatio: Int = 5): Pair<Int, Int> {
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