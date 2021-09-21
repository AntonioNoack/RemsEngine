package me.anno.video

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX.glThread
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.OS.desktop
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.files.Files.findNextFile
import java.awt.image.BufferedImage
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.Semaphore
import javax.imageio.ImageIO

abstract class VFrame(
    var w: Int, var h: Int, val code: Int
) : ICacheData {

    val isCreated: Boolean get() = getTextures().all { it.isCreated && !it.isDestroyed }
    val isDestroyed: Boolean get() = getTextures().any { it.isDestroyed }

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
        for (texture in getTextures()) {
            texture.destroy()
        }
    }

    fun interlace(a: ByteBuffer, b: ByteBuffer, dst: ByteBuffer): ByteBuffer {
        val size = a.limit()
        for (i in 0 until size) {
            dst.put(a[i])
            dst.put(b[i])
        }
        dst.flip()
        return dst
    }

    abstract fun load(input: InputStream)
    fun waitToLoad() {
        if (Thread.currentThread() == glThread) throw RuntimeException("Cannot wait on main thread")
        waitUntil(true) { isCreated }
    }

    open fun bindUVCorrection(shader: Shader) {
        val w = w
        val h = h
        shader.v2("uvCorrection", w.toFloat() / ((w + 1) / 2 * 2), h.toFloat() / ((h + 1) / 2 * 2))
    }

    fun writeMonochromeDebugImage(w: Int, h: Int, buffer: ByteBuffer) {
        val file = findNextFile(desktop, "mono", "png", 1, '-')
        val img = BufferedImage(w, h, 1)
        for (y in 0 until h) {
            for (x in 0 until w) {
                img.setRGB(x, y, buffer[x + y * w].toInt().and(255) * 0x10101)
            }
        }
        file.outputStream().use { ImageIO.write(img, "png", it) }
    }

    companion object {
        val creationLimiter = Semaphore(32)
    }

}