package me.anno.gpu.texture

import me.anno.cache.data.ICacheData
import me.anno.gpu.copying.FramebufferToMemory
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference
import me.anno.utils.OS

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

    fun write(dst: FileReference, flipY: Boolean, withAlpha: Boolean) {
        FramebufferToMemory.createImage(this, flipY, withAlpha)
            .write(dst)
    }

}