package me.anno.gpu.texture

import me.anno.Build
import me.anno.cache.ICacheData
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.maths.Maths
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL30C.*
import org.lwjgl.opengl.GL32C.GL_TEXTURE_CUBE_MAP_SEAMLESS
import java.nio.ByteBuffer
import kotlin.math.PI

// can be used e.g. for game engine for environment & irradiation maps
// todo multi-sampled environment maps, because some gpus may handle them just fine :3

open class CubemapTexture(
    var name: String,
    var size: Int,
    override val samples: Int
) : ICacheData, ITexture2D {

    var isCreated = false
    var isDestroyed = false
    var pointer = 0
    var session = 0
    var createdSize = 0

    var locallyAllocated = 0L
    var internalFormat = 0

    override var isHDR = false

    override var width: Int
        get() = size
        set(value) {
            size = value
        }

    override var height: Int
        get() = size
        set(_) {}

    private val target = GL_TEXTURE_CUBE_MAP

    var needsMipmaps = false

    private fun ensurePointer() {
        if (isDestroyed) throw RuntimeException("Texture was destroyed")
        if (pointer == 0) {
            GFX.check()
            pointer = Texture2D.createTexture()
            // many textures can be created by the console log and the fps viewer constantly xD
            // maybe we should use allocation free versions there xD
            GFX.check()
            if (pointer == 0) throw RuntimeException("Could not allocate texture pointer")
            if (Build.isDebug) synchronized(DebugGPUStorage.tex3dCs) {
                DebugGPUStorage.tex3dCs.add(this)
            }
        }
    }

    fun checkSession() {
        if (session != GFXState.session) {
            session = GFXState.session
            pointer = 0
            isCreated = false
            isDestroyed = false
            locallyAllocated = Texture2D.allocate(locallyAllocated, 0L)
            createdSize = 0
        }
    }

    private fun bindBeforeUpload() {
        if (pointer == 0) throw RuntimeException("Pointer must be defined")
        Texture2D.bindTexture(target, pointer)
    }

    private fun checkSize(channels: Int, size0: Int) {
        if (size0 < size * size * channels) throw IllegalArgumentException("Incorrect size, ${size * size * channels} vs ${size0}!")
    }

    private fun beforeUpload(channels: Int, size: Int) {
        if (isDestroyed) throw RuntimeException("Texture is already destroyed, call reset() if you want to stream it")
        checkSize(channels, size)
        GFX.check()
        ensurePointer()
        bindBeforeUpload()
        GFX.check()
    }

    @Suppress("unused")
    fun createRGB(sides: List<ByteArray>) {
        beforeUpload(3, sides[0].size)
        val size = size
        val byteBuffer = Texture2D.bufferPool[size * size * 3, false, false]
        val internalFormat = GL_RGBA8
        for (i in 0 until 6) {
            byteBuffer.position(0)
            byteBuffer.put(sides[i])
            byteBuffer.flip()
            glTexImage2D(
                getTarget(i), 0, internalFormat,
                size, size, 0, GL_RGB, GL_UNSIGNED_BYTE, byteBuffer
            )
        }
        Texture2D.bufferPool.returnBuffer(byteBuffer)
        afterUpload(internalFormat, 6 * 3)
    }

    fun createRGBA(sides: List<ByteArray>) {
        beforeUpload(4, sides[0].size)
        val size = size
        val byteBuffer = Texture2D.bufferPool[size * size * 4, false, false]
        val internalFormat = GL_RGBA8
        for (i in 0 until 6) {
            byteBuffer.position(0)
            byteBuffer.put(sides[i])
            byteBuffer.position(0)
            glTexImage2D(
                getTarget(i), 0, internalFormat,
                size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer
            )
        }
        Texture2D.bufferPool.returnBuffer(byteBuffer)
        afterUpload(internalFormat, 6 * 4)
    }

    fun create(type: TargetType) {
        beforeUpload(0, 0)
        val size = size
        OpenGLBuffer.bindBuffer(GL_PIXEL_UNPACK_BUFFER, 0)
        for (i in 0 until 6) {
            glTexImage2D(
                getTarget(i), 0, type.internalFormat, size, size,
                0, type.uploadFormat, type.fillType, null as ByteBuffer?
            )
        }
        afterUpload(type.internalFormat, type.bytesPerPixel)
    }

    private fun getTarget(side: Int) = GL_TEXTURE_CUBE_MAP_POSITIVE_X + side

    fun createDepth(lowQuality: Boolean = false) {
        create(if (lowQuality) TargetType.DEPTH16 else TargetType.DEPTH32F)
    }

    private fun afterUpload(internalFormat: Int, bytesPerPixel: Int) {
        GFX.check()
        this.internalFormat = internalFormat
        locallyAllocated = allocate(locallyAllocated, size * size * bytesPerPixel.toLong())
        isCreated = true
        filtering(filtering)
        clamping()
        GFX.check()
        createdSize = size
        if (isDestroyed) destroy()
    }

    private fun isBoundToSlot(slot: Int): Boolean {
        return Texture2D.boundTextures[slot] == pointer
    }

    fun bind(index: Int, nearest: Filtering): Boolean {
        if (pointer != 0 && isCreated) {
            if (isBoundToSlot(index)) return false
            Texture2D.activeSlot(index)
            val result = Texture2D.bindTexture(target, pointer)
            ensureFilterAndClamping(nearest)
            if (needsMipmaps && nearest.needsMipmap && (width > 1 || height > 1)) {
                needsMipmaps = false
                glGenerateMipmap(target)
            }
            return result
        } else throw IllegalStateException("Cannot bind non-created texture!")
    }

    override fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean {
        return bind(index, filtering)
    }

    override fun wrapAsFramebuffer(): IFramebuffer {
        throw NotImplementedError()
    }

    private fun clamping() {
        glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS)
        // glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        // glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        // glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)
    }

    var hasMipmap = false
    var filtering: Filtering = Filtering.TRULY_NEAREST

    var autoUpdateMipmaps = true

    private fun filtering(nearest: Filtering) {
        if (!hasMipmap && nearest.needsMipmap && samples <= 1) {
            // todo use a better algorithm for these mipmaps:
            //  the native algorithm generates blocky artefacts, we need gaussian blur, or similar,
            //  also ideally in pow(color,2.2) space
            glGenerateMipmap(target)
            hasMipmap = true
            if (GFX.supportsAnisotropicFiltering) {
                val anisotropy = GFX.anisotropy
                glTexParameteri(target, GL_TEXTURE_LOD_BIAS, 0)
                glTexParameterf(target, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy)
            }
            // automatic mipmap updates are not supported
            needsMipmaps = true
        } else if (!nearest.needsMipmap) needsMipmaps = false
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, nearest.min)
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, nearest.mag)
        this.filtering = nearest
    }

    private fun ensureFilterAndClamping(nearest: Filtering) {
        // ensure being bound?
        if (nearest != this.filtering) filtering(nearest)
    }

    override var depthFunc: DepthMode? = null
        set(value) {
            if (field != value) {
                field = value
                bindBeforeUpload()
                val mode = if (value == null) GL_NONE else GL_COMPARE_REF_TO_TEXTURE
                glTexParameteri(target, GL_TEXTURE_COMPARE_MODE, mode)
                if (value != null) glTexParameteri(target, GL_TEXTURE_COMPARE_FUNC, value.id)
            }
        }

    fun reset() {
        isDestroyed = false
    }

    override fun destroy() {
        isCreated = false
        isDestroyed = true
        val pointer = pointer
        if (pointer != 0) {
            if (GFX.isGFXThread()) destroy(pointer)
            else GFX.addGPUTask("CubemapTexture.destroy()", 1) { destroy(pointer) }
        }
        this.pointer = 0
    }

    private fun destroy(pointer: Int) {
        if (Build.isDebug) synchronized(DebugGPUStorage.tex3dCs) {
            DebugGPUStorage.tex3dCs.remove(this)
        }
        GFX.checkIsGFXThread()
        Texture2D.invalidateBinding()
        locallyAllocated = allocate(locallyAllocated, 0L)
        Texture2D.texturesToDelete.add(pointer)
    }

    companion object {

        /**
         * cubemaps are in a left-handed coordinate system in OpenGL, because apparently Renderman did it that way;
         * the rest of OpenGL is right-handed, so we get a mismatch, which we can fix by multiplying the uvw with this vector
         * */
        val cubemapsAreLeftHanded = "vec3(-1.0,1.0,1.0)"

        var allocated = 0L
        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated += newValue - oldValue
            return newValue
        }

        fun rotateForCubemap(dst: Quaterniond, side: Int) {
            // rotate based on direction
            // POSITIVE_X, NEGATIVE_X, POSITIVE_Y, NEGATIVE_Y, POSITIVE_Z, NEGATIVE_Z
            when (side) {
                0 -> dst.rotateY(+PI * 0.5)
                1 -> dst.rotateY(-PI * 0.5)
                2 -> dst.rotateX(+PI * 0.5)
                3 -> dst.rotateX(-PI * 0.5)
                // 4 is already correct
                5 -> dst.rotateY(PI)
            }
        }

        fun rotateForCubemap(dst: Quaternionf, side: Int) {
            // rotate based on direction
            // POSITIVE_X, NEGATIVE_X, POSITIVE_Y, NEGATIVE_Y, POSITIVE_Z, NEGATIVE_Z
            when (side) {
                0 -> dst.rotateY(+Maths.PIf * 0.5f)
                1 -> dst.rotateY(-Maths.PIf * 0.5f)
                2 -> dst.rotateX(+Maths.PIf * 0.5f)
                3 -> dst.rotateX(-Maths.PIf * 0.5f)
                // 4 is already correct
                5 -> dst.rotateY(Maths.PIf)
            }
        }
    }
}