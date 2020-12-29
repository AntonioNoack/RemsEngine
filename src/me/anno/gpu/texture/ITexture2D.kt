package me.anno.gpu.texture

import me.anno.cache.ICacheData

interface ITexture2D: ICacheData {

    var w: Int
    var h: Int


    fun bind(nearest: GPUFiltering, clamping: Clamping)
    fun bind(filtering: Filtering, clamping: Clamping){
        bind(if(filtering.baseIsNearest) GPUFiltering.NEAREST else GPUFiltering.LINEAR, clamping)
    }

    fun bind(index: Int, nearest: GPUFiltering, clamping: Clamping)
    fun bind(index: Int, filtering: Filtering, clamping: Clamping){
        bind(index, if(filtering.baseIsNearest) GPUFiltering.NEAREST else GPUFiltering.LINEAR, clamping)
    }

    // override fun destroy()

}