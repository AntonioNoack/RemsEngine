package me.anno.gpu.framebuffer

import me.anno.gpu.texture.ITexture2D

interface IFramebuffer {

    val pointer: Int

    val w: Int
    val h: Int

    val samples: Int

    fun ensure()

    fun bindDirectly()

    fun bindDirectly(w: Int, h: Int)

    fun destroy()

    val depthTexture: ITexture2D?

}