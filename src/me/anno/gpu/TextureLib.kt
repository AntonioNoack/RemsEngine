package me.anno.gpu

import me.anno.gpu.texture.Texture2D

object TextureLib {

    val invisibleTexture = Texture2D(1, 1, 1)
    val whiteTexture = Texture2D(1, 1, 1)
    val stripeTexture = Texture2D(5, 1, 1)
    val colorShowTexture = Texture2D(2, 2, 1)
    val normalTexture = Texture2D(1,1,1)
    val blackTexture = Texture2D(1,1,1)

    fun init(){
        invisibleTexture.createRGBA(ByteArray(4) { 0.toByte() })
        whiteTexture.createRGBA(
            byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte()))
        stripeTexture.createMonochrome(
            byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte()))
        colorShowTexture.createRGBA(
            intArrayOf(
                255,255,255,127, 255,255,255,255,
                255,255,255,255, 255,255,255,127
            ).map { it.toByte() }.toByteArray())
        normalTexture.createRGBA(byteArrayOf(127,127,255.toByte(),255.toByte()))
        blackTexture.createRGBA(byteArrayOf(0,0,0,255.toByte()))
    }

}