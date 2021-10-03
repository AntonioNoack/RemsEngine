package me.anno.image

object ImageScale {

    fun scale(imageWidth: Int, imageHeight: Int, maxSize: Int): Pair<Int, Int> {
        return if (imageWidth > imageHeight) {
            Pair(maxSize, (imageHeight * maxSize + imageWidth / 2) / imageWidth)
        } else {
            Pair((imageWidth * maxSize + imageHeight / 2) / imageHeight, maxSize)
        }
    }

    fun scale(imageWidth: Int, imageHeight: Int, maxWidth: Int, maxHeight: Int): Pair<Int, Int> {
        return if (imageWidth * maxHeight > imageHeight * maxWidth) {
            // width is the limit
            Pair(maxWidth, (imageHeight * maxWidth + imageWidth / 2) / imageWidth)
        } else {
            // height is the limit
            Pair((imageWidth * maxHeight + imageHeight / 2) / imageHeight, maxHeight)
        }
    }

}