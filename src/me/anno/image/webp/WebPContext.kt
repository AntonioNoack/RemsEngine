package me.anno.image.webp

import java.nio.ByteBuffer

class WebPContext : VP8Context() {

    lateinit var data: ByteBuffer
    var alphaFrame: AVFrame? = null
    var avPacket: AVPacket? = null
    // avcodeccontext

    var isInited = false
    var hasAlpha = false
    var alphaCompression = 0
    var alphaFilter = 0
    var alphaData: ByteArray? = null
    var alphaDataSize = 0
    var hasExif = false
    var hasIccp = false
    var width = 0
    var height = 0
    var lossless = false

    var numTransforms = 0
    var transforms = IntArray(4)

    var reducedWidth = 0
    var numHuffmanGroups = 0
    var images = Array(5) { ImageContext() } // image context for each image role

}