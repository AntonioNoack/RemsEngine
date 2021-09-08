package me.anno.gpu.texture

import me.anno.cache.data.ICacheData

interface ITexture2D : ICacheData {

    var w: Int
    var h: Int

    /*fun bind(nearest: GPUFiltering, clamping: Clamping): Boolean
    fun bind(filtering: Filtering, clamping: Clamping): Boolean {
        return bind(if(filtering.baseIsNearest) GPUFiltering.NEAREST else GPUFiltering.LINEAR, clamping)
    }*/

    fun bind(index: Int, nearest: GPUFiltering, clamping: Clamping): Boolean
    fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean {
        return bind(index, if (filtering.baseIsNearest) GPUFiltering.NEAREST else GPUFiltering.LINEAR, clamping)
    }

    fun bindTrulyNearest(index: Int) = bind(index, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

}