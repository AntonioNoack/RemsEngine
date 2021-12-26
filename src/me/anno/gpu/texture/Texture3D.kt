package me.anno.gpu.texture

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import me.anno.gpu.TextureLib.invisibleTexture
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.texture.Texture2D.Companion.activeSlot
import me.anno.gpu.texture.Texture2D.Companion.bindTexture
import me.anno.gpu.texture.Texture2D.Companion.bufferPool
import me.anno.gpu.texture.Texture2D.Companion.packAlignment
import me.anno.gpu.texture.Texture2D.Companion.textureBudgetTotal
import me.anno.gpu.texture.Texture2D.Companion.textureBudgetUsed
import me.anno.image.Image
import me.anno.utils.hpc.Threads.threadWithName
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30.*
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Texture3D(val w: Int, val h: Int, val d: Int) : ICacheData {

    constructor(img: BufferedImage, depth: Int) : this(img.width / depth, img.height, depth) {
        create(img, true)
        filtering(isFilteredNearest)
    }

    var pointer = -1
    var session = -1

    var isCreated = false
    var isDestroyed = false
    var isFilteredNearest = GPUFiltering.NEAREST

    fun ensureSession() {
        if (session != OpenGL.session) {
            session = OpenGL.session
            pointer = -1
            isCreated = false
            isDestroyed = false
        }
    }

    fun ensurePointer() {
        if (pointer < 0) pointer = Texture2D.createTexture()
        if (pointer < 0) throw RuntimeException("Could not generate texture")
        DebugGPUStorage.tex3d.add(this)
        isDestroyed = false
    }

    fun create() {
        ensurePointer()
        forceBind()
        packAlignment(1)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA8, w, h, d, 0, GL11.GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)
        filtering(isFilteredNearest)
        isCreated = true
    }

    fun createFP32() {
        ensurePointer()
        forceBind()
        packAlignment(1)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, w, h, d, 0, GL_RGBA, GL_FLOAT, null as ByteBuffer?)
        filtering(isFilteredNearest)
        isCreated = true
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
        val bi = img.createBufferedImage()
        create(bi, sync)
    }

    fun create(img: BufferedImage, sync: Boolean) {
        val intData = img.getRGB(0, 0, img.width, img.height, null, 0, img.width)
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            // todo check whether this inversion is really needed
            for (i in intData.indices) {// argb -> abgr
                val argb = intData[i]
                val r = (argb and 0xff0000).shr(16)
                val b = (argb and 0xff).shl(16)
                intData[i] = argb and 0xff00ff00.toInt() or r or b
            }
        } else {
            for (i in intData.indices) {// argb -> rgba
                val argb = intData[i]
                val a = argb.shr(24) and 255
                val rgb = argb.and(0xffffff) shl 8
                intData[i] = rgb or a
            }
        }
        if (sync) uploadData(intData)
        else GFX.addGPUTask(img.width, img.height) {
            uploadData(intData)
        }
    }

    fun uploadData(intData: IntArray) {
        GFX.check()
        ensurePointer()
        forceBind()
        packAlignment(1)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA8, w, h, d, 0, GL_RGBA, GL_UNSIGNED_BYTE, intData)
        isCreated = true
        GFX.check()
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun createMonochrome(data: ByteArray) {
        if (w * h * d != data.size) throw RuntimeException("incorrect size!")
        ensurePointer()
        forceBind()
        GFX.check()
        val byteBuffer = bufferPool[data.size, false]
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.position(0)
        packAlignment(1)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_R8, w, h, d, 0, GL11.GL_RED, GL_UNSIGNED_BYTE, byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
        isCreated = true
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun create(data: FloatArray) {
        if (w * h * d * 4 != data.size) throw RuntimeException("incorrect size!")
        val byteBuffer = bufferPool[data.size * 4, false]
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.position(0)
        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(data)
        floatBuffer.position(0)
        create(floatBuffer, byteBuffer)
    }

    fun create(floatBuffer: FloatBuffer, byteBuffer: ByteBuffer) {
        ensurePointer()
        forceBind()
        GFX.check()
        // rgba32f as internal format is extremely important... otherwise the value is cropped
        packAlignment(1)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, w, h, d, 0, GL_RGBA, GL_FLOAT, floatBuffer)
        bufferPool.returnBuffer(byteBuffer)
        isCreated = true
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun create(data: ByteArray) {
        if (w * h * d * 4 != data.size) throw RuntimeException("incorrect size!")
        ensurePointer()
        forceBind()
        GFX.check()
        val byteBuffer = bufferPool[data.size, false]
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.position(0)
        packAlignment(1)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA, w, h, d, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
        isCreated = true
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun ensureFiltering(nearest: GPUFiltering) {
        if (nearest != isFilteredNearest) filtering(nearest)
    }

    fun filtering(nearest: GPUFiltering) {
        if (nearest != GPUFiltering.LINEAR) {
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        } else {
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        }
        isFilteredNearest = nearest
    }

    fun clamping(repeat: Boolean) {
        val type = if (repeat) GL_REPEAT else GL_CLAMP_TO_EDGE
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, type)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, type)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, type)
    }

    fun forceBind() {
        if (pointer == -1) throw RuntimeException()
        bindTexture(GL_TEXTURE_3D, pointer)
    }

    fun bind(index: Int, nearest: GPUFiltering) {
        activeSlot(index)
        if (pointer > -1 && isCreated) {
            bindTexture(GL_TEXTURE_3D, pointer)
            ensureFiltering(nearest)
        } else {
            invisibleTexture.bind(index, GPUFiltering.LINEAR, Clamping.CLAMP)
        }
    }

    override fun destroy() {
        val pointer = pointer
        if (pointer > -1) {
            if (GFX.isGFXThread()) destroy(pointer)
            else GFX.addGPUTask(1) { destroy(pointer) }
        }
        this.pointer = -1
    }

    private fun destroy(pointer: Int) {
        DebugGPUStorage.tex3d.remove(this)
        glDeleteTextures(pointer)
        Texture2D.invalidateBinding()
        isDestroyed = true
    }

}