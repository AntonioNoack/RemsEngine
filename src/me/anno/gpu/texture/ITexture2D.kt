package me.anno.gpu.texture

interface ITexture2D {

    var w: Int
    var h: Int


    fun bind(nearest: Boolean, clampMode: ClampMode)
    fun bind(filtering: FilteringMode, clampMode: ClampMode) = bind(filtering.baseIsNearest, clampMode)

    fun bind(index: Int, nearest: Boolean, clampMode: ClampMode)
    fun bind(index: Int, filtering: FilteringMode, clampMode: ClampMode) = bind(index, filtering.baseIsNearest, clampMode)

    fun destroy()

}