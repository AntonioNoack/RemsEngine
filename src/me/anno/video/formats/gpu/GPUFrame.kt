package me.anno.video.formats.gpu

import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.*
import me.anno.image.raw.ByteImage
import me.anno.utils.OS.desktop
import me.anno.utils.Sleep.waitForGFXThread
import me.anno.utils.files.Files.findNextFile
import me.anno.video.BlankFrameDetector
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.Semaphore

abstract class GPUFrame(var width: Int, var height: Int, val code: Int) : ICacheData {

    init {
        if (width < 1 || height < 1) throw IllegalArgumentException("Cannot create empty frames")
    }

    val isCreated: Boolean get() = getTextures().all { it.isCreated && !it.isDestroyed }
    val isDestroyed: Boolean get() = getTextures().any { it.isDestroyed }

    val blankDetector = BlankFrameDetector()

    fun isBlankFrame(f0: GPUFrame, f4: GPUFrame, outlierThreshold: Float = 1f): Boolean {
        return blankDetector.isBlankFrame(f0.blankDetector, f4.blankDetector, outlierThreshold)
    }

    abstract fun get3DShader(): BaseShader
    abstract fun get2DShader(): Shader

    abstract fun getTextures(): List<Texture2D>

    abstract fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping)

    fun bind(offset: Int, filtering: Filtering, clamping: Clamping) {
        val gpuFiltering = if (filtering.baseIsNearest) GPUFiltering.NEAREST else GPUFiltering.LINEAR
        bind(offset, gpuFiltering, clamping)
    }

    fun bind(offset: Int, filtering: Filtering, clamping: Clamping, tex: List<ITexture2D>) {
        val gpuFiltering = if (filtering.baseIsNearest) GPUFiltering.NEAREST else GPUFiltering.LINEAR
        for ((index, texture) in tex.withIndex().reversed()) {
            texture.bind(offset + index, gpuFiltering, clamping)
        }
    }

    fun bind2(offset: Int, filtering: Filtering, clamping: Clamping, tex: List<IFramebuffer>) {
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

    fun interlaceReplace(a: ByteBuffer, b: ByteBuffer): ByteBuffer {
        val dst = Texture2D.bufferPool[a.remaining() * 2, false, false]
        val size = a.limit()
        for (i in 0 until size) {
            dst.put(a[i])
            dst.put(b[i])
        }
        dst.flip()
        Texture2D.bufferPool.returnBuffer(a)
        Texture2D.bufferPool.returnBuffer(b)
        return dst
    }

    abstract fun load(input: InputStream)
    fun waitToLoad() {
        waitForGFXThread(true) { isCreated }
    }

    open fun bindUVCorrection(shader: Shader) {
        val w = width
        val h = height
        shader.v2f("uvCorrection", w.toFloat() / ((w + 1) / 2 * 2), h.toFloat() / ((h + 1) / 2 * 2))
    }

    fun writeMonochromeDebugImage(w: Int, h: Int, buffer: ByteBuffer) {
        val file = findNextFile(desktop, "mono", "png", 1, '-')
        val image = ByteImage(w, h, ByteImage.Format.R)
        val data = image.data
        for (i in 0 until w * h) {
            data[i] = buffer[i]
        }
        image.write(file)
    }

    fun toTexture(): Texture2D {
        GFX.checkIsGFXThread()
        val tmp = Framebuffer("webp-temp", width, height, 1, 1, false, DepthBufferType.NONE)
        lateinit var tex: Texture2D
        GFXState.useFrame(tmp, Renderer.copyRenderer) {
            GFXState.renderPurely {
                val shader = get2DShader()
                shader.use()
                bind(0, GPUFiltering.LINEAR, Clamping.CLAMP)
                bindUVCorrection(shader)
                GFX.flat01.draw(shader)
                GFX.check()
                tex = tmp.textures[0]
            }
        }
        GFX.check()
        tmp.destroyExceptTextures(false)
        return tex
    }

    companion object {
        @JvmField
        val creationLimiter = Semaphore(32)
    }
}