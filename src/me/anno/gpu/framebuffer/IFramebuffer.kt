package me.anno.gpu.framebuffer

import me.anno.gpu.texture.ITexture2D

interface IFramebuffer {

    val pointer: Int

    val w: Int
    val h: Int

    val samples: Int

    fun ensure()

    fun bindDirectly(viewport: Boolean)

    fun bindDirectly(w: Int, h: Int, viewport: Boolean)

    fun destroy()

    val depthTexture: ITexture2D?

}