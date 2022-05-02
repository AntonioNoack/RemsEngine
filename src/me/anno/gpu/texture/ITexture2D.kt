package me.anno.gpu.texture

import me.anno.cache.data.ICacheData
import me.anno.gpu.shader.Shader

interface ITexture2D : ICacheData {

    var w: Int
    var h: Int

    /*fun bind(nearest: GPUFiltering, clamping: Clamping): Boolean
    fun bind(filtering: Filtering, clamping: Clamping): Boolean {
        return bind(if(filtering.baseIsNearest) GPUFiltering.NEAREST else GPUFiltering.LINEAR, clamping)
    }*/

    val isHDR: Boolean

    fun bind(index: Int, filtering: GPUFiltering, clamping: Clamping): Boolean
    fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean {
        return bind(index, if (filtering.baseIsNearest) GPUFiltering.NEAREST else GPUFiltering.LINEAR, clamping)
    }

    fun bind(shader: Shader, texName: String, nearest: GPUFiltering, clamping: Clamping): Boolean {
        val index = shader.getTextureIndex(texName)
        return if (index >= 0) {
            bind(index, nearest, clamping)
        } else false
    }

    fun bindTrulyNearest(index: Int) = bind(index, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

}