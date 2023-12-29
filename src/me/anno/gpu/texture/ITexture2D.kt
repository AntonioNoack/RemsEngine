package me.anno.gpu.texture

import me.anno.cache.ICacheData
import me.anno.gpu.DepthMode
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.VRAMToRAM
import me.anno.gpu.shader.GPUShader
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference

interface ITexture2D : ICacheData {

    val name: String

    val width: Int
    val height: Int
    val samples: Int
    val channels: Int

    val isHDR: Boolean
    val wasCreated: Boolean
    val isDestroyed: Boolean

    var depthFunc: DepthMode?

    val filtering: Filtering
    val clamping: Clamping

    fun isCreated(): Boolean {
        return wasCreated && !isDestroyed
    }

    fun bind(index: Int): Boolean = bind(index, filtering, clamping)
    fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean
    fun bind(shader: GPUShader, texName: String, nearest: Filtering, clamping: Clamping): Boolean {
        val index = shader.getTextureIndex(texName)
        return if (index >= 0) {
            bind(index, nearest, clamping)
        } else false
    }

    fun bindTrulyNearest(index: Int) = bind(index, Filtering.TRULY_NEAREST, Clamping.CLAMP)

    fun bindTrulyNearest(shader: GPUShader, texName: String): Boolean {
        val index = shader.getTextureIndex(texName)
        return if (index >= 0) bindTrulyNearest(index) else false
    }

    fun bindTrulyLinear(index: Int) = bind(index, Filtering.TRULY_LINEAR, Clamping.CLAMP)

    fun bindTrulyLinear(shader: GPUShader, texName: String): Boolean {
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