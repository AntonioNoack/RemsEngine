package me.anno.gpu.texture

import me.anno.cache.data.ICacheData
import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.Docs
import me.anno.gpu.GFX
import me.anno.gpu.GFX.check
import me.anno.gpu.GFX.isGFXThread
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.buffer.OpenGLBuffer.Companion.bindBuffer
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.image.Image
import me.anno.image.RotateJPEG
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min
import me.anno.utils.hpc.WorkSplitter
import me.anno.utils.pooling.ByteArrayPool
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.pooling.FloatArrayPool
import me.anno.utils.pooling.IntArrayPool
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL11.GL_R
import org.lwjgl.opengl.GL14.GL_GENERATE_MIPMAP
import org.lwjgl.opengl.GL45C.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.*
import kotlin.concurrent.thread

@Suppress("unused")
open class Texture2D(
    val name: String,
    override var w: Int,
    override var h: Int,
    samples: Int
) : ICacheData, ITexture2D {

    constructor(name: String, img: BufferedImage, checkRedundancy: Boolean) : this(name, img.width, img.height, 1) {
        create(img, true, checkRedundancy)
        filtering(GPUFiltering.NEAREST)
    }

    constructor(img: BufferedImage, checkRedundancy: Boolean) : this("img", img.width, img.height, 1) {
        create(img, true, checkRedundancy)
        filtering(GPUFiltering.NEAREST)
    }

    constructor(name: String, img: Image, checkRedundancy: Boolean) : this(name, img.width, img.height, 1) {
        create(img, true, checkRedundancy)
        filtering(GPUFiltering.NEAREST)
    }

    constructor(img: Image, checkRedundancy: Boolean) : this("img", img.width, img.height, 1) {
        create(img, true, checkRedundancy)
        filtering(GPUFiltering.NEAREST)
    }

    val samples = clamp(samples, 1, GFX.maxSamples)

    var internalFormat = 0

    override fun toString() = "Tex2D(\"$name\", $w $h $samples)"

    private val withMultisampling = samples > 1

    val target = if (withMultisampling) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D
    val state get(): Int = pointer * 4 + isDestroyed.toInt(2) + isCreated.toInt(1)

    var pointer = -1
    var session = 0

    fun checkSession() {
        if (session != GFXState.session) {
            session = GFXState.session
            pointer = -1
            isCreated = false
            isDestroyed = false
            locallyAllocated = allocate(locallyAllocated, 0L)
            createdW = 0
            createdH = 0
        }
    }

    fun resize(w: Int, h: Int, type: TargetType) {
        if (w != this.w || h != this.h) {
            this.w = w
            this.h = h
            destroy() // needed?
            reset()
            create(type)
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

    override var isHDR = false

    fun setSize(width: Int, height: Int) {
        w = width
        h = height
    }

    fun swizzleMonochrome() {
        swizzle(GL_RED, GL_RED, GL_RED, GL_ONE)
    }

    // could and should be used for roughness/metallic like textures in the future
    fun swizzle(r: Int, g: Int, b: Int, a: Int) {
        tmp4i[0] = r
        tmp4i[1] = g
        tmp4i[2] = b
        tmp4i[3] = a
        check()
        glTexParameteriv(target, GL_TEXTURE_SWIZZLE_RGBA, tmp4i)
        check()
    }

    fun ensurePointer() {
        checkSession()
        if (isDestroyed) throw RuntimeException("Texture was destroyed")
        if (pointer <= 0) {
            check()
            pointer = createTexture()
            DebugGPUStorage.tex2d.add(this)
            // many textures can be created by the console log and the fps viewer constantly xD
            // maybe we should use allocation free versions there xD
            check()
        }
        if (pointer <= 0) throw RuntimeException("Could not allocate texture pointer")
    }

    private fun unbindUnpackBuffer() {
        bindBuffer(GL_PIXEL_UNPACK_BUFFER, 0)
    }

    fun setAlignmentAndBuffer(w: Int, dataFormat: Int, dataType: Int, unbind: Boolean) {
        val typeSize = when (dataType) {
            GL_UNSIGNED_BYTE, GL_BYTE -> 1
            GL_UNSIGNED_SHORT, GL_SHORT -> 2
            GL_UNSIGNED_INT, GL_INT, GL_FLOAT -> 4
            GL_DOUBLE -> 8
            else -> {
                LOGGER.warn("Unknown data type $dataType")
                RuntimeException().printStackTrace()
                1
            }
        }
        val numChannels = when (dataFormat) {
            GL_R, GL_RED -> 1
            GL_RG -> 2
            GL_RGB, GL_BGR -> 3
            GL_RGBA, GL_BGRA -> 4
            else -> {
                LOGGER.warn("Unknown data format $dataFormat")
                RuntimeException().printStackTrace()
                1
            }
        }
        writeAlignment(w * typeSize * numChannels)
        if (unbind) unbindUnpackBuffer()
    }

    fun texImage2D(internalFormat: Int, dataFormat: Int, dataType: Int, data: Any?, unbind: Boolean = true) {
        val w = w
        val h = h
        val target = target
        check()
        if (createdW == w && createdH == h && data != null && !withMultisampling) {
            setAlignmentAndBuffer(w, dataFormat, dataType, unbind)
            when (data) {
                is ByteBuffer -> glTexSubImage2D(target, 0, 0, 0, w, h, dataFormat, dataType, data)
                is ShortBuffer -> glTexSubImage2D(target, 0, 0, 0, w, h, dataFormat, dataType, data)
                is IntBuffer -> glTexSubImage2D(target, 0, 0, 0, w, h, dataFormat, dataType, data)
                is FloatBuffer -> glTexSubImage2D(target, 0, 0, 0, w, h, dataFormat, dataType, data)
                is DoubleBuffer -> glTexSubImage2D(target, 0, 0, 0, w, h, dataFormat, dataType, data)
                is IntArray -> glTexSubImage2D(target, 0, 0, 0, w, h, dataFormat, dataType, data)
                else -> throw IllegalArgumentException("${data::class.simpleName} is not supported")
            }
        } else {
            if (withMultisampling) {
                glTexImage2DMultisample(target, samples, internalFormat, w, h, false)
                check()
            } else {
                if (data != null) setAlignmentAndBuffer(w, dataFormat, dataType, unbind)
                when (data) {
                    is ByteArray -> throw IllegalArgumentException("ByteArray is not supported")
                    is ByteBuffer -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is ShortBuffer -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is IntBuffer -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is FloatBuffer -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is DoubleBuffer -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is IntArray -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is FloatArray -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    null -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, null as ByteBuffer?)
                    else -> throw IllegalArgumentException("${data::class.simpleName} is not supported")
                }
                check()
            }
            this.internalFormat = internalFormat
            when (internalFormat) {
                GL_R8, GL_R8I, GL_R8UI,
                GL_R16F, GL_R16I, GL_R16UI,
                GL_R32F, GL_R32I, GL_R32UI -> swizzleMonochrome()
            }
            createdW = w
            createdH = h
        }
    }

    fun texSubImage2D(
        level: Int,
        x: Int, y: Int, w: Int, h: Int,
        dataFormat: Int,
        dataType: Int,
        data: Any,
        unbind: Boolean = true
    ) {
        setAlignmentAndBuffer(w, dataFormat, dataType, unbind)
        when (data) {
            is ByteBuffer -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            is ShortBuffer -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            is IntBuffer -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            is FloatBuffer -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            is DoubleBuffer -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            is IntArray -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            else -> throw RuntimeException(data::class.simpleName)
        }
    }

    fun texImage2D(type: TargetType, data: ByteBuffer?) {
        texImage2D(type.internalFormat, type.uploadFormat, type.fillType, data)
    }

    fun createRGB() = create(TargetType.UByteTarget3)
    fun createRGBA() = create(TargetType.UByteTarget4)
    fun createFP32() = create(TargetType.FloatTarget4)

    fun create(type: TargetType) {
        beforeUpload(0, 0)
        texImage2D(type, null)
        afterUpload(type.isHDR, type.bytesPerPixel)
    }

    fun create(type: TargetType, data: ByteBuffer) {
        beforeUpload(0, 0)
        texImage2D(type, data)
        afterUpload(type.isHDR, type.bytesPerPixel)
    }

    fun create(creationType: TargetType, uploadType: TargetType, data: Any) {
        beforeUpload(0, 0)
        texImage2D(creationType.internalFormat, uploadType.uploadFormat, uploadType.fillType, data)
        afterUpload(creationType.isHDR, creationType.bytesPerPixel)
    }

    /**
     * force sync should be always enabled, when the image is directly available
     * */
    fun create2(name: String, createImage: () -> BufferedImage, forceSync: Boolean, checkRedundancy: Boolean) {
        if (isDestroyed) throw RuntimeException("Texture $name must be reset first")
        val requiredBudget = textureBudgetUsed + w * h
        if ((requiredBudget > textureBudgetTotal && !loadTexturesSync.peek()) || !isGFXThread()) {
            if (forceSync) {
                GFX.addGPUTask("Texture2D.create0($name)", w, h) {
                    if (!isDestroyed) {
                        create(createImage(), true, checkRedundancy)
                    }
                }
            } else {
                thread(name = "Create Image") {
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
            create(image, false, checkRedundancy)
        } else {
            textureBudgetUsed += requiredBudget
            image.createTexture(this, true, checkRedundancy)
        }
    }

    fun setSize1x1() {
        // a warning, because you might not expect your image to be empty, and wonder why its size is 1x1
        LOGGER.warn("Reduced \"$name\" from $w x $h to 1x1, because it was mono-colored")
        w = 1
        h = 1
    }

    fun create(image: Image, sync: Boolean, checkRedundancy: Boolean) {
        if (sync && isGFXThread()) {
            image.createTexture(this, true, checkRedundancy)
        } else if (isGFXThread() && (w * h > 10_000)) {// large -> avoid the load and create it async
            thread(name = name) {
                image.createTexture(this, false, checkRedundancy)
            }
        } else {
            image.createTexture(this, false, checkRedundancy)
        }
    }

    fun create(image: BufferedImage, sync: Boolean, checkRedundancy: Boolean) {

        w = image.width
        h = image.height
        isCreated = false

        // use the type to correctly create the image
        val buffer = image.data.dataBuffer
        when (image.type) {
            BufferedImage.TYPE_INT_ARGB -> {
                buffer as DataBufferInt
                val data = buffer.data
                if (sync && isGFXThread()) {
                    createBGRA(data, checkRedundancy)
                } else {
                    val data2 = if (checkRedundancy) checkRedundancy(data) else data
                    switchRGB2BGR(data2)
                    GFX.addGPUTask("Texture2D-ARGB", w, h) {
                        createRGBA(data2, checkRedundancy)
                    }
                }
            }
            BufferedImage.TYPE_INT_RGB -> {
                buffer as DataBufferInt
                val data = buffer.data
                if (sync && isGFXThread()) {
                    createBGR(data, checkRedundancy)
                } else {
                    val data2 = if (checkRedundancy) checkRedundancy(data) else data
                    switchRGB2BGR(data2)
                    GFX.addGPUTask("Texture2D-RGB-IntArray($w x $h)", w, h) {
                        createRGB(data2, false)
                    }
                }
            }
            BufferedImage.TYPE_INT_BGR -> {
                buffer as DataBufferInt
                val data = buffer.data
                // data is already in the correct format; no swizzling needed
                if (sync && isGFXThread()) {
                    createRGB(data, checkRedundancy)
                } else {
                    val data2 = if (checkRedundancy) checkRedundancy(data) else data
                    GFX.addGPUTask("Texture2D-BGR", w, h) {
                        createRGB(data2, false)
                    }
                }
            }
            else -> createRGBA(image, sync, checkRedundancy)
        }
    }

    private fun createRGBA(img: BufferedImage, sync: Boolean, checkRedundancy: Boolean) {
        val data = img.getRGB(0, 0, w, h, intArrayPool[w * h, false, false], 0, w)
        val hasAlpha = img.hasAlphaChannel()
        if (!hasAlpha) {
            // ensure opacity
            if (sync && isGFXThread()) {
                createBGR(data, checkRedundancy)
                intArrayPool.returnBuffer(data)
            } else {
                val data2 = if (checkRedundancy) checkRedundancy(data) else data
                switchRGB2BGR(data2)
                val buffer2 = bufferPool[data2.size * 4, false, false]
                val buffer2i = buffer2.asIntBuffer()
                buffer2i.put(data2)
                buffer2i.flip()
                GFX.addGPUTask("Texture2D.createRGB($name)", w, h) {
                    createRGB(buffer2i, checkRedundancy)
                    intArrayPool.returnBuffer(data)
                }
            }
        } else {
            if (sync && isGFXThread()) {
                createBGRA(data, checkRedundancy)
                intArrayPool.returnBuffer(data)
            } else {
                val data2 = if (checkRedundancy) checkRedundancy(data) else data
                switchRGB2BGR(data2)
                val buffer2 = bufferPool[data2.size * 4, false, false]
                buffer2.asIntBuffer().put(data2)
                buffer2.position(0)
                GFX.addGPUTask("Texture2D.createRGBA($name)", w, h) {
                    createRGBA(buffer2, false)
                    intArrayPool.returnBuffer(data)
                }
            }
        }
    }

    fun overridePartially(data: Any, level: Int, x: Int, y: Int, w: Int, h: Int, type: TargetType) {
        ensurePointer()
        bindBeforeUpload()
        texSubImage2D(level, x, y, w, h, type.uploadFormat, type.fillType, data)
        check()
    }

    fun createRGBA(buffer: ByteBuffer) {
        beforeUpload(4, buffer.remaining())
        val t0 = System.nanoTime()
        texImage2D(TargetType.UByteTarget4, buffer)
        bufferPool.returnBuffer(buffer)
        val t1 = System.nanoTime() // 0.02s for a single 4k texture
        afterUpload(false, 4)
        val t2 = System.nanoTime() // 1e-6
        if (w * h > 1e4 && (t2 - t0) * 1e-9f > 0.01f) LOGGER.info("Used ${(t1 - t0) * 1e-9f}s + ${(t2 - t1) * 1e-9f}s to upload ${(w * h) / 1e6f} MPixel image to GPU")
    }

    private fun beforeUpload(channels: Int, size: Int) {
        if (isDestroyed) throw RuntimeException("Texture is already destroyed, call reset() if you want to stream it")
        checkSize(channels, size)
        check()
        ensurePointer()
        bindBeforeUpload()
    }

    private fun beforeUpload() {
        if (isDestroyed) throw RuntimeException("Texture is already destroyed, call reset() if you want to stream it")
        check()
        ensurePointer()
        bindBeforeUpload()
    }

    private fun afterUpload(isHDR: Boolean, bytesPerPixel: Int) {
        locallyAllocated = allocate(locallyAllocated, w * h * bytesPerPixel.toLong())
        isCreated = true
        this.isHDR = isHDR
        filtering(filtering)
        clamping(clamping ?: Clamping.REPEAT)
        check()
        if (isDestroyed) destroy()
    }

    fun checkRedundancy(data: IntArray): IntArray {
        if (w * h <= 1) return data
        val c0 = data[0]
        for (i in 1 until w * h) {
            if (c0 != data[i]) return data
        }
        setSize1x1()
        return intArrayOf(c0)
    }

    fun checkRedundancy(data: IntBuffer) {
        if (w * h <= 1) return
        val c0 = data[0]
        for (i in 1 until w * h) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    fun checkRedundancyMonochrome(data: ByteBuffer) {
        if (w * h <= 1) return
        val c0 = data[0]
        for (i in 1 until w * h) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    fun checkRedundancyMonochrome(data: FloatArray): FloatArray {
        val c0 = data[0]
        for (i in 1 until w * h) {
            if (c0 != data[i]) return data
        }
        setSize1x1()
        return floatArrayOf(data[0])
    }

    fun checkRedundancyMonochrome(data: FloatBuffer) {
        val c0 = data[0]
        for (i in 1 until w * h) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    fun checkRedundancyMonochrome(data: ByteArray): ByteArray {
        val c0 = data[0]
        for (i in 1 until w * h) {
            if (c0 != data[i]) return data
        }
        setSize1x1()
        return byteArrayOf(c0)
    }

    fun checkRedundancyRGBA(data: FloatArray): FloatArray {
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

    fun checkRedundancyRGB(data: FloatArray): FloatArray {
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        for (i in 3 until w * h * 3 step 3) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2]) return data
        }
        setSize1x1()
        return floatArrayOf(c0, c1, c2)
    }

    fun checkRedundancyRGB(data: FloatBuffer) {
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        for (i in 3 until w * h * 3 step 3) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2]) return
        }
        setSize1x1()
        data.limit(3)
    }

    fun checkRedundancyRGBA(data: FloatBuffer) {
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

    fun checkRedundancy(data: ByteArray): ByteArray {
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

    fun checkRedundancy(data: ByteBuffer) {
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

    fun checkRedundancyRG(data: ByteArray): ByteArray {
        val c0 = data[0]
        val c1 = data[1]
        for (i in 2 until w * h * 2 step 2) {
            if (c0 != data[i] || c1 != data[i + 1]) return data
        }
        setSize1x1()
        return byteArrayOf(c0, c1)
    }

    fun checkRedundancyRG(data: FloatArray): FloatArray {
        val c0 = data[0]
        val c1 = data[1]
        for (i in 2 until w * h * 2 step 2) {
            if (c0 != data[i] || c1 != data[i + 1]) return data
        }
        setSize1x1()
        return floatArrayOf(c0, c1)
    }

    fun checkRedundancyRGB(data: ByteArray): ByteArray {
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        for (i in 3 until w * h * 3 step 3) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2])
                return data
        }
        setSize1x1()
        return byteArrayOf(c0, c1, c2)
    }

    fun checkRedundancyRGB(data: ByteBuffer) {
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        for (i in 3 until w * h * 3 step 3) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2])
                return
        }
        setSize1x1()
        data.limit(3)
    }

    fun checkRedundancy(data: ByteBuffer, rgbOnly: Boolean) {
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        if (rgbOnly) {
            for (i in 4 until w * h * 4 step 4) {
                if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2]) return
            }
        } else {
            for (i in 4 until w * h * 4 step 4) {
                if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return
            }
        }
        setSize1x1()
        data.limit(4)
    }

    fun checkRedundancyRG(data: ByteBuffer) {
        // when rgbOnly, check rgb only?
        val c0 = data[0]
        val c1 = data[1]
        for (i in 2 until w * h * 2 step 2) {
            if (c0 != data[i] || c1 != data[i + 1]) return
        }
        setSize1x1()
        data.limit(2)
    }

    fun createBGRA(data: IntArray, checkRedundancy: Boolean) {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        writeAlignment(4 * w)
        // uses bgra instead of rgba to save the swizzle
        texImage2D(GL_RGBA8, GL_BGRA, GL_UNSIGNED_BYTE, data2)
        afterUpload(false, 4)
    }

    fun createBGR(data: IntArray, checkRedundancy: Boolean) {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        // would work without swizzle, but I am not sure, that this is legal,
        // because the number of channels from the input and internal format differ
        writeAlignment(4 * w)
        texImage2D(GL_RGB8, GL_BGRA, GL_UNSIGNED_BYTE, data2)
        afterUpload(false, 3)
    }

    fun createRGB(data: FloatArray, checkRedundancy: Boolean) {
        beforeUpload(3, data.size)
        val floats2 = if (checkRedundancy) checkRedundancyRGB(data) else data
        writeAlignment(12 * w)
        texImage2D(GL_RGB32F, GL_RGB, GL_FLOAT, floats2)
        afterUpload(true, 12)
    }

    fun createRGB(data: FloatBuffer, checkRedundancy: Boolean) {
        beforeUpload(3, data.capacity())
        if (checkRedundancy) checkRedundancyRGB(data)
        writeAlignment(12 * w)
        texImage2D(GL_RGB32F, GL_RGB, GL_FLOAT, data)
        afterUpload(true, 12)
    }

    fun createRGB(data: ByteArray, checkRedundancy: Boolean) {
        beforeUpload(3, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        val buffer = bufferPool[data2.size, false, false]
        buffer.put(data2).flip()
        writeAlignment(3 * w)
        texImage2D(GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 3)
    }

    fun createRGBA(data: IntBuffer, checkRedundancy: Boolean) {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancy(data)
        if (data.order() != ByteOrder.nativeOrder()) throw RuntimeException("Byte order must be native!")
        writeAlignment(4 * w)
        texImage2D(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(false, 4)
    }

    fun createRGBA(data: IntArray, checkRedundancy: Boolean) {
        beforeUpload(1, data.size)
        if (checkRedundancy) checkRedundancy(data)
        writeAlignment(4 * w)
        texImage2D(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(false, 4)
    }

    fun createRGB(data: IntBuffer, checkRedundancy: Boolean) {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancy(data)
        if (data.order() != ByteOrder.nativeOrder()) throw RuntimeException("Byte order must be native!")
        writeAlignment(4 * w)
        texImage2D(GL_RGB8, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(false, 4)
    }

    fun createRGB(data: IntArray, checkRedundancy: Boolean) {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        writeAlignment(4 * w)
        texImage2D(GL_RGB8, GL_RGBA, GL_UNSIGNED_BYTE, data2)
        afterUpload(false, 4)
    }

    fun createTiled(
        creationType: TargetType,
        uploadingType: TargetType,
        dataI: Buffer,
        data1: ByteBuffer?
    ) {
        val width = w
        val height = h
        val tiles = Maths.max(Maths.roundDiv(height, Maths.max(1, (1024) / width)), 1)
        val useTiles = tiles >= 4 && dataI.capacity() > 16
        if (useTiles) {
            GFX.addGPUTask("IntImage", width, height) {
                create(creationType)
                isCreated = false // mark as non-finished
            }
            for (y in 0 until tiles) {
                val y0 = WorkSplitter.partition(y, height, tiles)
                val y1 = WorkSplitter.partition(y + 1, height, tiles)
                val dy = y1 - y0
                GFX.addGPUTask("IntImage", width, dy) {
                    if (!isDestroyed) {
                        isCreated = true // reset to true state
                        dataI.position(y0 * width)
                        dataI.limit(y1 * width)
                        overridePartially(
                            dataI, 0, 0, y0, width, dy,
                            uploadingType
                        )
                        // mark as non-finished again, if we're not done yet
                        if (y1 < height) isCreated = false
                    }
                    if (y1 == height) bufferPool.returnBuffer(data1)
                }
            }
        } else {
            GFX.addGPUTask("IntImage", width, height) {
                create(creationType, uploadingType, dataI)
                bufferPool.returnBuffer(data1)
            }
        }
    }

    /**
     * for testing, create a pixel buffer object for uploading;
     * not faster on Win10 at least
     * */
    fun testRGBPBO(data: IntArray) {

        val t0 = System.nanoTime()

        // pbo
        val pbo = object : OpenGLBuffer(GL_PIXEL_UNPACK_BUFFER, emptyList(), GL_STREAM_DRAW) {

            override fun createNioBuffer() {
                throw NotImplementedError()
            }

            override fun upload(allowResize: Boolean) {
                if (pointer <= 0) pointer = glGenBuffers()
                if (pointer <= 0) throw OutOfMemoryError("Could not generate OpenGL Buffer")
                bindBuffer(type, pointer)
                val size = data.size * 4
                locallyAllocated = allocate(locallyAllocated, size.toLong())
                val t0i = System.nanoTime()
                // todo why is this taking 1s/GB, when we send no data???
                glBufferData(type, size.toLong(), usage)
                val t1i = System.nanoTime()
                println("used ${(t1i - t0i) / 1e9}s for p0/$this")
                isUpToDate = true
                DebugGPUStorage.buffers.add(this)
            }
        }
        pbo.simpleBind() // create it without data

        val ptr = glMapBuffer(pbo.type, GL_WRITE_ONLY)
        if (ptr != null) {
            // upload data into ptr
            ptr.asIntBuffer().put(data)
            glUnmapNamedBuffer(pbo.pointer) // release ptr to mapping buffer
        } else LOGGER.warn("glMapBuffer returned null")
        pbo.unbind()

        val t1 = System.nanoTime()
        println("used ${(t1 - t0) / 1e9}s for p1/$this")

        // then create texture
        thread(name = name) {
            Thread.sleep(500)
            GFX.addGPUTask(name, w, h) {
                val t2 = System.nanoTime()
                beforeUpload(1, data.size)
                writeAlignment(4 * w)
                pbo.simpleBind()
                texImage2D(GL_RGB8, GL_RGBA, GL_UNSIGNED_BYTE, null, unbind = false)
                afterUpload(false, 4)
                pbo.unbind()

                val t3 = System.nanoTime()
                println("used ${(t3 - t2) / 1e9}s for p2/$this")

                pbo.destroy()
            }
        }
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
        val buffer = bufferPool[data.size, false, false]
        buffer.put(data2).flip()
        texImage2D(GL_RG, GL_RG, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 2)
    }

    fun createRG(data: FloatArray, checkRedundancy: Boolean) {
        beforeUpload(2, data.size)
        val data2 = if (checkRedundancy) checkRedundancyRG(data) else data
        val buffer = bufferPool[data.size, false, false]
        buffer.asFloatBuffer().put(data2)
        texImage2D(GL_RG, GL_RG, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 8)
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
        writeAlignment(4 * w)
        texImage2D(GL_R32F, GL_RED, GL_FLOAT, data)
        afterUpload(true, 4)
    }

    /**
     * creates a monochrome float32 image on the GPU
     * used by SDF
     * */
    fun createMonochrome(data: FloatArray, checkRedundancy: Boolean) {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancyMonochrome(data) else data
        writeAlignment(4 * w)
        texImage2D(GL_R32F, GL_RED, GL_FLOAT, data2)
        afterUpload(true, 4)
    }

    /**
     * creates a monochrome float16 image on the GPU
     * */
    fun createMonochromeFP16(data: FloatBuffer, checkRedundancy: Boolean) {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancyMonochrome(data)
        writeAlignment(4 * w)
        texImage2D(GL_R16F, GL_RED, GL_FLOAT, data)
        afterUpload(true, 4)
    }

    fun createBGR(data: ByteArray, checkRedundancy: Boolean) {
        beforeUpload(3, data.size)
        val data2 = if (checkRedundancy) checkRedundancyRGB(data) else data
        val buffer = bufferPool[data2.size, false, false]
        buffer.put(data2).flip()
        texImage2D(GL_RGBA8, GL_BGR, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 3)
    }

    fun createBGR(data: ByteBuffer, checkRedundancy: Boolean) {
        beforeUpload(3, data.remaining())
        if (checkRedundancy) checkRedundancyRGB(data)
        texImage2D(GL_RGBA8, GL_BGR, GL_UNSIGNED_BYTE, data)
        bufferPool.returnBuffer(data)
        afterUpload(false, 3)
    }

    fun createMonochrome(data: ByteArray, checkRedundancy: Boolean) {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancyMonochrome(data) else data
        val buffer = bufferPool[data2.size, false, false]
        buffer.put(data2).flip()
        texImage2D(TargetType.UByteTarget1, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 1)
    }

    fun createRGBA(data: FloatArray, checkRedundancy: Boolean) {
        beforeUpload(4, data.size)
        val data2 = if (checkRedundancy && w * h > 1) checkRedundancyRGBA(data) else data
        val byteBuffer = bufferPool[data2.size * 4, false, false]
        byteBuffer.asFloatBuffer().put(data2).flip()
        // rgba32f as internal format is extremely important... otherwise the value is cropped
        texImage2D(TargetType.FloatTarget4, byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(true, 16)
    }

    fun createRGBA(data: FloatBuffer, buffer: ByteBuffer, checkRedundancy: Boolean) {

        beforeUpload(4, data.capacity())
        if (checkRedundancy && w * h > 1) checkRedundancyRGBA(data)

        // rgba32f as internal format is extremely important... otherwise the value is cropped
        texImage2D(TargetType.FloatTarget4, buffer)

        afterUpload(true, 16)

    }

    fun createBGRA(data: ByteArray, checkRedundancy: Boolean) {
        checkSize(4, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        val buffer = bufferPool[data2.size, false, false]
        buffer.put(data2).flip()
        beforeUpload(4, buffer.remaining())
        texImage2D(GL_RGBA, GL_BGRA, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 4)
    }

    fun createBGRA(buffer: ByteBuffer, checkRedundancy: Boolean) {
        if (checkRedundancy) checkRedundancy(buffer)
        beforeUpload(4, buffer.remaining())
        texImage2D(GL_RGBA, GL_BGRA, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 4)
    }

    fun createRGBA(data: ByteArray, checkRedundancy: Boolean) {
        checkSize(4, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        val buffer = bufferPool[data2.size, false, false]
        buffer.put(data2).flip()
        createRGBA(buffer, false)
    }

    fun createARGB(data: ByteArray, checkRedundancy: Boolean) {
        checkSize(4, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        val buffer = bufferPool[data2.size, false, false]
        for (i in 0 until w * h * 4 step 4) {
            buffer.put(data2[i + 1]) // r
            buffer.put(data2[i + 2]) // g
            buffer.put(data2[i + 3]) // b
            buffer.put(data2[i]) // a
        }
        buffer.flip()
        createRGBA(buffer, false)
    }

    /** creates the texture, and returns the buffer */
    fun createRGBA(data: ByteBuffer, checkRedundancy: Boolean) {
        beforeUpload(4, data.remaining())
        if (checkRedundancy) checkRedundancy(data, false)
        texImage2D(TargetType.UByteTarget4, data)
        bufferPool.returnBuffer(data)
        afterUpload(false, 4)
    }

    fun createRGB(data: ByteBuffer, checkRedundancy: Boolean) {
        beforeUpload(3, data.remaining())
        if (checkRedundancy) checkRedundancy(data, true)
        // texImage2D(TargetType.UByteTarget3, buffer)
        writeAlignment(3 * w)
        texImage2D(GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE, data)
        bufferPool.returnBuffer(data)
        afterUpload(false, 3)
    }

    fun createDepth(lowQuality: Boolean = false) {
        beforeUpload()
        texImage2D(
            if (lowQuality) GL_DEPTH_COMPONENT16 else GL_DEPTH_COMPONENT32F,
            GL_DEPTH_COMPONENT, GL_FLOAT, null
        )
        afterUpload(!lowQuality, if (lowQuality) 2 else 4)
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
            glTexParameteri(target, GL_TEXTURE_WRAP_S, type)
            glTexParameteri(target, GL_TEXTURE_WRAP_T, type)
        }
    }

    var autoUpdateMipmaps = true

    private fun filtering(filtering: GPUFiltering) {
        if (withMultisampling) {
            this.filtering = GPUFiltering.TRULY_NEAREST
            // multisample textures only support nearest filtering;
            // they don't accept the command to be what they are either
            return
        }
        if (!hasMipmap && filtering.needsMipmap && (w > 1 || h > 1)) {
            glGenerateMipmap(target)
            hasMipmap = true
            if (GFX.supportsAnisotropicFiltering) {
                val anisotropy = GFX.anisotropy
                glTexParameteri(target, GL_TEXTURE_LOD_BIAS, 0)
                glTexParameterf(target, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy)
            }
            // whenever the base mipmap is changed, the mipmaps will be updated :)
            glTexParameteri(target, GL_GENERATE_MIPMAP, if (autoUpdateMipmaps) GL_TRUE else GL_FALSE)
            // is called afterwards anyways
            // glTexParameteri(tex2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        }
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, filtering.min)
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, filtering.mag)
        this.filtering = filtering
    }

    var hasMipmap = false

    private fun bindBeforeUpload() {
        if (pointer <= -1) throw RuntimeException("Pointer must be defined")
        bindTexture(target, pointer)
    }

    override fun bind(index: Int, filtering: GPUFiltering, clamping: Clamping): Boolean {
        checkSession()
        if (pointer > 0 && isCreated) {
            if (isBoundToSlot(index)) {
                if (filtering != this.filtering || clamping != this.clamping) {
                    activeSlot(index) // force this to be bound
                    ensureFilterAndClamping(filtering, clamping)
                }
                return false
            }
            activeSlot(index)
            val result = bindTexture(target, pointer)
            ensureFilterAndClamping(filtering, clamping)
            return result
        } else throw IllegalStateException("Cannot bind non-created texture!, $name")
    }

    fun bind(index: Int) = bind(index, filtering, clamping ?: Clamping.REPEAT)

    override fun destroy() {
        isCreated = false
        isDestroyed = true
        val pointer = pointer
        if (pointer > -1) {
            synchronized(texturesToDelete) {
                // allocation counter is removed a bit early, shouldn't be too bad
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
        return slot in boundTextures.indices && boundTextures[slot] == pointer
    }

    fun copyFrom(src: Texture2D) {
        if (src.pointer <= 0 || pointer <= 0) throw IllegalArgumentException("Both textures need to be created for copyFrom()")
        check()
        invalidateBinding()
        check()
        bindTexture(target, pointer)
        check()
        glCopyTextureSubImage2D(src.pointer, 0, 0, 0, 0, 0, min(w, src.w), min(h, src.h))
        check()
    }

    override fun wrapAsFramebuffer(): IFramebuffer {
        return object : IFramebuffer {
            override val name: String get() = this@Texture2D.name
            override val pointer: Int get() = -1
            override val w: Int get() = this@Texture2D.w
            override val h: Int get() = this@Texture2D.h
            override val samples: Int get() = this@Texture2D.samples
            override val numTextures: Int get() = 1
            override fun ensure() {}
            override fun checkSession() = this@Texture2D.checkSession()
            override fun bindDirectly() =
                throw NotImplementedError()

            override fun bindDirectly(w: Int, h: Int) =
                throw NotImplementedError()

            override fun destroy() = this@Texture2D.destroy()

            override fun attachFramebufferToDepth(targetCount: Int, fpTargets: Boolean) =
                throw NotImplementedError()

            override fun attachFramebufferToDepth(targets: Array<TargetType>) =
                throw NotImplementedError()

            override fun bindTextureI(index: Int, offset: Int, nearest: GPUFiltering, clamping: Clamping) {
                if (offset == 0) this@Texture2D.bind(index, nearest, clamping)
                else throw IndexOutOfBoundsException()
            }

            override fun bindTextures(offset: Int, nearest: GPUFiltering, clamping: Clamping) {
                this@Texture2D.bind(0, nearest, clamping)
            }

            override fun getTextureI(index: Int) = if (index == 0) this@Texture2D else throw IndexOutOfBoundsException()

            override val depthTexture = null

        }
    }

    companion object {

        var alwaysBindTexture = false
        var wasModifiedInComputePipeline = false

        fun BufferedImage.hasAlphaChannel() = colorModel.hasAlpha()

        val bufferPool = ByteBufferPool(64)
        val byteArrayPool = ByteArrayPool(64)
        val intArrayPool = IntArrayPool(64)
        val floatArrayPool = FloatArrayPool(64)

        fun freeUnusedEntries() {
            bufferPool.freeUnusedEntries()
            byteArrayPool.freeUnusedEntries()
            intArrayPool.freeUnusedEntries()
            floatArrayPool.freeUnusedEntries()
        }

        @Docs("Remove all pooled entries")
        fun gc() {
            bufferPool.gc()
            byteArrayPool.gc()
            intArrayPool.gc()
            floatArrayPool.gc()
        }

        var allocated = 0L
        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated += newValue - oldValue
            return newValue
        }

        var boundTextureSlot = 0
        val boundTextures = IntArray(64) { -1 }
        val tmp4i = IntArray(4)

        fun invalidateBinding() {
            boundTextureSlot = -1
            activeSlot(0)
            for (i in boundTextures.indices) {
                boundTextures[i] = -1
            }
        }

        fun activeSlot(index: Int) {
            if (alwaysBindTexture || index != boundTextureSlot) {
                glActiveTexture(GL_TEXTURE0 + index)
                boundTextureSlot = index
            }
        }

        /**
         * bind the texture, the slot doesn't matter
         * @return whether the texture was actively bound
         * */
        fun bindTexture(mode: Int, pointer: Int): Boolean {
            if (pointer < 0) throw IllegalArgumentException("Pointer must be valid")
            if (wasModifiedInComputePipeline) {
                glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
                wasModifiedInComputePipeline = false
            }
            return if (alwaysBindTexture || boundTextures[boundTextureSlot] != pointer) {
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
            if (creationSession != GFXState.session || creationIndex == creationIndices.size) {
                creationIndex = 0
                creationSession = GFXState.session
                glGenTextures(creationIndices)
                check()
            }
            return creationIndices[creationIndex++]
        }

        fun switchRGB2BGR(values: IntArray) {
            // convert argb to abgr
            val agMask = 0xff00ff00.toInt()
            for (i in values.indices) {
                val v = values[i]
                val r = v.shr(16).and(0xff)
                val ag = v.and(agMask)
                val b = v.and(0xff)
                values[i] = ag or r or b.shl(16)
            }
        }

        fun switchRGB2BGR(values: IntBuffer) {
            // convert argb to abgr
            val agMask = 0xff00ff00.toInt()
            for (i in 0 until values.limit()) {
                val v = values[i]
                val r = v.shr(16).and(0xff)
                val ag = v.and(agMask)
                val b = v.and(0xff)
                values.put(i, ag or r or b.shl(16))
            }
        }

        fun destroyTextures() {
            synchronized(texturesToDelete) {
                if (texturesToDelete.isNotEmpty()) {
                    // unbind old textures
                    boundTextureSlot = -1
                    boundTextures.fill(-1)
                    for (slot in boundTextures.indices) {
                        activeSlot(slot)
                        bindTexture(GL_TEXTURE_2D, 0)
                    }
                    glDeleteTextures(texturesToDelete.toIntArray())
                    texturesToDelete.clear()
                }
            }
        }

        private fun getAlignment(w: Int): Int {
            return when {
                w and 7 == 0 -> 8
                w and 3 == 0 -> 4
                w and 1 == 0 -> 2
                else -> 1
            }
        }

        fun readAlignment(w: Int) {
            glPixelStorei(GL_PACK_ALIGNMENT, getAlignment(w))
        }

        fun writeAlignment(w: Int) {
            glPixelStorei(GL_UNPACK_ALIGNMENT, getAlignment(w))
        }

    }

}