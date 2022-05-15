package me.anno.gpu.texture

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D.Companion.activeSlot
import me.anno.gpu.texture.Texture2D.Companion.bindTexture
import me.anno.gpu.texture.Texture2D.Companion.bufferPool
import me.anno.gpu.texture.Texture2D.Companion.textureBudgetTotal
import me.anno.gpu.texture.Texture2D.Companion.textureBudgetUsed
import me.anno.gpu.texture.Texture2D.Companion.writeAlignment
import me.anno.gpu.texture.TextureLib.invisibleTex3d
import me.anno.image.Image
import me.anno.utils.hpc.Threads.threadWithName
import org.lwjgl.opengl.GL30C.*
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

open class Texture3D(var name: String, var w: Int, var h: Int, var d: Int) : ICacheData {

    constructor(name: String, img: BufferedImage, depth: Int) : this(name, img.width / depth, img.height, depth) {
        create(img, true)
        filtering(filtering)
    }

    var pointer = -1
    var session = -1

    var isCreated = false
    var isDestroyed = false
    var filtering = GPUFiltering.NEAREST

    var locallyAllocated = 0L

    var internalFormat = 0

    fun checkSession() {
        if (session != OpenGL.session) {
            session = OpenGL.session
            pointer = -1
            isCreated = false
            isDestroyed = false
        }
    }

    private fun ensurePointer() {
        checkSession()
        if (pointer < 0) pointer = Texture2D.createTexture()
        if (pointer < 0) throw RuntimeException("Could not generate texture")
        DebugGPUStorage.tex3d.add(this)
        isDestroyed = false
    }

    private fun beforeUpload(alignment: Int) {
        ensurePointer()
        forceBind()
        writeAlignment(alignment)
    }

    private fun afterUpload(internalFormat: Int, bpp: Int) {
        isCreated = true
        this.internalFormat = internalFormat
        locallyAllocated = allocate(locallyAllocated, w.toLong() * h.toLong() * d.toLong() * bpp)
        filtering(filtering)
        GFX.check()
    }

    fun createRGBA8() {
        beforeUpload(w * 4)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA8, w, h, d, 0, GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)
        afterUpload(GL_RGBA8, 4)
    }

    fun createRGBAFP32() {
        beforeUpload(w * 16)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, w, h, d, 0, GL_RGBA, GL_FLOAT, null as ByteBuffer?)
        afterUpload(GL_RGBA32F, 16)
    }

    fun create(createImage: () -> BufferedImage) {
        val requiredBudget = textureBudgetUsed + w * h * d
        if (requiredBudget > textureBudgetTotal) {
            threadWithName("Create Image") { create(createImage(), false) }
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
        afterUpload(GL_RGBA8, 4)
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

    fun createMonochrome(data: ByteBuffer) {
        if (w * h * d != data.remaining()) throw RuntimeException("incorrect size!")
        beforeUpload(w)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_R8, w, h, d, 0, GL_RED, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_R8, 1)
    }

    fun create(type: TargetType, data: ByteArray?) {
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
        afterUpload(type.internalFormat, type.bytesPerPixel)
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
        afterUpload(GL_RGBA32F, 16)
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
        afterUpload(GL_RGBA8, 4)
    }

    fun createRGBA(data: ByteBuffer) {
        if (w * h * d * 4 != data.remaining()) throw RuntimeException("incorrect size!, got ${data.remaining()}, expected $w * $h * $d * 4 bpp")
        beforeUpload(w * 4)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA8, w, h, d, 0, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4)
    }

    fun ensureFiltering(nearest: GPUFiltering) {
        if (nearest != filtering) filtering(nearest)
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

    // todo full clamping support
    fun clamping(repeat: Boolean) {
        val type = if (repeat) GL_REPEAT else GL_CLAMP_TO_EDGE
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, type)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, type)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, type)
    }

    private fun forceBind() {
        if (pointer == -1) throw RuntimeException()
        bindTexture(GL_TEXTURE_3D, pointer)
    }

    fun bind(index: Int) {
        bind(index, filtering)
    }

    open fun bind(index: Int, filtering: GPUFiltering) {
        activeSlot(index)
        if (pointer > -1 && isCreated) {
            bindTexture(GL_TEXTURE_3D, pointer)
            ensureFiltering(filtering)
        } else {
            invisibleTex3d.bind(index, GPUFiltering.LINEAR)
        }
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
        DebugGPUStorage.tex3d.remove(this)
        glDeleteTextures(pointer)
        Texture2D.invalidateBinding()
        locallyAllocated = allocate(locallyAllocated, 0L)
        isDestroyed = true
    }

    companion object {
        var allocated = 0L
        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated += newValue - oldValue
            return newValue
        }
    }

}