package me.anno.gpu.texture

import me.anno.cache.ICacheData
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.VRAMToRAM
import me.anno.gpu.shader.Shader
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference

interface ITexture2D : ICacheData {

    var width: Int
    var height: Int
    val samples: Int

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

    fun bindTrulyNearest(shader: Shader, texName: String): Boolean {
        val index = shader.getTextureIndex(texName)
        return if (index >= 0) bindTrulyNearest(index) else false
    }

    fun bindTrulyLinear(index: Int) = bind(index, GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)

    fun bindTrulyLinear(shader: Shader, texName: String): Boolean {
        val index = shader.getTextureIndex(texName)
        return if (index >= 0) bindTrulyLinear(index) else false
    }

    fun write(dst: FileReference, flipY: Boolean = false, withAlpha: Boolean = false) {
        createImage(flipY, withAlpha).write(dst)
    }

    fun wrapAsFramebuffer(): IFramebuffer

    fun createImage(flipY: Boolean, withAlpha: Boolean): IntImage {
        return VRAMToRAM.createImage(width, height, VRAMToRAM.zero, flipY, withAlpha) { x2, y2, _, _ ->
            VRAMToRAM.drawTexturePure(-x2, -y2, width, height, this, !withAlpha)
        }
    }

}