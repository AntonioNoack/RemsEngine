package me.anno.gpu.texture

import me.anno.Build
import me.anno.gpu.DepthMode
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
import me.anno.gpu.texture.Texture2D.Companion.setWriteAlignment
import me.anno.gpu.texture.Texture2D.Companion.switchRGB2BGR
import me.anno.gpu.texture.Texture2D.Companion.texturesToDelete
import me.anno.gpu.texture.TextureLib.invisibleTex3d
import me.anno.utils.callbacks.I3B
import me.anno.utils.callbacks.I3I
import me.anno.image.Image
import me.anno.utils.Color.convertARGB2RGBA
import me.anno.utils.types.Booleans.toInt
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GL46C.GL_BGRA
import org.lwjgl.opengl.GL46C.GL_COMPARE_REF_TO_TEXTURE
import org.lwjgl.opengl.GL46C.GL_FALSE
import org.lwjgl.opengl.GL46C.GL_FLOAT
import org.lwjgl.opengl.GL46C.GL_NONE
import org.lwjgl.opengl.GL46C.GL_ONE
import org.lwjgl.opengl.GL46C.GL_R16
import org.lwjgl.opengl.GL46C.GL_R16F
import org.lwjgl.opengl.GL46C.GL_R32F
import org.lwjgl.opengl.GL46C.GL_R8
import org.lwjgl.opengl.GL46C.GL_RED
import org.lwjgl.opengl.GL46C.GL_RGBA
import org.lwjgl.opengl.GL46C.GL_RGBA32F
import org.lwjgl.opengl.GL46C.GL_RGBA8
import org.lwjgl.opengl.GL46C.GL_TEXTURE
import org.lwjgl.opengl.GL46C.GL_TEXTURE_2D_ARRAY
import org.lwjgl.opengl.GL46C.GL_TEXTURE_3D
import org.lwjgl.opengl.GL46C.GL_TEXTURE_COMPARE_FUNC
import org.lwjgl.opengl.GL46C.GL_TEXTURE_COMPARE_MODE
import org.lwjgl.opengl.GL46C.GL_TEXTURE_LOD_BIAS
import org.lwjgl.opengl.GL46C.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL46C.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL46C.GL_TRUE
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL46C.glGenerateMipmap
import org.lwjgl.opengl.GL46C.glObjectLabel
import org.lwjgl.opengl.GL46C.glTexImage3D
import org.lwjgl.opengl.GL46C.glTexParameterf
import org.lwjgl.opengl.GL46C.glTexParameteri
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

