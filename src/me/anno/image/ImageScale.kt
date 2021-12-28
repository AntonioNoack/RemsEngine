package me.anno.image

object ImageScale {

    /**
     * cuts off excess of the image
     * */
    fun scaleMin(imageWidth: Int, imageHeight: Int, minSize: Int): Pair<Int, Int> {
        return if (imageWidth < imageHeight) {
            Pair(minSize, (imageHeight * minSize + imageWidth / 2) / imageWidth)
        } else {
            Pair((imageWidth * minSize + imageHeight / 2) / imageHeight, minSize)
        }
    }

    /**
     * cuts off excess of the image
     * */
    fun scaleMin(imageWidth: Int, imageHeight: Int, minWidth: Int, minHeight: Int): Pair<Int, Int> {
        return if (imageWidth * minHeight < imageHeight * minWidth) {
            // width is the limit
            Pair(minWidth, (imageHeight * minWidth + imageWidth / 2) / imageWidth)
        } else {
            // height is the limit
            Pair((imageWidth * minHeight + imageHeight / 2) / imageHeight, minHeight)
        }
    }

    /**
     * adds bars on left+right/top+bottom
     * */
    fun scaleMax(imageWidth: Int, imageHeight: Int, maxSize: Int): Pair<Int, Int> {
        return if (imageWidth > imageHeight) {
            Pair(maxSize, (imageHeight * maxSize + imageWidth / 2) / imageWidth)
        } else {
            Pair((imageWidth * maxSize + imageHeight / 2) / imageHeight, maxSize)
        }
    }

    /**
     * adds bars on left+right/top+bottom
     * */
    fun scaleMax(imageWidth: Int, imageHeight: Int, maxWidth: Int, maxHeight: Int): Pair<Int, Int> {
        return if (imageWidth * maxHeight > imageHeight * maxWidth) {
            // width is the limit
            Pair(maxWidth, (imageHeight * maxWidth + imageWidth / 2) / imageWidth)
        } else {
            // height is the limit
            Pair((imageWidth * maxHeight + imageHeight / 2) / imageHeight, maxHeight)
        }
    }

}