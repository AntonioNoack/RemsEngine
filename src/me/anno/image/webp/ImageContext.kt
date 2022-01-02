package me.anno.image.webp

class ImageContext {
    // role: ImageRole
    var frame: AVFrame? = null
    var colorCacheBits = 0
    var colorCache: IntArray? = null
    val numHuffmanGroups = 0
    var huffmanGroups: Array<HuffReader>? = null
    /**
     * relative size compared to primary image, log2,
     * for IMAGE_ROLE_COLOR_INDEXING with <= 16 colors,
     * this is log2 of #pixels/byte in the primary image
     * */
    var sizeReduction = 0
    var isAlphaPrimary = false
}