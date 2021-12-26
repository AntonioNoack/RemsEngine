package me.anno.gpu.texture

import me.anno.cache.data.ICacheData
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.isGFXThread
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.OpenGL
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.buffer.Buffer.Companion.bindBuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.image.Image
import me.anno.objects.modes.RotateJPEG
import me.anno.utils.hpc.Threads.threadWithName
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.pooling.ByteArrayPool
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.pooling.IntArrayPool
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.GL_TEXTURE_2D_MULTISAMPLE
import org.lwjgl.opengl.GL32.glTexImage2DMultisample
import org.lwjgl.opengl.GL43
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

open class Texture2D(
    val name: String,
    override var w: Int,
    override var h: Int,
    samples: Int
) : ICacheData, ITexture2D {

    constructor(img: BufferedImage, checkRedundancy: Boolean) : this("img", img.width, img.height, 1) {
        create(img, true, checkRedundancy)
        filtering(GPUFiltering.NEAREST)
    }

    val samples = clamp(samples, 1, GFX.maxSamples)

    override fun toString() = "$name $w $h $samples"

    private val withMultisampling = samples > 1

    private val tex2D = if (withMultisampling) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D
    val state get(): Int = pointer * 4 + isDestroyed.toInt(2) + isCreated.toInt(1)

    var pointer = -1
    var session = 0

    fun checkSession() {
        if (session != OpenGL.session) {
            session = OpenGL.session
            pointer = -1
            isCreated = false
            isDestroyed = false
            locallyAllocated = allocate(locallyAllocated, 0L)
        }
    }

    var isCreated = false
    var isDestroyed = false

    var filtering = GPUFiltering.TRULY_NEAREST
    var clamping: Clamping? = null

    // only used for images with exif rotation tag...
    var rotation: RotateJPEG? = null

    var locallyAllocated = 0L

    var createdW = 0
    var createdH = 0

    var isHDR = false

    fun setSize(width: Int, height: Int) {
        w = width
        h = height
    }

    fun ensurePointer() {
        checkSession()
        if (isDestroyed) throw RuntimeException("Texture was destroyed")
        if (pointer <= 0) {
            GFX.check()
            pointer = createTexture()
            DebugGPUStorage.tex2d.add(this)
            // many textures can be created by the console log and the fps viewer constantly xD
            // maybe we should use allocation free versions there xD
            GFX.check()
        }
        if (pointer <= 0) throw RuntimeException("Could not allocate texture pointer")
    }

    fun texImage2D(type0: Int, type1: Int, fillType: Int, data: ByteBuffer?) {
        val w = w
        val h = h
        unpackAlignment(w)
        if (createdW == w && createdH == h) {
            if (data == null) return
            glTexSubImage2D(tex2D, 0, 0, 0, w, h, type1, fillType, data)
        } else {
            glTexImage2D(tex2D, 0, type0, w, h, 0, type1, fillType, data)
            createdW = w
            createdH = h
        }
    }

    fun texImage2D(type: TargetType, data: ByteBuffer?) {
        val w = w
        val h = h
        bindBuffer(GL_PIXEL_UNPACK_BUFFER, 0)
        unpackAlignment(w)
        if (createdW == w && createdH == h) {
            if (data == null) return
            glTexSubImage2D(tex2D, 0, 0, 0, w, h, type.type1, type.fillType, data)
        } else {
            glTexImage2D(tex2D, 0, type.type0, w, h, 0, type.type1, type.fillType, data)
            createdW = w
            createdH = h
        }
    }

    fun create() {
        beforeUpload(0, 0)
        if (withMultisampling) {
            glTexImage2DMultisample(tex2D, samples, GL_RGBA8, w, h, false)
        } else texImage2D(TargetType.UByteTarget4, null)
        afterUpload(false, 4 * samples)
    }

    fun create(type: TargetType) {
        beforeUpload(0, 0)
        if (withMultisampling) {
            glTexImage2DMultisample(tex2D, samples, type.type0, w, h, false)
        } else texImage2D(type, null)
        afterUpload(type.isHDR, type.bytesPerPixel)
    }

    fun createFP32() {
        beforeUpload(0, 0)
        if (withMultisampling) {
            glTexImage2DMultisample(tex2D, samples, GL_RGBA32F, w, h, false)
        } else texImage2D(TargetType.FloatTarget4, null)
        afterUpload(true, 4 * 4 * samples)
    }

    /**
     * force sync should be always enabled, when the image is directly available
     * */
    fun create2(name: String, createImage: () -> BufferedImage, forceSync: Boolean, checkRedundancy: Boolean) {
        if (isDestroyed) throw RuntimeException("Texture $name must be reset first")
        val requiredBudget = textureBudgetUsed + w * h
        if ((requiredBudget > textureBudgetTotal && !loadTexturesSync.peek()) || !isGFXThread()) {
            if (forceSync) {
                GFX.addGPUTask(1000) {
                    if (!isDestroyed) {
                        create(createImage(), true, checkRedundancy)
                    }
                }
            } else {
                threadWithName("Create Image") {
                    create(createImage(), false, checkRedundancy)
                }
            }
        } else {
            textureBudgetUsed += requiredBudget
            create(createImage(), true, checkRedundancy)
        }
    }

    fun create(name: String, image: Image, checkRedundancy: Boolean) {
        w = image.width
        h = image.height
        if (isDestroyed) throw RuntimeException("Texture $name must be reset first")
        val requiredBudget = textureBudgetUsed + w * h
        if ((requiredBudget > textureBudgetTotal && !loadTexturesSync.peek()) || !isGFXThread()) {
            GFX.addGPUTask(1000) {
                image.createTexture(this, checkRedundancy)
            }
        } else {
            textureBudgetUsed += requiredBudget
            image.createTexture(this, checkRedundancy)
        }
    }

    fun setSize1x1() {
        w = 1
        h = 1
    }

    fun create(image: Image, sync: Boolean, checkRedundancy: Boolean) {
        if (sync && isGFXThread()) {
            image.createTexture(this, checkRedundancy)
        } else GFX.addGPUTask(w, h) {
            image.createTexture(this, checkRedundancy)
        }
    }

    fun create(image: BufferedImage, sync: Boolean, checkRedundancy: Boolean) {

        w = image.width
        h = image.height
        isCreated = false

        // todo use the type to correctly create the image
        val buffer = image.data.dataBuffer
        when (image.type) {
            BufferedImage.TYPE_INT_ARGB -> {
                buffer as DataBufferInt
                val data = buffer.data
                if (sync && isGFXThread()) {
                    createRGBA(data, checkRedundancy)
                } else GFX.addGPUTask(w, h) {
                    createRGBA(data, checkRedundancy)
                }
            }
            BufferedImage.TYPE_INT_RGB -> {
                buffer as DataBufferInt
                val data = buffer.data
                if (sync && isGFXThread()) {
                    createRGBSwizzle(data, checkRedundancy)
                } else GFX.addGPUTask(w, h) {
                    createRGBSwizzle(data, checkRedundancy)
                }
            }
            BufferedImage.TYPE_INT_BGR -> {
                buffer as DataBufferInt
                val data = buffer.data
                for (i in data.indices) {
                    val c = data[i]
                    data[i] = c.shr(16) or
                            c.shl(16) or
                            c.and(0x00ff00)
                }
                if (sync && isGFXThread()) {
                    createRGBSwizzle(data, checkRedundancy)
                } else GFX.addGPUTask(w, h) {
                    createRGBSwizzle(data, checkRedundancy)
                }
            }
            else -> createRGBA(image, sync, checkRedundancy)
        }
    }

    private fun createRGBA(img: BufferedImage, sync: Boolean, checkRedundancy: Boolean) {
        val intData = img.getRGB(0, 0, w, h, intArrayPool[w * h, false], 0, w)
        val hasAlpha = img.hasAlphaChannel()
        if (!hasAlpha) {
            // ensure opacity
            if (sync && isGFXThread()) {
                createRGBSwizzle(intData, checkRedundancy)
                intArrayPool.returnBuffer(intData)
            } else GFX.addGPUTask(w, h) {
                createRGBSwizzle(intData, checkRedundancy)
                intArrayPool.returnBuffer(intData)
            }
        } else {
            if (sync && isGFXThread()) {
                createRGBA(intData, checkRedundancy)
                intArrayPool.returnBuffer(intData)
            } else GFX.addGPUTask(w, h) {
                createRGBA(intData, checkRedundancy)
                intArrayPool.returnBuffer(intData)
            }
        }
    }

    fun getBuffer(bytes: ByteArray, ensureOpaque: Boolean): ByteBuffer {
        val buffer = bufferPool[w * h * 4, false]
        val wh = w * h
        when (bytes.size) {
            wh -> {
                for (i in 0 until wh) {
                    val c = bytes[i]
                    buffer.put(c)
                    buffer.put(c)
                    buffer.put(c)
                    buffer.put(-1)
                }
            }
            wh * 3 -> {
                for (i in 0 until wh) {
                    buffer.put(bytes[i * 3 + 2])
                    buffer.put(bytes[i * 3 + 1])
                    buffer.put(bytes[i * 3])
                    buffer.put(-1)
                }
            }
            wh * 4 -> {
                if (ensureOpaque) {
                    for (i in 0 until wh) {
                        buffer.put(bytes[i * 4 + 3])
                        buffer.put(bytes[i * 4 + 2])
                        buffer.put(bytes[i * 4 + 1])
                        buffer.put(-1)
                    }
                } else {
                    for (i in 0 until wh) {
                        buffer.put(bytes[i * 4 + 3])
                        buffer.put(bytes[i * 4 + 2])
                        buffer.put(bytes[i * 4 + 1])
                        buffer.put(bytes[i * 4])
                    }
                }
            }
            else -> throw RuntimeException("Not matching sizes! $wh vs ${bytes.size}")
        }
        buffer.position(0)
        return buffer
    }

    fun createRGBA(buffer: ByteBuffer) {
        beforeUpload(4, buffer.remaining())
        val t0 = System.nanoTime()
        texImage2D(TargetType.UByteTarget4, buffer)
        // glTexImage2D(tex2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        val t1 = System.nanoTime() // 0.02s for a single 4k texture
        afterUpload(false, 4)
        val t2 = System.nanoTime() // 1e-6
        if (w * h > 1e4 && (t2 - t0) * 1e-9f > 0.01f) LOGGER.info("Used ${(t1 - t0) * 1e-9f}s + ${(t2 - t1) * 1e-9f}s to upload ${(w * h) / 1e6f} MPixel image to GPU")
    }

    /*fun uploadData2(data: ByteBuffer, callback: () -> Unit) {

        val pbo = GL15.glGenBuffers()
        val type = GL21.GL_PIXEL_UNPACK_BUFFER

        bindBuffer(type, pbo)
        GL15.glBufferData(type, w * h * 4L, GL_STREAM_DRAW)
        val mappedBuffer = GL15.glMapBuffer(type, GL15.GL_WRITE_ONLY)!!

        //threadWithName("Texture2D::uploadData2") {

        val startPosition = mappedBuffer.position()
        mappedBuffer.put(data)
        mappedBuffer.position(startPosition)

        //GFX.addGPUTask(1) {
        bindBuffer(type, pbo)
        if (!GL15.glUnmapBuffer(type)) {
            LOGGER.warn("Unmap failed!")
        }
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0)
        GFX.check()
        callback()
        //}
        //}
        GFX.check()

    }*/

    private fun beforeUpload(channels: Int, size: Int) {
        if (isDestroyed) throw RuntimeException("Texture is already destroyed, call reset() if you want to stream it")
        checkSize(channels, size)
        GFX.check()
        ensurePointer()
        bindBeforeUpload()
    }

    private fun beforeUpload() {
        if (isDestroyed) throw RuntimeException("Texture is already destroyed, call reset() if you want to stream it")
        GFX.check()
        ensurePointer()
        bindBeforeUpload()
    }

    private fun afterUpload(isHDR: Boolean, bytesPerPixel: Int) {
        locallyAllocated = allocate(locallyAllocated, w * h * bytesPerPixel.toLong())
        isCreated = true
        this.isHDR = isHDR
        filtering(filtering)
        clamping(clamping ?: Clamping.REPEAT)
        GFX.check()
        if (isDestroyed) destroy()
    }

    private fun checkRedundancy(data: IntArray): IntArray {
        if (w * h <= 1) return data
        val c0 = data[0]
        for (i in 1 until w * h) {
            if (c0 != data[i]) return data
        }
        setSize1x1()
        return intArrayOf(c0)
    }

    private fun checkRedundancy(data: IntBuffer) {
        if (w * h <= 1) return
        val c0 = data[0]
        for (i in 1 until w * h) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    private fun checkRedundancyMonochrome(data: ByteBuffer) {
        if (w * h <= 1) return
        val c0 = data[0]
        for (i in 1 until w * h) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    private fun checkRedundancyMonochrome(data: FloatBuffer) {
        val c0 = data[0]
        for (i in 1 until w * h) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    private fun checkRedundancyMonochrome(data: ByteArray): ByteArray {
        val c0 = data[0]
        for (i in 1 until w * h) {
            if (c0 != data[i]) return data
        }
        setSize1x1()
        return byteArrayOf(c0)
    }

    private fun checkRedundancy(data: FloatArray): FloatArray {
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        for (i in 4 until w * h * 4 step 4) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return data
        }
        setSize1x1()
        return floatArrayOf(c0, c1, c2, c3)
    }

    private fun checkRedundancy(data: FloatBuffer) {
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        for (i in 4 until w * h * 4 step 4) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return
        }
        setSize1x1()
        data.limit(4)
    }

    private fun checkRedundancy(data: ByteArray): ByteArray {
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        for (i in 4 until w * h * 4 step 4) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return data
        }
        setSize1x1()
        return byteArrayOf(c0, c1, c2, c3)
    }

    private fun checkRedundancyRG(data: ByteArray): ByteArray {
        val c0 = data[0]
        val c1 = data[1]
        for (i in 2 until w * h * 2 step 2) {
            if (c0 != data[i] || c1 != data[i + 1]) return data
        }
        setSize1x1()
        return byteArrayOf(c0, c1)
    }

    private fun checkRedundancy(data: ByteBuffer, rgbOnly: Boolean) {
        // when rgbOnly, check rgb only?
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        for (i in 4 until w * h * 4 step 4) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return
        }
        setSize1x1()
        data.limit(4)
    }

    private fun checkRedundancyRG(data: ByteBuffer) {
        // when rgbOnly, check rgb only?
        val c0 = data[0]
        val c1 = data[1]
        for (i in 2 until w * h * 2 step 2) {
            if (c0 != data[i] || c1 != data[i + 1]) return
        }
        setSize1x1()
        data.limit(2)
    }

    fun createRGBA(ints: IntArray, checkRedundancy: Boolean) {
        beforeUpload(1, ints.size)
        val ints2 = if (checkRedundancy) checkRedundancy(ints) else ints
        switchRGB2BGR(ints2)
        unpackAlignment(4 * w)
        // uses bgra instead of rgba to save the swizzle
        // glTexImage2D(tex2D, 0, GL_RGBA8, w, h, 0, GL_BGRA, GL_UNSIGNED_BYTE, ints2)
        glTexImage2D(tex2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, ints2)
        afterUpload(false, 4)
    }

    /**
     * Warning:
     * changes the red and blue bytes; if that is not ok, create a copy of your array!
     * */
    fun createRGBSwizzle(ints: IntArray, checkRedundancy: Boolean) {
        beforeUpload(1, ints.size)
        val ints2 = if (checkRedundancy) checkRedundancy(ints) else ints
        switchRGB2BGR(ints2)
        // would work without swizzle, but I am not sure, that this is legal,
        // because the number of channels from the input and internal format differ
        // glTexImage2D(tex2D, 0, GL_RGB8, w, h, 0, GL_BGRA, GL_UNSIGNED_BYTE, ints2)
        unpackAlignment(4 * w)
        glTexImage2D(tex2D, 0, GL_RGB8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, ints2)
        afterUpload(false, 3)
    }

    fun createRGB(floats: FloatArray, checkRedundancy: Boolean) {
        beforeUpload(3, floats.size)
        val floats2 = if (checkRedundancy) checkRedundancy(floats) else floats
        unpackAlignment(12 * w)
        glTexImage2D(tex2D, 0, GL_RGB32F, w, h, 0, GL_RGB, GL_FLOAT, floats2)
        afterUpload(true, 12)
    }

    fun createRGB(floats: FloatBuffer, checkRedundancy: Boolean) {
        beforeUpload(3, floats.capacity())
        if (checkRedundancy) checkRedundancy(floats)
        unpackAlignment(12 * w)
        glTexImage2D(tex2D, 0, GL_RGB32F, w, h, 0, GL_RGB, GL_FLOAT, floats)
        afterUpload(true, 12)
    }

    fun createRGB(data: ByteArray, checkRedundancy: Boolean) {
        beforeUpload(3, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        val buffer = bufferPool[data2.size, false]
        buffer.put(data2).flip()
        unpackAlignment(3 * w)
        glTexImage2D(tex2D, 0, GL_RGB8, w, h, 0, GL_RGB, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 3)
    }

    fun createRGBA(ints: IntBuffer, checkRedundancy: Boolean) {
        beforeUpload(1, ints.remaining())
        if (checkRedundancy) checkRedundancy(ints)
        if (ints.order() != ByteOrder.nativeOrder()) throw RuntimeException("Byte order must be native!")
        unpackAlignment(4 * w)
        glTexImage2D(tex2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, ints)
        afterUpload(false, 4)
    }

    fun createRGB(ints: IntBuffer, checkRedundancy: Boolean) {
        beforeUpload(1, ints.remaining())
        if (checkRedundancy) checkRedundancy(ints)
        if (ints.order() != ByteOrder.nativeOrder()) throw RuntimeException("Byte order must be native!")
        unpackAlignment(4 * w)
        glTexImage2D(tex2D, 0, GL_RGB8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, ints)
        afterUpload(false, 4)
    }

    fun createMonochrome(data: ByteBuffer, checkRedundancy: Boolean) {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancyMonochrome(data)
        texImage2D(TargetType.UByteTarget1, data)
        bufferPool.returnBuffer(data)
        afterUpload(false, 1)
    }

    fun createRG(data: ByteArray, checkRedundancy: Boolean) {
        beforeUpload(2, data.size)
        val data2 = if (checkRedundancy) checkRedundancyRG(data) else data
        val buffer = bufferPool[data.size, false]
        buffer.put(data2).flip()
        texImage2D(GL_RG, GL_RG, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 2)
    }

    fun createRG(data: ByteBuffer, checkRedundancy: Boolean) {
        beforeUpload(2, data.remaining())
        if (checkRedundancy) checkRedundancyRG(data)
        texImage2D(GL_RG, GL_RG, GL_UNSIGNED_BYTE, data)
        bufferPool.returnBuffer(data)
        afterUpload(false, 2)
    }

    /**
     * creates a monochrome float32 image on the GPU
     * used by SDF
     * */
    fun createMonochrome(data: FloatBuffer, checkRedundancy: Boolean) {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancyMonochrome(data)
        unpackAlignment(4 * w)
        glTexImage2D(tex2D, 0, GL_R32F, w, h, 0, GL_RED, GL_FLOAT, data)
        afterUpload(true, 4)
    }

    fun createBGR(data: ByteArray, checkRedundancy: Boolean) {
        beforeUpload(3, data.size)
        val data2 = if (checkRedundancy) checkRedundancyMonochrome(data) else data
        val byteBuffer = bufferPool[data2.size, false]
        byteBuffer.put(data2).flip()
        texImage2D(GL_RGBA8, GL_BGR, GL_UNSIGNED_BYTE, byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(false, 3)
    }

    fun createBGR(data: ByteBuffer, checkRedundancy: Boolean) {
        beforeUpload(3, data.remaining())
        if (checkRedundancy) checkRedundancyMonochrome(data)
        texImage2D(GL_RGBA8, GL_BGR, GL_UNSIGNED_BYTE, data)
        bufferPool.returnBuffer(data)
        afterUpload(false, 3)
    }

    fun createMonochrome(data: ByteArray, checkRedundancy: Boolean) {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancyMonochrome(data) else data
        val byteBuffer = bufferPool[data2.size, false]
        byteBuffer.put(data2).flip()
        texImage2D(TargetType.UByteTarget1, byteBuffer)
        // glTexImage2D(tex2D, 0, GL_RED, w, h, 0, GL_RED, GL_UNSIGNED_BYTE, byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(false, 1)
    }

    fun createRGBA(data: FloatArray, checkRedundancy: Boolean) {

        beforeUpload(4, data.size)
        val data2 = if (checkRedundancy && w * h > 1) checkRedundancy(data) else data

        val byteBuffer = bufferPool[data2.size * 4, false]

        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(data2).flip()

        // rgba32f as internal format is extremely important... otherwise the value is cropped
        texImage2D(TargetType.FloatTarget4, byteBuffer)
        // glTexImage2D(tex2D, 0, GL_RGBA32F, w, h, 0, GL_RGBA, GL_FLOAT, buffer)
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(true, 16)

    }

    fun createBGRA(data: ByteArray, checkRedundancy: Boolean) {
        checkSize(4, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        val buffer = bufferPool[data2.size, false]
        buffer.put(data2).flip()
        beforeUpload(4, buffer.remaining())
        if (checkRedundancy) checkRedundancy(buffer, false)
        texImage2D(GL_RGBA, GL_BGRA, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 4)
    }

    fun createRGBA(data: ByteArray, checkRedundancy: Boolean) {
        checkSize(4, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        val buffer = bufferPool[data2.size, false]
        buffer.put(data2).flip()
        createRGBA(buffer, false)
    }

    fun createRGBA(buffer: ByteBuffer, checkRedundancy: Boolean) {
        beforeUpload(4, buffer.remaining())
        if (checkRedundancy) checkRedundancy(buffer, false)
        texImage2D(TargetType.UByteTarget4, buffer)
        // glTexImage2D(tex2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 4)
    }

    fun createRGB(buffer: ByteBuffer, checkRedundancy: Boolean) {
        beforeUpload(3, buffer.remaining())
        if (checkRedundancy) checkRedundancy(buffer, true)
        // texImage2D(TargetType.UByteTarget3, buffer)
        unpackAlignment(3 * w)
        glTexImage2D(tex2D, 0, GL_RGB, w, h, 0, GL_RGB, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 3)
    }

    fun createDepth(lowQuality: Boolean = false) {
        beforeUpload()
        val format = if (lowQuality) GL_DEPTH_COMPONENT16 else GL_DEPTH_COMPONENT32F
        if (withMultisampling) {
            glTexImage2DMultisample(tex2D, samples, format, w, h, false)
        } else {
            glTexImage2D(tex2D, 0, format, w, h, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0)
        }
        afterUpload(false, if (lowQuality) 2 else 4)
    }

    /**
     * texture must be bound!
     * */
    fun ensureFilterAndClamping(nearest: GPUFiltering, clamping: Clamping) {
        // ensure being bound?
        if (nearest != this.filtering) filtering(nearest)
        if (clamping != this.clamping) clamping(clamping)
    }

    private fun clamping(clamping: Clamping) {
        if (!withMultisampling && this.clamping != clamping) {
            this.clamping = clamping
            val type = clamping.mode
            glTexParameteri(tex2D, GL_TEXTURE_WRAP_S, type)
            glTexParameteri(tex2D, GL_TEXTURE_WRAP_T, type)
        }
    }

    var autoUpdateMipmaps = true

    private fun filtering(nearest: GPUFiltering) {
        if (withMultisampling) {
            this.filtering = GPUFiltering.TRULY_NEAREST
            // multisample textures only support nearest filtering;
            // they don't accept the command to be what they are either
            return
        }
        if (!hasMipmap && nearest.needsMipmap && (w > 1 || h > 1)) {
            glGenerateMipmap(tex2D)
            hasMipmap = true
            if (GFX.supportsAnisotropicFiltering) {
                val anisotropy = GFX.anisotropy
                glTexParameteri(tex2D, GL_TEXTURE_LOD_BIAS, 0)
                glTexParameterf(tex2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy)
            }
            // whenever the base mipmap is changed, the mipmaps will be updated :)
            glTexParameteri(tex2D, GL_GENERATE_MIPMAP, if (autoUpdateMipmaps) GL_TRUE else GL_FALSE)
            // is called afterwards anyways
            // glTexParameteri(tex2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        }
        glTexParameteri(tex2D, GL_TEXTURE_MIN_FILTER, nearest.min)
        glTexParameteri(tex2D, GL_TEXTURE_MAG_FILTER, nearest.mag)
        this.filtering = nearest
    }

    var hasMipmap = false

    private fun bindBeforeUpload() {
        if (pointer <= -1) throw RuntimeException("Pointer must be defined")
        bindTexture(tex2D, pointer)
    }

    /*override fun bind(nearest: GPUFiltering, clamping: Clamping): Boolean {
        if (pointer > -1 && isCreated) {
            val result = bindTexture(tex2D, pointer)
            ensureFilterAndClamping(nearest, clamping)
            return result
        } else throw IllegalStateException("Cannot bind non-created texture!")
    }*/

    override fun bind(index: Int, nearest: GPUFiltering, clamping: Clamping): Boolean {
        if (pointer > 0 && isCreated) {
            if (isBoundToSlot(index)) return false
            activeSlot(index)
            val result = bindTexture(tex2D, pointer)
            ensureFilterAndClamping(nearest, clamping)
            return result
        } else throw IllegalStateException("Cannot bind non-created texture!")
    }

    fun bind(index: Int) = bind(index, filtering, clamping ?: Clamping.REPEAT)

    override fun destroy() {
        isCreated = false
        isDestroyed = true
        val pointer = pointer
        if (pointer > -1) {
            if (!isGFXThread()) {
                GFX.addGPUTask(1) {
                    DebugGPUStorage.tex2d.remove(this)
                    invalidateBinding()
                    locallyAllocated = allocate(locallyAllocated, 0L)
                    texturesToDelete.add(pointer)
                }
            } else {
                DebugGPUStorage.tex2d.remove(this)
                invalidateBinding()
                locallyAllocated = allocate(locallyAllocated, 0L)
                texturesToDelete.add(pointer)
            }
        }
        this.pointer = -1
    }

    private fun checkSize(channels: Int, size: Int) {
        if (size < w * h * channels) throw IllegalArgumentException("Incorrect size, $w*$h*$channels vs ${size}!")
        if (size > w * h * channels) LOGGER.warn("$size != $w*$h*$channels")
    }

    fun reset() {
        isDestroyed = false
    }

    fun isBoundToSlot(slot: Int): Boolean {
        return boundTextures[slot] == pointer
    }

    companion object {

        var wasModifiedInComputePipeline = false

        fun BufferedImage.hasAlphaChannel() = colorModel.hasAlpha()

        val bufferPool = ByteBufferPool(64, false)
        val byteArrayPool = ByteArrayPool(64, true)
        val intArrayPool = IntArrayPool(64, true)

        var allocated = 0L
        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated += newValue - oldValue
            return newValue
        }

        var boundTextureSlot = 0
        val boundTextures = IntArray(64) { -1 }

        fun invalidateBinding() {
            boundTextureSlot = -1
            activeSlot(0)
            for (i in boundTextures.indices) {
                boundTextures[i] = -1
            }
        }

        fun activeSlot(index: Int) {
            if (index != boundTextureSlot) {
                glActiveTexture(GL_TEXTURE0 + index)
                boundTextureSlot = index
            }
        }

        /**
         * bind the texture, the slot doesn't matter
         * */
        fun bindTexture(mode: Int, pointer: Int): Boolean {
            if (pointer < 0) throw IllegalArgumentException("Pointer must be valid")
            if (wasModifiedInComputePipeline) {
                GL43.glMemoryBarrier(GL43.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
                wasModifiedInComputePipeline = false
            }
            return if (boundTextures[boundTextureSlot] != pointer) {
                boundTextures[boundTextureSlot] = pointer
                glBindTexture(mode, pointer)
                true
            } else false
        }

        private val LOGGER = LogManager.getLogger(Texture2D::class)
        val textureBudgetTotal = DefaultConfig["gpu.textureBudget", 1_000_000L]
        var textureBudgetUsed = 0L
        val texturesToDelete = ArrayList<Int>()

        fun resetBudget() {
            textureBudgetUsed = 0L
        }

        // val isLittleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN

        private var creationSession = -1
        private var creationIndex = 0
        private val creationIndices = IntArray(16)

        fun createTexture(): Int {
            GFX.checkIsGFXThread()
            if (creationSession != OpenGL.session || creationIndex == creationIndices.size) {
                creationIndex = 0
                creationSession = OpenGL.session
                glGenTextures(creationIndices)
                GFX.check()
            }
            return creationIndices[creationIndex++]
        }

        fun switchRGB2BGR(ints: IntArray) {
            // convert argb to abgr
            for (i in ints.indices) {
                val v = ints[i]
                val r = v.shr(16).and(0xff)
                val ag = v.and(0xff00ff00.toInt())
                val b = v.and(0xff)
                ints[i] = ag or r or b.shl(16)
            }
        }

        fun destroyTextures() {
            if (texturesToDelete.isNotEmpty()) {
                glDeleteTextures(texturesToDelete.toIntArray())
                texturesToDelete.clear()
            }
        }

        private fun getAlignment(w: Int): Int {
            return when {
                w % 8 == 0 -> 8
                w % 4 == 0 -> 4
                w % 2 == 0 -> 2
                else -> 1
            }
        }

        fun packAlignment(w: Int) {
            glPixelStorei(GL_PACK_ALIGNMENT, getAlignment(w))
        }

        fun unpackAlignment(w: Int) {
            glPixelStorei(GL_UNPACK_ALIGNMENT, getAlignment(w))
        }

    }

}