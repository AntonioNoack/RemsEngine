package me.anno.gpu.texture

import me.anno.Build
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.INVALID_POINTER
import me.anno.gpu.GFX.INVALID_SESSION
import me.anno.gpu.GFX.isPointerValid
import me.anno.gpu.GFXState
import me.anno.gpu.GLNames
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.framebuffer.VRAMToRAM
import me.anno.gpu.shader.FlatShaders
import me.anno.gpu.texture.Texture2D.Companion.activeSlot
import me.anno.gpu.texture.Texture2D.Companion.bindTexture
import me.anno.gpu.texture.Texture2D.Companion.setWriteAlignment
import me.anno.gpu.texture.TextureLib.invisibleTex3d
import me.anno.image.Image
import me.anno.utils.Color.convertARGB2ABGR
import me.anno.utils.Color.convertARGB2RGBA
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.callbacks.I3B
import me.anno.utils.callbacks.I3I
import me.anno.utils.pooling.Pools.byteBufferPool
import me.anno.utils.pooling.Pools.intArrayPool
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C.GL_BGRA
import org.lwjgl.opengl.GL46C.GL_FLOAT
import org.lwjgl.opengl.GL46C.GL_LINEAR
import org.lwjgl.opengl.GL46C.GL_NEAREST
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
import java.util.concurrent.atomic.AtomicLong

