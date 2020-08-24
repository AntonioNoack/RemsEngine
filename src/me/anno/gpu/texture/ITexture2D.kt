package me.anno.gpu.texture

interface ITexture2D {

    var w: Int
    var h: Int


    fun bind(nearest: Boolean)
    fun bind(filtering: FilteringMode) = bind(filtering.baseIsNearest)

    fun bind(index: Int, nearest: Boolean)
    fun bind(index: Int, filtering: FilteringMode) = bind(index, filtering.baseIsNearest)

    fun destroy()

}