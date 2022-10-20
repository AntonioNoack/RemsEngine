package me.anno.gpu.texture

import me.anno.Build
import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.framebuffer.VRAMToRAM
import me.anno.gpu.shader.FlatShaders
import me.anno.gpu.texture.Texture2D.Companion.activeSlot
import me.anno.gpu.texture.Texture2D.Companion.bindTexture
import me.anno.gpu.texture.Texture2D.Companion.bufferPool
import me.anno.gpu.texture.Texture2D.Companion.textureBudgetTotal
import me.anno.gpu.texture.Texture2D.Companion.textureBudgetUsed
import me.anno.gpu.texture.Texture2D.Companion.writeAlignment
import me.anno.gpu.texture.TextureLib.invisibleTex3d
import me.anno.image.Image
import me.anno.utils.types.Booleans.toInt
import org.lwjgl.opengl.GL30C.*
import org.lwjgl.opengl.GL45C
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.concurrent.thread

open class Texture3D(
    var name: String,
    override var w: Int,
    override var h: Int,
    var d: Int
) : ICacheData, ITexture2D {

    constructor(name: String, img: BufferedImage, depth: Int) : this(name, img.width / depth, img.height, depth) {
        create(img, true)
        filtering(filtering)
        clamping(clamping)
    }

    var pointer = -1
    var session = -1

    var isCreated = false
    var isDestroyed = false
    var filtering = GPUFiltering.NEAREST
    var clamping = Clamping.CLAMP

    var locallyAllocated = 0L

    var internalFormat = 0

    val target get() = GL_TEXTURE_3D

    override var isHDR = false

    fun checkSession() {
        if (session != GFXState.session) {
            session = GFXState.session
            pointer = -1
            isCreated = false
            isDestroyed = false
        }
    }

    private fun ensurePointer() {
        checkSession()
        if (pointer < 0) pointer = Texture2D.createTexture()
        if (pointer < 0) throw RuntimeException("Could not generate texture")
        if (Build.isDebug) synchronized(DebugGPUStorage.tex3d) {
            DebugGPUStorage.tex3d.add(this)
        }
        isDestroyed = false
    }

    private fun beforeUpload(alignment: Int) {
        ensurePointer()
        forceBind()
        writeAlignment(alignment)
    }

    private fun afterUpload(internalFormat: Int, bpp: Int, hdr: Boolean) {
        isCreated = true
        this.internalFormat = internalFormat
        locallyAllocated = allocate(locallyAllocated, w.toLong() * h.toLong() * d.toLong() * bpp)
        filtering(filtering)
        clamping(clamping)
        isHDR = hdr
        GFX.check()
        when (internalFormat) {
            GL_R8, GL_R16, GL_R16F,
            GL_R32F -> swizzleMonochrome()
        }
    }

    @Suppress("unused")
    fun createRGBA8() {
        beforeUpload(w * 4)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA8, w, h, d, 0, GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)
        afterUpload(GL_RGBA8, 4, false)
    }

    @Suppress("unused")
    fun createRGBAFP32() {
        beforeUpload(w * 16)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, w, h, d, 0, GL_RGBA, GL_FLOAT, null as ByteBuffer?)
        afterUpload(GL_RGBA32F, 16, true)
    }

    fun create(createImage: () -> BufferedImage) {
        val requiredBudget = textureBudgetUsed + w * h * d
        if (requiredBudget > textureBudgetTotal) {
            thread(name = "Create Image") { create(createImage(), false) }
        } else {
            textureBudgetUsed = requiredBudget
            create(createImage(), true)
        }
    }

    fun create(img: Image, sync: Boolean) {
        create(img.createBufferedImage(), sync)
    }

    fun create(img: BufferedImage, sync: Boolean) {
        val intData = img.getRGB(0, 0, img.width, img.height, null, 0, img.width)
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            // todo check whether this inversion is really needed
            for (i in intData.indices) {// argb -> abgr
                val argb = intData[i]
                val r = argb.shr(16) and 0xff
                val b = (argb and 0xff).shl(16)
                intData[i] = (argb and 0xff00ff00.toInt()) or r or b
            }
        } else {
            for (i in intData.indices) {// argb -> rgba
                val argb = intData[i]
                val a = argb.shr(24) and 255
                val rgb = argb.and(0xffffff) shl 8
                intData[i] = rgb or a
            }
        }
        if (sync) createRGBA8(intData)
        else GFX.addGPUTask("Texture3D.create()", img.width, img.height) {
            createRGBA8(intData)
        }
    }

    fun createRGBA8(data: IntArray) {
        beforeUpload(w * 4)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA8, w, h, d, 0, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
    }

    fun createBGRA8(data: ByteBuffer) {
        beforeUpload(w * 4)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA8, w, h, d, 0, GL_BGRA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
    }

    fun createMonochrome(data: ByteArray) {
        if (w * h * d != data.size) throw RuntimeException("incorrect size!")
        val byteBuffer = bufferPool[data.size, false, false]
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.position(0)
        createMonochrome(byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
    }

    fun createMonochrome(getValue: (x: Int, y: Int, z: Int) -> Byte) {
        val w = w
        val h = h
        val d = d
        val size = w * h * d
        val byteBuffer = bufferPool[size, false, false]
        for (z in 0 until d) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    byteBuffer.put(getValue(x, y, z))
                }
            }
        }
        byteBuffer.flip()
        createMonochrome(byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
    }

    fun createRGBA8(getValue: (x: Int, y: Int, z: Int) -> Int) {
        val w = w
        val h = h
        val d = d
        val size = 4 * w * h * d
        val byteBuffer = bufferPool[size, false, false]
        for (z in 0 until d) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    byteBuffer.putInt(getValue(x, y, z))
                }
            }
        }
        byteBuffer.flip()
        createBGRA8(byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
    }

    fun createMonochrome(data: ByteBuffer) {
        if (w * h * d != data.remaining()) throw RuntimeException("incorrect size!")
        beforeUpload(w)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_R8, w, h, d, 0, GL_RED, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_R8, 1, false)
    }

    fun create(type: TargetType, data: ByteArray? = null) {
        // might be incorrect for RGB!!
        if (data != null && type.bytesPerPixel != 3 && w * h * d * type.bytesPerPixel != data.size)
            throw RuntimeException("incorrect size!, got ${data.size}, expected $w * $h * $d * ${type.bytesPerPixel} bpp")
        val byteBuffer = if (data != null) {
            val byteBuffer = bufferPool[data.size, false, false]
            byteBuffer.position(0)
            byteBuffer.put(data)
            byteBuffer.position(0)
            byteBuffer
        } else null
        beforeUpload(w)
        glTexImage3D(
            GL_TEXTURE_3D, 0, type.internalFormat, w, h, d, 0,
            type.uploadFormat, type.fillType, byteBuffer
        )
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(type.internalFormat, type.bytesPerPixel, type.isHDR)
    }

    fun createRGBA(data: FloatArray) {
        if (w * h * d * 4 != data.size) throw RuntimeException("incorrect size!, got ${data.size}, expected $w * $h * $d * 4 bpp")
        val byteBuffer = bufferPool[data.size * 4, false, false]
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.position(0)
        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(data)
        floatBuffer.flip()
        createRGBA(floatBuffer, byteBuffer)
    }

    fun createRGBA(floatBuffer: FloatBuffer, byteBuffer: ByteBuffer) {
        // rgba32f as internal format is extremely important... otherwise the value is cropped
        beforeUpload(w * 16)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, w, h, d, 0, GL_RGBA, GL_FLOAT, floatBuffer)
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(GL_RGBA32F, 16, true)
    }

    fun createRGBA(data: ByteArray) {
        if (w * h * d * 4 != data.size) throw RuntimeException("incorrect size!, got ${data.size}, expected $w * $h * $d * 4 bpp")
        val byteBuffer = bufferPool[data.size, false, false]
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.flip()
        beforeUpload(w * 4)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA8, w, h, d, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(GL_RGBA8, 4, false)
    }

    fun createRGBA(data: ByteBuffer) {
        if (w * h * d * 4 != data.remaining()) throw RuntimeException("incorrect size!, got ${data.remaining()}, expected $w * $h * $d * 4 bpp")
        beforeUpload(w * 4)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA8, w, h, d, 0, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
    }

    fun ensureFiltering(nearest: GPUFiltering, clamping: Clamping) {
        if (nearest != filtering) filtering(nearest)
        if (clamping != this.clamping) clamping(clamping)
    }

    fun filtering(nearest: GPUFiltering) {
        if (nearest != GPUFiltering.LINEAR) {
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        } else {
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        }
        filtering = nearest
    }

    fun clamping(clamping: Clamping) {
        val type = clamping.mode
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, type)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, type)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, type)
        this.clamping = clamping
    }

    private fun forceBind() {
        if (pointer == -1) throw RuntimeException()
        bindTexture(GL_TEXTURE_3D, pointer)
    }

    fun bind(index: Int) {
        bind(index, filtering, clamping)
    }

    override fun bind(index: Int, filtering: GPUFiltering, clamping: Clamping): Boolean {
        activeSlot(index)
        if (pointer > -1 && isCreated) {
            bindTexture(GL_TEXTURE_3D, pointer)
            ensureFiltering(filtering, clamping)
        } else {
            invisibleTex3d.bind(index, GPUFiltering.LINEAR, Clamping.CLAMP)
        }
        return true
    }

    override fun wrapAsFramebuffer(): IFramebuffer {
        throw NotImplementedError()
    }

    override fun destroy() {
        val pointer = pointer
        if (pointer > -1) {
            if (GFX.isGFXThread()) destroy(pointer)
            else GFX.addGPUTask("Texture3D.destroy()", 1) { destroy(pointer) }
        }
        this.pointer = -1
    }

    private fun destroy(pointer: Int) {
        if (Build.isDebug) synchronized(DebugGPUStorage.tex3d) {
            DebugGPUStorage.tex3d.remove(this)
        }
        GFX.checkIsGFXThread()
        glDeleteTextures(pointer)
        Texture2D.invalidateBinding()
        locallyAllocated = allocate(locallyAllocated, 0L)
        isDestroyed = true
    }

    override fun createBufferedImage(flipY: Boolean, withAlpha: Boolean) =
        VRAMToRAM.createBufferedImage(w * d, h, VRAMToRAM.zero, flipY, withAlpha) { x2, y2, w2, _ ->
            drawSlice(x2, y2, w2, withAlpha)
        }

    override fun createImage(flipY: Boolean, withAlpha: Boolean) =
        VRAMToRAM.createImage(w * d, h, VRAMToRAM.zero, flipY, withAlpha) { x2, y2, w2, _ ->
            drawSlice(x2, y2, w2, withAlpha)
        }

    private fun drawSlice(x2: Int, y2: Int, w2: Int, withAlpha: Boolean) {
        val z0 = x2 / w
        val z1 = (x2 + w2 - 1) / w
        drawSlice(x2, y2, z0 / maxOf(1f, d - 1f), withAlpha)
        if (z1 > z0) {
            // todo we have to draw two slices
            // drawSlice(x2, y2, z0 / maxOf(1f, d - 1f), withAlpha)
        }
    }

    private fun drawSlice(x2: Int, y2: Int, z: Float, withAlpha: Boolean) {
        val x = -x2
        val y = -y2
        GFX.check()
        // we could use an easier shader here
        val shader = FlatShaders.flatShaderTexture3D.value
        shader.use()
        GFXx2D.posSize(shader, x, GFX.viewportHeight - y, w, -h)
        GFXx2D.defineAdvancedGraphicalFeatures(shader)
        shader.v4f("color", -1)
        shader.v1i("alphaMode", 1 - withAlpha.toInt())
        shader.v1b("applyToneMapping", isHDR)
        shader.v1f("uvZ", z)
        GFXx2D.noTiling(shader)
        bind(0, filtering, Clamping.CLAMP)
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun swizzleMonochrome() {
        swizzle(GL_RED, GL_RED, GL_RED, GL_ONE)
    }

    fun swizzleAlpha() {
        swizzle(GL_ONE, GL_ONE, GL_ONE, GL_RED)
    }

    // could and should be used for roughness/metallic like textures in the future
    fun swizzle(r: Int, g: Int, b: Int, a: Int) {
        val tmp = Texture2D.tmp4i
        tmp[0] = r
        tmp[1] = g
        tmp[2] = b
        tmp[3] = a
        GFX.check()
        glTexParameteriv(target, GL45C.GL_TEXTURE_SWIZZLE_RGBA, tmp)
        GFX.check()
    }

    companion object {
        var allocated = 0L
        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated += newValue - oldValue
            return newValue
        }
    }

}