open class Texture3D(
    override var name: String,
    override var width: Int,
    override var height: Int,
    var depth: Int
) : ITexture2D {

    var pointer = INVALID_POINTER
    var session = INVALID_SESSION

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

    override fun checkSession() {
        if (session != GFXState.session) {
            session = GFXState.session
            pointer = INVALID_POINTER
            wasCreated = false
            isDestroyed = false
        }
    }

    private fun ensurePointer() {
        checkSession()
        if (!isPointerValid(pointer)) pointer = Texture2D.createTexture()
        assertNotEquals(0, pointer, "Could not generate texture")
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

    fun afterUpload(internalFormat: Int, bpp: Int, hdr: Boolean) {
        wasCreated = true
        this.internalFormat = internalFormat
        locallyAllocated = allocate(locallyAllocated, width.toLong() * height.toLong() * depth.toLong() * bpp)
        filtering(filtering)
        clamping(clamping)
        isHDR = hdr
        GFX.check()
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

    fun create(img: Image): Texture3D {
        // todo we could detect monochrome, float-precision and such :)
        assertTrue(img.width >= width * depth)
        assertTrue(img.height >= height)
        val intData = intArrayPool[width * height * depth, false, false]
        fillIntData(img, intData)
        convertToRGBAForGPU(intData)
        addGPUTask("Texture3D.create()", img.width, img.height) {
            createRGBA8(intData)
            intArrayPool.returnBuffer(intData)
        }
        return this
    }

    private fun fillIntData(img: Image, intData: IntArray) {
        var k = 0
        repeat(depth) { z ->
            repeat(height) { y ->
                repeat(width) { x ->
                    intData[k++] = img.getRGB(z * width + x, y)
                }
            }
        }
    }

    private fun convertToRGBAForGPU(intData: IntArray) {
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            convertARGB2ABGR(intData)
        } else {
            convertARGB2RGBA(intData)
        }
    }

    fun createRGBA8(data: IntArray): Texture3D {
        beforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, depth, 0, GL_BGRA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
        return this
    }

    @Suppress("unused")
    fun createRGB8(data: IntArray): Texture3D {
        beforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, depth, 0, GL_BGRA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 3, false)
        return this
    }

    fun createBGRA8(data: ByteBuffer): Texture3D {
        beforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, depth, 0, GL_BGRA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
        return this
    }

    @Suppress("unused")
    fun createBGR8(data: ByteBuffer): Texture3D {
        beforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, depth, 0, GL_BGRA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
        return this
    }

    fun createMonochrome(data: ByteArray): Texture3D {
        if (width * height * depth != data.size) throw RuntimeException("incorrect size!")
        val byteBuffer = byteBufferPool[data.size, false, false]
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.position(0)
        createMonochrome(byteBuffer)
        byteBufferPool.returnBuffer(byteBuffer)
        return this
    }

    fun createMonochrome(getValue: I3B): Texture3D {
        val w = width
        val h = height
        val d = depth
        val size = w * h * d
        val byteBuffer = byteBufferPool[size, false, false]
        for (z in 0 until d) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    byteBuffer.put(getValue.call(x, y, z))
                }
            }
        }
        byteBuffer.flip()
        createMonochrome(byteBuffer)
        byteBufferPool.returnBuffer(byteBuffer)
        return this
    }

    fun createRGBA8(getValue: I3I): Texture3D {
        val w = width
        val h = height
        val d = depth
        val size = 4 * w * h * d
        val byteBuffer = byteBufferPool[size, false, false]
        for (z in 0 until d) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    byteBuffer.putInt(getValue.call(x, y, z))
                }
            }
        }
        byteBuffer.flip()
        createBGRA8(byteBuffer)
        byteBufferPool.returnBuffer(byteBuffer)
        return this
    }

    fun createMonochrome(data: ByteBuffer): Texture3D {
        assertEquals(width * height * depth, data.remaining(), "incorrect size!")
        beforeUpload(width)
        glTexImage3D(target, 0, GL_R8, width, height, depth, 0, GL_RED, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_R8, 1, false)
        return this
    }

    fun create(type: TargetType, data: ByteArray? = null): Texture3D {
        if (data != null) {
            var bpp = type.bytesPerPixel
            if (bpp == 3) bpp = 4 // todo check correctness
            assertEquals(width * height * depth * bpp, data.size, "incorrect size")
        }
        val byteBuffer = if (data != null) {
            val byteBuffer = byteBufferPool[data.size, false, false]
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
        byteBufferPool.returnBuffer(byteBuffer)
        afterUpload(type.internalFormat, type.bytesPerPixel, type.isHDR)
        return this
    }

    fun createRGBA(data: FloatArray): Texture3D {
        assertEquals(width * height * depth * 4, data.size, "expected 4 bpp")
        val byteBuffer = byteBufferPool[data.size * 4, false, false]
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.position(0)
        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(data)
        floatBuffer.flip()
        return createRGBA(floatBuffer, byteBuffer)
    }

    fun createRGBA(floatBuffer: FloatBuffer, byteBuffer: ByteBuffer): Texture3D {
        // rgba32f as internal format is extremely important... otherwise the value is cropped
        assertEquals(width * height * depth * 4, floatBuffer.remaining(), "incorrect size!")
        beforeUpload(width * 16)
        glTexImage3D(target, 0, GL_RGBA32F, width, height, depth, 0, GL_RGBA, GL_FLOAT, floatBuffer)
        byteBufferPool.returnBuffer(byteBuffer)
        afterUpload(GL_RGBA32F, 16, true)
        return this
    }

    fun createRGBA(data: ByteArray): Texture3D {
        assertEquals(width * height * depth * 4, data.size, "incorrect size!, expected 4 bpp")
        val byteBuffer = byteBufferPool[data.size, false, false]
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.flip()
        beforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, depth, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer)
        byteBufferPool.returnBuffer(byteBuffer)
        afterUpload(GL_RGBA8, 4, false)
        return this
    }

    fun createRGBA(data: ByteBuffer): Texture3D {
        assertEquals(width * height * depth * 4, data.remaining(), "incorrect size!, expected 4 bpp")
        beforeUpload(width * 4)
        glTexImage3D(target, 0, GL_RGBA8, width, height, depth, 0, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(GL_RGBA8, 4, false)
        return this
    }

    fun ensureFiltering(nearest: Filtering, clamping: Clamping): Texture3D {
        if (nearest != filtering) filtering(nearest)
        if (clamping != this.clamping) clamping(clamping)
        return this
    }

    fun filtering(filtering: Filtering): Texture3D {
        val glFilter = if (filtering == Filtering.NEAREST || filtering == Filtering.TRULY_NEAREST)
            GL_NEAREST else GL_LINEAR
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, glFilter)
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, glFilter)
        this.filtering = filtering
        return this
    }

    fun clamping(clamping: Clamping): Texture3D {
        TextureHelper.clamping(target, clamping.mode, border)
        this.clamping = clamping
        return this
    }

    private fun forceBind() {
        if (!isPointerValid(pointer)) throw RuntimeException()
        bindTexture(target, pointer)
    }

    override fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean {
        activeSlot(index)
        if (isPointerValid(pointer) && wasCreated) {
            bindTexture(target, pointer)
            ensureFiltering(filtering, clamping)
        } else {
            invisibleTex3d.bind(index, Filtering.LINEAR, Clamping.CLAMP)
        }
        return true
    }

    override fun destroy() {
        val pointer = pointer
        if (isPointerValid(pointer)) destroy(pointer)
        this.pointer = INVALID_POINTER
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

    override fun createImage(flipY: Boolean, withAlpha: Boolean, level: Int) =
        VRAMToRAM.createImage(width * depth, height, VRAMToRAM.zero, flipY, withAlpha) { x2, y2, w2, _ ->
            drawSlice(x2, y2, w2, !withAlpha)
        }

    private fun drawSlice(x2: Int, y2: Int, w2: Int, ignoreAlpha: Boolean) {
        val z0 = x2 / width
        val z1 = (x2 + w2 - 1) / width
        drawSlice(x2, y2, z0 / maxOf(1f, depth - 1f), ignoreAlpha)
        if (z1 > z0) {
            // todo we have to draw two slices
            // drawSlice(x2, y2, z0 / maxOf(1f, depth - 1f), withAlpha)
        }
    }

    private fun drawSlice(x2: Int, y2: Int, z: Float, ignoreAlpha: Boolean) {
        val x = -x2
        val y = -y2
        GFX.check()
        // we could use an easier shader here
        val shader = FlatShaders.flatShaderTexture3D.value
        shader.use()
        GFXx2D.posSize(shader, x, GFX.viewportHeight - y, width, -height)
        shader.v4f("color", -1)
        shader.v1i("alphaMode", ignoreAlpha.toInt())
        shader.v1b("applyToneMapping", isHDR)
        shader.v1f("layer", z)
        GFXx2D.noTiling(shader)
        bind(0, filtering, Clamping.CLAMP)
        SimpleBuffer.flat01.draw(shader)
        GFX.check()
    }

    override fun toString() =
        "Texture3D(\"$name\"@$pointer, $width x $height x $height x $samples, ${GLNames.getName(internalFormat)})"

    companion object {
        private val LOGGER = LogManager.getLogger(Texture3D::class)
        val allocated = AtomicLong()
        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated.addAndGet(newValue - oldValue)
            return newValue
        }
    }
}