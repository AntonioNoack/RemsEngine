package me.anno.gpu.texture

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R
import org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP
import org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X
import org.lwjgl.opengl.GL30

// todo test it
// todo can be used e.g. for game engine for environment & irradiation maps
class TextureCubemap : ICacheData {

    var w = 0
    var h = 0

    var isCreated = false
    var isDestroyed = false
    var pointer = -1

    var locallyAllocated = 0L

    val tex2D = GL_TEXTURE_CUBE_MAP

    fun ensurePointer() {
        if (isDestroyed) throw RuntimeException("Texture was destroyed")
        if (pointer < 0) {
            GFX.check()
            pointer = Texture2D.createTexture()
            // many textures can be created by the console log and the fps viewer constantly xD
            // maybe we should use allocation free versions there xD
            GFX.check()
        }
        if (pointer <= 0) throw RuntimeException("Could not allocate texture pointer")
    }

    private fun bindBeforeUpload() {
        if (pointer == -1) throw RuntimeException("Pointer must be defined")
        Texture2D.bindTexture(tex2D, pointer)
    }

    private fun checkSize(channels: Int, size: Int) {
        if (size < w * h * channels) throw IllegalArgumentException("Incorrect size, ${w * h * channels} vs ${size}!")
    }

    private fun beforeUpload(channels: Int, size: Int) {
        if (isDestroyed) throw RuntimeException("Texture is already destroyed, call reset() if you want to stream it")
        checkSize(channels, size)
        GFX.check()
        ensurePointer()
        bindBeforeUpload()
    }

    fun create(sides: List<ByteArray>) {
        bindBeforeUpload()
        val byteBuffer = Texture2D.byteBufferPool[w * h * 3, false]
        for (i in 0 until 6) {
            byteBuffer.position(0)
            byteBuffer.put(sides[i])
            byteBuffer.position(0)
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer)
        }
        Texture2D.byteBufferPool.returnBuffer(byteBuffer)
        afterUpload(6 * 3)
    }

    private fun afterUpload(bytesPerPixel: Int) {
        locallyAllocated = Texture2D.allocate(locallyAllocated, w * h * bytesPerPixel.toLong())
        isCreated = true
        filtering(filtering)
        clamping()
        GFX.check()
        if (isDestroyed) destroy()
    }

    private fun clamping() {
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)
    }

    var hasMipmap = false
    var filtering: GPUFiltering = GPUFiltering.TRULY_NEAREST

    private fun filtering(nearest: GPUFiltering) {
        if (!hasMipmap && nearest.needsMipmap) {
            GL30.glGenerateMipmap(tex2D)
            hasMipmap = true
            if (GFX.supportsAnisotropicFiltering) {
                val anisotropy = GFX.anisotropy
                glTexParameteri(tex2D, GL30.GL_TEXTURE_LOD_BIAS, 0)
                glTexParameterf(tex2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy)
            }
            glTexParameteri(tex2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        }
        glTexParameteri(tex2D, GL_TEXTURE_MIN_FILTER, nearest.min)
        glTexParameteri(tex2D, GL_TEXTURE_MAG_FILTER, nearest.mag)
        this.filtering = nearest
    }

    fun ensureFilterAndClamping(nearest: GPUFiltering, clamping: Clamping) {
        // ensure being bound?
        if (nearest != this.filtering) filtering(nearest)
    }


    fun reset() {
        isDestroyed = false
    }

    override fun destroy() {
        isCreated = false
        isDestroyed = true
        val pointer = pointer
        if (pointer > -1) {
            if (!GFX.isGFXThread()) {
                GFX.addGPUTask(1) {
                    Texture2D.invalidateBinding()
                    locallyAllocated = Texture2D.allocate(locallyAllocated, 0L)
                    Texture2D.texturesToDelete.add(pointer)
                }
            } else {
                Texture2D.invalidateBinding()
                locallyAllocated = Texture2D.allocate(locallyAllocated, 0L)
                Texture2D.texturesToDelete.add(pointer)
            }
        }
        this.pointer = -1
    }

}