// is this correct?
open class Texture2DArray(
    override var name: String,
    override var width: Int,
    override var height: Int,
    var layers: Int
) : ITexture2D {

    var pointer = 0
    var session = -1

    override var wasCreated = false
    override var isDestroyed = false
    override var filtering = Filtering.TRULY_LINEAR
    override var clamping = Clamping.CLAMP

    // todo set this when the texture is created
    override var channels: Int = 3

    var needsMipmaps = false

    override val samples: Int get() = 1

    override var locallyAllocated = 0L
    override var internalFormat = 0

    var border = 0

    val target get() = GL_TEXTURE_2D_ARRAY

    override var isHDR = false

    fun checkSession() {
        if (session != GFXState.session) {
            session = GFXState.session
            pointer = 0
            wasCreated = false
            isDestroyed = false
        }
    }

    // needs to be public for JVM2WASM
    fun ensurePointer() {
        checkSession()
        if (pointer == 0) pointer = Texture2D.createTexture()
        if (pointer == 0) throw RuntimeException("Could not generate texture")
        if (Build.isDebug) synchronized(DebugGPUStorage.tex2da) {
            DebugGPUStorage.tex2da.add(this)
        }
        isDestroyed = false
    }

    private fun bindBeforeUpload() {
        ensurePointer()
        forceBind()
    }

    private fun bindBeforeUpload(alignment: Int) {
        bindBeforeUpload()
        setWriteAlignment(alignment)
    }

    private fun afterUpload(internalFormat: Int, bpp: Int, hdr: Boolean) {
        wasCreated = true
        this.internalFormat = internalFormat
        locallyAllocated = allocate(locallyAllocated, width.toLong() * height.toLong() * layers.toLong() * bpp)
        filtering(filtering)
        clamping(clamping)
        isHDR = hdr
        GFX.check()
        when (internalFormat) {
            GL_R8, GL_R16, GL_R16F,
            GL_R32F -> swizzleMonochrome()
        }
        if (Build.isDebug) glObjectLabel(GL_TEXTURE, pointer, name)
    }

    @Suppress("unused")
    fun createRGBA8() {
        bindBeforeUpload(width * 4)
        glTexImage3D(
            GL_TEXTURE_3D, 0, GL_RGBA8, width, height, layers, 0,
            GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?
        )
        afterUpload(GL_RGBA8, 4, false)
    }

    @Suppress("unused")
    fun createRGBAFP32() {
        bindBeforeUpload(width * 16)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, width, height, layers, 0, GL_RGBA, GL_FLOAT, null as ByteBuffer?)
        afterUpload(GL_RGBA32F, 16, true)
    }

    fun create(img: Image, sync: Boolean) {
        // todo we could detect monochrome and such :)
        val intData = img.createIntImage().data
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            // argb -> abgr
            switchRGB2BGR(intData)
        } else {
            for (i in intData.indices) {// argb -> rgba
                intData[i] = convertARGB2RGBA(intData[i])
            }
        }
        if (sync) createRGBA8(intData)
        else GFX.addGPUTask("Texture3D.create()", img.width, img.height) {
            createRGBA8(intData)
        }
    }

    fun create(images: Array<Image>, sync: Boolean) {
        // todo we could detect monochrome and such :)
        val intData = IntArray(width * height * layers)
        var i0 = 0
        for (image in images) {
            val data = image.createIntImage().data
            data.copyInto(intData, i0)
            i0 += data.size
        }
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            // argb -> abgr
            switchRGB2BGR(intData)
        } else {
            for (i in intData.indices) {// argb -> rgba
                intData[i] = convertARGB2RGBA(intData[i])
            }
        }
        if (sync) createRGBA8(intData)
        else GFX.addGPUTask("Texture3D.create()", width, height * layers) {
            createRGBA8(intData)
        }
    }

    fun createRGBA8(data: IntArray) {
        bindBeforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, layers, 0, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
    }

    @Suppress("unused")
    fun createRGB8(data: IntArray) {
        bindBeforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, layers, 0, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 3, false)
    }

    fun createBGRA8(data: ByteBuffer) {
        bindBeforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, layers, 0, GL_BGRA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
    }

    @Suppress("unused")
    fun createBGR8(data: ByteBuffer) {
        bindBeforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, layers, 0, GL_BGRA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
    }

    fun createMonochrome(data: ByteArray) {
        if (width * height * layers != data.size) throw RuntimeException("incorrect size!")
        val byteBuffer = bufferPool[data.size, false, false]
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.position(0)
        createMonochrome(byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
    }

    fun createMonochrome(getValue: I3B) {
        val w = width
        val h = height
        val d = layers
        val size = w * h * d
        val byteBuffer = bufferPool[size, false, false]
        for (z in 0 until d) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    byteBuffer.put(getValue.run(x, y, z))
                }
            }
        }
        byteBuffer.flip()
        createMonochrome(byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
    }

    fun createRGBA8(getValue: I3I) {
        val w = width
        val h = height
        val d = layers
        val size = 4 * w * h * d
        val byteBuffer = bufferPool[size, false, false]
        for (z in 0 until d) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    byteBuffer.putInt(getValue.run(x, y, z))
                }
            }
        }
        byteBuffer.flip()
        createBGRA8(byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
    }

    fun createMonochrome(data: ByteBuffer) {
        if (width * height * layers != data.remaining()) throw RuntimeException("incorrect size!")
        bindBeforeUpload(width)
        glTexImage3D(target, 0, GL_R8, width, height, layers, 0, GL_RED, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_R8, 1, false)
    }

    fun create(type: TargetType, data: ByteArray? = null) {
        // might be incorrect for RGB!!
        if (data != null && type.bytesPerPixel != 3 && width * height * layers * type.bytesPerPixel != data.size)
            throw RuntimeException("incorrect size!, got ${data.size}, expected $width * $height * $layers * ${type.bytesPerPixel} bpp")
        val byteBuffer = if (data != null) {
            val byteBuffer = bufferPool[data.size, false, false]
            byteBuffer.position(0)
            byteBuffer.put(data)
            byteBuffer.position(0)
            byteBuffer
        } else null
        bindBeforeUpload(width)
        glTexImage3D(
            target, 0, type.internalFormat, width, height, layers, 0,
            type.uploadFormat, type.fillType, byteBuffer
        )
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(type.internalFormat, type.bytesPerPixel, type.isHDR)
    }

    fun createRGBA(data: FloatArray) {
        if (width * height * layers * 4 != data.size) throw RuntimeException("incorrect size!, got ${data.size}, expected $width * $height * $layers * 4 bpp")
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
        bindBeforeUpload(width * 16)
        glTexImage3D(target, 0, GL_RGBA32F, width, height, layers, 0, GL_RGBA, GL_FLOAT, floatBuffer)
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(GL_RGBA32F, 16, true)
    }

    fun createRGBA(data: ByteArray) {
        if (width * height * layers * 4 != data.size) throw RuntimeException("incorrect size!, got ${data.size}, expected $width * $height * $layers * 4 bpp")
        val byteBuffer = bufferPool[data.size, false, false]
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.flip()
        bindBeforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, layers, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(GL_RGBA8, 4, false)
    }

    fun createRGBA(data: ByteBuffer) {
        if (width * height * layers * 4 != data.remaining()) throw RuntimeException("incorrect size!, got ${data.remaining()}, expected $width * $height * $layers * 4 bpp")
        bindBeforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, layers, 0, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
    }

    fun createDepth(lowQuality: Boolean = false) {
        create(if (lowQuality) TargetType.DEPTH16 else TargetType.DEPTH32F)
    }

    fun ensureFiltering(nearest: Filtering, clamping: Clamping) {
        if (nearest != filtering) filtering(nearest)
        if (clamping != this.clamping) clamping(clamping)
    }

    var hasMipmap = false
    var autoUpdateMipmaps = true

    override var depthFunc: DepthMode? = null
        set(value) {
            if (field != value) {
                field = value
                if (GFX.supportsDepthTextures) {
                    bindBeforeUpload()
                    val mode = if (value == null) GL_NONE else GL_COMPARE_REF_TO_TEXTURE
                    glTexParameteri(target, GL_TEXTURE_COMPARE_MODE, mode)
                    if (value != null) glTexParameteri(target, GL_TEXTURE_COMPARE_FUNC, value.id)
                }
            }
        }

    fun filtering(filtering: Filtering) {
        if (!hasMipmap && filtering.needsMipmap && (width > 1 || height > 1)) {
            glGenerateMipmap(target)
            hasMipmap = true
            if (GFX.supportsAnisotropicFiltering) {
                val anisotropy = GFX.anisotropy
                glTexParameteri(target, GL_TEXTURE_LOD_BIAS, 0)
                glTexParameterf(target, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy)
            }
            // whenever the base mipmap is changed, the mipmaps will be updated :)
            // todo it seems like this needs to be called manually in WebGL
            glTexParameteri(target, GL14.GL_GENERATE_MIPMAP, if (autoUpdateMipmaps) GL_TRUE else GL_FALSE)
            // is called afterward anyway
            // glTexParameteri(tex2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        }
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, filtering.min)
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, filtering.mag)
        this.filtering = filtering
    }

    fun clamping(clamping: Clamping) {
        TextureHelper.clamping(target, clamping.mode, border)
        this.clamping = clamping
    }

    private fun forceBind() {
        if (pointer == 0) throw RuntimeException()
        bindTexture(target, pointer)
    }

    override fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean {
        activeSlot(index)
        if (pointer != 0 && wasCreated) {
            bindTexture(target, pointer)
            ensureFiltering(filtering, clamping)
        } else {
            invisibleTex3d.bind(index, Filtering.LINEAR, Clamping.CLAMP)
        }
        return true
    }

    override fun wrapAsFramebuffer(): IFramebuffer {
        throw NotImplementedError()
    }

    override fun destroy() {
        val pointer = pointer
        if (pointer != 0) {
            destroy(pointer)
        }
        this.pointer = 0
    }

    private fun destroy(pointer: Int) {
        if (Build.isDebug) synchronized(DebugGPUStorage.tex2da) {
            DebugGPUStorage.tex2da.remove(this)
        }
        synchronized(texturesToDelete) {
            // allocation counter is removed a bit early, shouldn't be too bad
            locallyAllocated = allocate(locallyAllocated, 0L)
            texturesToDelete.add(pointer)
        }
        isDestroyed = true
    }

    override fun createImage(flipY: Boolean, withAlpha: Boolean) =
        VRAMToRAM.createImage(width * layers, height, VRAMToRAM.zero, flipY, withAlpha) { x2, y2, w2, _ ->
            drawSlice(x2, y2, w2, withAlpha)
        }

    private fun drawSlice(x2: Int, y2: Int, w2: Int, withAlpha: Boolean) {
        val z0 = x2 / width
        val z1 = (x2 + w2 - 1) / width
        drawSlice(x2, y2, z0 / maxOf(1f, layers - 1f), withAlpha)
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
        GFXx2D.posSize(shader, x, GFX.viewportHeight - y, width, -height)
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

    fun swizzle(r: Int, g: Int, b: Int, a: Int) {
        TextureHelper.swizzle(target, r, g, b, a)
    }

    companion object {
        var allocated = 0L
        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated += newValue - oldValue
            return newValue
        }
    }
}