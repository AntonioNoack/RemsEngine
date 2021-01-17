package me.anno.gpu

import me.anno.cache.data.ICacheData
import me.anno.gpu.texture.Texture2D

object TextureLib {

    class IndestructableTexture2D(name: String, w: Int, h: Int) : Texture2D(name, w, h, 1) {
        override fun destroy() {}
    }

    val invisibleTexture = IndestructableTexture2D("invisible", 1, 1)
    val whiteTexture = IndestructableTexture2D("white", 1, 1)
    val stripeTexture = IndestructableTexture2D("stripes", 5, 1)
    val colorShowTexture = IndestructableTexture2D("color-show", 2, 2)
    val normalTexture = IndestructableTexture2D("normal", 1, 1)
    val blackTexture = IndestructableTexture2D("black", 1, 1)

    object nullTexture: ICacheData { override fun destroy() {} }

    fun init() {
        invisibleTexture.createRGBA(ByteArray(4) { 0.toByte() })
        whiteTexture.createRGBA(
            byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
        )
        stripeTexture.createMonochrome(
            byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
        )
        colorShowTexture.createRGBA(
            intArrayOf(
                255, 255, 255, 127, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 127
            ).map { it.toByte() }.toByteArray()
        )
        normalTexture.createRGBA(byteArrayOf(127, 127, 255.toByte(), 255.toByte()))
        blackTexture.createRGBA(byteArrayOf(0, 0, 0, 255.toByte()))
    }

}