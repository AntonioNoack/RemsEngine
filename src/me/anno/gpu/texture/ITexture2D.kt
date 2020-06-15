package me.anno.gpu.texture

interface ITexture2D {

    var w: Int
    var h: Int

    fun bind(nearest: Boolean)

    fun bind(index: Int, nearest: Boolean)

    fun destroy()

}