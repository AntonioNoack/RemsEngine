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
import me.anno.gpu.texture.TextureLib.invisibleTex3d
import me.anno.image.Image
import me.anno.utils.callbacks.I3B
import me.anno.utils.callbacks.I3I
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C.GL_BGRA
import org.lwjgl.opengl.GL46C.GL_FLOAT
import org.lwjgl.opengl.GL46C.GL_LINEAR
import org.lwjgl.opengl.GL46C.GL_NEAREST
import org.lwjgl.opengl.GL46C.GL_ONE
import org.lwjgl.opengl.GL46C.GL_R8
import org.lwjgl.opengl.GL46C.GL_RED
import org.lwjgl.opengl.GL46C.GL_RGBA
import org.lwjgl.opengl.GL46C.GL_RGBA32F
import org.lwjgl.opengl.GL46C.GL_RGBA8
import org.lwjgl.opengl.GL46C.GL_TEXTURE
import org.lwjgl.opengl.GL46C.GL_TEXTURE_3D
import org.lwjgl.opengl.GL46C.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL46C.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL46C.glObjectLabel
import org.lwjgl.opengl.GL46C.glTexImage3D
import org.lwjgl.opengl.GL46C.glTexParameteri
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

open class Texture3D(
    override var name: String,
    override var width: Int,
    override var height: Int,
    var depth: Int
) : ITexture2D {

    var pointer = 0
    var session = -1

    override var wasCreated = false
    override var isDestroyed = false
    override var filtering = Filtering.NEAREST
    override var clamping = Clamping.CLAMP

    // todo set this when the texture is created
    override var channels: Int = 3

    override var locallyAllocated = 0L
    override var internalFormat = 0

    var border = 0

    override val samples: Int get() = 1

    val target get() = GL_TEXTURE_3D

    override var depthFunc: DepthMode?
        get() = null
        set(value) {
            LOGGER.warn("Texture3D doesn't support depth")
        }

    override var isHDR = false

    fun checkSession() {
        if (session != GFXState.session) {
            session = GFXState.session
            pointer = 0
            wasCreated = false
            isDestroyed = false
        }
    }

    private fun ensurePointer() {
        checkSession()
        if (pointer == 0) pointer = Texture2D.createTexture()
        if (pointer == 0) throw RuntimeException("Could not generate texture")
        if (Build.isDebug) synchronized(DebugGPUStorage.tex3d) {
            DebugGPUStorage.tex3d.add(this)
        }
        isDestroyed = false
    }

    private fun beforeUpload(alignment: Int) {
        ensurePointer()
        forceBind()
        setWriteAlignment(alignment)
    }

    private fun afterUpload(internalFormat: Int, bpp: Int, hdr: Boolean) {
        wasCreated = true
        this.internalFormat = internalFormat
        locallyAllocated = allocate(locallyAllocated, width.toLong() * height.toLong() * depth.toLong() * bpp)
        filtering(filtering)
        clamping(clamping)
        isHDR = hdr
        GFX.check()
        if (TextureHelper.getNumChannels(internalFormat) == 1) {
            swizzleMonochrome()
        }
        if (Build.isDebug) glObjectLabel(GL_TEXTURE, pointer, name)
    }

    @Suppress("unused")
    fun createRGBA8() {
        beforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, depth, 0, GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)
        afterUpload(GL_RGBA8, 4, false)
    }

    @Suppress("unused")
    fun createRGBAFP32() {
        beforeUpload(width * 16)
        glTexImage3D(target, 0, GL_RGBA32F, width, height, depth, 0, GL_RGBA, GL_FLOAT, null as ByteBuffer?)
        afterUpload(GL_RGBA32F, 16, true)
    }

    fun create(img: Image, sync: Boolean) {
        // todo we could detect monochrome and such :)
        val intData = img.createIntImage().data
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
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
        beforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, depth, 0, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
    }

    @Suppress("unused")
    fun createRGB8(data: IntArray) {
        beforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, depth, 0, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 3, false)
    }

    fun createBGRA8(data: ByteBuffer) {
        beforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, depth, 0, GL_BGRA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
    }

    @Suppress("unused")
    fun createBGR8(data: ByteBuffer) {
        beforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, depth, 0, GL_BGRA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
    }

    fun createMonochrome(data: ByteArray) {
        if (width * height * depth != data.size) throw RuntimeException("incorrect size!")
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
        val d = depth
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
        val d = depth
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
        if (width * height * depth != data.remaining()) throw RuntimeException("incorrect size!")
        beforeUpload(width)
        glTexImage3D(target, 0, GL_R8, width, height, depth, 0, GL_RED, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_R8, 1, false)
    }

    fun create(type: TargetType, data: ByteArray? = null) {
        // might be incorrect for RGB!!
        if (data != null && type.bytesPerPixel != 3 && width * height * depth * type.bytesPerPixel != data.size)
            throw RuntimeException("incorrect size!, got ${data.size}, expected $width * $height * $depth * ${type.bytesPerPixel} bpp")
        val byteBuffer = if (data != null) {
            val byteBuffer = bufferPool[data.size, false, false]
            byteBuffer.position(0)
            byteBuffer.put(data)
            byteBuffer.position(0)
            byteBuffer
        } else null
        beforeUpload(width)
        glTexImage3D(
            target, 0, type.internalFormat, width, height, depth, 0,
            type.uploadFormat, type.fillType, byteBuffer
        )
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(type.internalFormat, type.bytesPerPixel, type.isHDR)
    }

    fun createRGBA(data: FloatArray) {
        if (width * height * depth * 4 != data.size) throw RuntimeException("incorrect size!, got ${data.size}, expected $width * $height * $depth * 4 bpp")
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
        beforeUpload(width * 16)
        glTexImage3D(target, 0, GL_RGBA32F, width, height, depth, 0, GL_RGBA, GL_FLOAT, floatBuffer)
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(GL_RGBA32F, 16, true)
    }

    fun createRGBA(data: ByteArray) {
        if (width * height * depth * 4 != data.size) throw RuntimeException("incorrect size!, got ${data.size}, expected $width * $height * $depth * 4 bpp")
        val byteBuffer = bufferPool[data.size, false, false]
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.flip()
        beforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, depth, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(GL_RGBA8, 4, false)
    }

    fun createRGBA(data: ByteBuffer) {
        if (width * height * depth * 4 != data.remaining()) throw RuntimeException("incorrect size!, got ${data.remaining()}, expected $width * $height * $depth * 4 bpp")
        beforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, depth, 0, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
    }

    fun ensureFiltering(nearest: Filtering, clamping: Clamping) {
        if (nearest != filtering) filtering(nearest)
        if (clamping != this.clamping) clamping(clamping)
    }

    fun filtering(nearest: Filtering) {
        if (nearest != Filtering.LINEAR) {
            glTexParameteri(target, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(target, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        } else {
            glTexParameteri(target, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(target, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        }
        filtering = nearest
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
        if (pointer != 0) destroy(pointer)
        this.pointer = 0
    }

    private fun destroy(pointer: Int) {
        if (Build.isDebug) synchronized(DebugGPUStorage.tex3d) {
            DebugGPUStorage.tex3d.remove(this)
        }
        synchronized(Texture2D.texturesToDelete) {
            // allocation counter is removed a bit early, shouldn't be too bad
            locallyAllocated = Texture2D.allocate(locallyAllocated, 0L)
            Texture2D.texturesToDelete.add(pointer)
        }
        locallyAllocated = allocate(locallyAllocated, 0L)
        isDestroyed = true
    }

    override fun createImage(flipY: Boolean, withAlpha: Boolean) =
        VRAMToRAM.createImage(width * depth, height, VRAMToRAM.zero, flipY, withAlpha) { x2, y2, w2, _ ->
            drawSlice(x2, y2, w2, withAlpha)
        }

    private fun drawSlice(x2: Int, y2: Int, w2: Int, withAlpha: Boolean) {
        val z0 = x2 / width
        val z1 = (x2 + w2 - 1) / width
        drawSlice(x2, y2, z0 / maxOf(1f, depth - 1f), withAlpha)
        if (z1 > z0) {
            // todo we have to draw two slices
            // drawSlice(x2, y2, z0 / maxOf(1f, depth - 1f), withAlpha)
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
        private val LOGGER = LogManager.getLogger(Texture3D::class)
        var allocated = 0L
        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated += newValue - oldValue
            return newValue
        }
    }
}