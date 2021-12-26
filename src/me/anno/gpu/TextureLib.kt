package me.anno.gpu

import me.anno.cache.data.ICacheData
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Clock

object TextureLib {

    class IndestructibleTexture2D(name: String, w: Int, h: Int) : Texture2D(name, w, h, 1) {
        override fun destroy() {}
        fun doDestroy() {
            super.destroy()
        }
    }

    val invisibleTexture = IndestructibleTexture2D("invisible", 1, 1)
    val whiteTexture = IndestructibleTexture2D("white", 1, 1)
    val stripeTexture = IndestructibleTexture2D("stripes", 5, 1)
    val colorShowTexture = IndestructibleTexture2D("color-show", 2, 2)
    val normalTexture = IndestructibleTexture2D("normal", 1, 1)
    val blackTexture = IndestructibleTexture2D("black", 1, 1)

    private var session = 0

    object nullTexture : ICacheData {
        override fun destroy() {}
    }

    fun init() {
        val tick = Clock()
        if (session != OpenGL.session) {
            session = OpenGL.session
            // mark all textures as not-yet-created
            invisibleTexture.createdW = 0
            whiteTexture.createdW = 0
            stripeTexture.createdW = 0
            colorShowTexture.createdW = 0
            normalTexture.createdW = 0
            blackTexture.createdW = 0
        }
        invisibleTexture.createRGBA(ByteArray(4) { 0.toByte() }, false)
        whiteTexture.createRGBA(
            byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte()), false
        )
        stripeTexture.createMonochrome(
            byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte()), false
        )
        colorShowTexture.createRGBA(
            intArrayOf(
                255, 255, 255, 127, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 127
            ).map { it.toByte() }.toByteArray(), false
        )
        normalTexture.createRGBA(byteArrayOf(127, 127, 255.toByte(), 255.toByte()), false)
        blackTexture.createRGBA(byteArrayOf(0, 0, 0, 255.toByte()), false)
        tick.stop("creating default textures")
    }

    fun bindWhite(index: Int): Boolean {
        return whiteTexture.bind(index, whiteTexture.filtering, whiteTexture.clamping!!)
    }

    fun destroy() {
        invisibleTexture.doDestroy()
        whiteTexture.doDestroy()
        stripeTexture.doDestroy()
        colorShowTexture.doDestroy()
        normalTexture.doDestroy()
        blackTexture.doDestroy()
    }

}