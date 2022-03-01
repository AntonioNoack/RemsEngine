package me.anno.gpu.framebuffer

import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D

interface IFramebuffer {

    val name: String

    val pointer: Int

    val w: Int
    val h: Int

    val samples: Int

    val numTextures: Int

    fun ensure()

    fun bindDirectly()

    fun bindDirectly(w: Int, h: Int)

    fun destroy()

    fun attachFramebufferToDepth(targetCount: Int, fpTargets: Boolean): IFramebuffer

    fun checkSession()

    fun bindTexture0(shader: Shader, texName: String, nearest: GPUFiltering, clamping: Clamping) {
        val index = shader.getTextureIndex(texName)
        if (index >= 0) {
            checkSession()
            bindTextureI(index, 0, nearest, clamping)
        }
    }

    fun bindTexture0(offset: Int = 0, nearest: GPUFiltering, clamping: Clamping) {
        bindTextureI(0, offset, nearest, clamping)
    }

    fun bindTextureI(index: Int, offset: Int) {
        checkSession()
        bindTextureI(index, offset, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
    }

    fun bindTextureI(index: Int, offset: Int, nearest: GPUFiltering, clamping: Clamping)

    fun bindTextures(offset: Int = 0, nearest: GPUFiltering, clamping: Clamping)

    fun getTexture0() = getTextureI(0)

    fun getTextureI(index: Int): ITexture2D

    val depthTexture: ITexture2D?

}