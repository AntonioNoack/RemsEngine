package me.anno.video

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX.glThread
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep.waitUntil
import java.io.InputStream
import java.util.concurrent.Semaphore

abstract class VFrame(
    var w: Int, var h: Int, val code: Int
) : ICacheData {

    open val isCreated = false
    var isDestroyed = false

    abstract fun get3DShader(): BaseShader

    abstract fun getTextures(): List<Texture2D>

    abstract fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping)
    fun bind(offset: Int, filtering: Filtering, clamping: Clamping) {
        val gpuFiltering = if (filtering.baseIsNearest) GPUFiltering.NEAREST else GPUFiltering.LINEAR
        bind(offset, gpuFiltering, clamping)
    }

    fun bind(offset: Int, filtering: Filtering, clamping: Clamping, tex: List<Texture2D>) {
        val gpuFiltering = if (filtering.baseIsNearest) GPUFiltering.NEAREST else GPUFiltering.LINEAR
        for ((index, texture) in tex.withIndex().reversed()) {
            texture.bind(offset + index, gpuFiltering, clamping)
        }
    }

    fun bind2(offset: Int, filtering: Filtering, clamping: Clamping, tex: List<Framebuffer>) {
        val gpuFiltering = if (filtering.baseIsNearest) GPUFiltering.NEAREST else GPUFiltering.LINEAR
        for ((index, texture) in tex.withIndex().reversed()) {
            texture.bindTexture0(offset + index, gpuFiltering, clamping)
        }
    }

    override fun destroy() {
        isDestroyed = true
    }

    abstract fun load(input: InputStream)
    fun waitToLoad() {
        if (Thread.currentThread() == glThread) throw RuntimeException("Cannot wait on main thread")
        waitUntil(true) { isCreated }
    }

    companion object {
        val creationLimiter = Semaphore(32)
    }

}