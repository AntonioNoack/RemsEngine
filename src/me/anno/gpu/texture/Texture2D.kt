package me.anno.gpu.texture

import me.anno.Build
import me.anno.Time
import me.anno.cache.ICacheData
import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.Docs
import me.anno.utils.structures.Callback
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.check
import me.anno.gpu.GFX.isGFXThread
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.GFX.maxBoundTextures
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.OpenGLBuffer.Companion.bindBuffer
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.Image
import me.anno.image.ImageTransform
import me.anno.image.raw.GPUImage
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.clamp
import me.anno.utils.Color.convertABGR2ARGB
import me.anno.utils.hpc.WorkSplitter
import me.anno.utils.pooling.ByteArrayPool
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.pooling.FloatArrayPool
import me.anno.utils.pooling.IntArrayPool
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.f1
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GL46C.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferInt
import java.nio.*
import kotlin.concurrent.thread

@Suppress("unused")
open class Texture2D(
    override val name: String,
    final override var width: Int,
    final override var height: Int,
    samples: Int
) : ICacheData, ITexture2D {

    constructor(name: String, img: Image, checkRedundancy: Boolean) : this(name, img.width, img.height, 1) {
        create(img, true, checkRedundancy) { _, _ -> }
        filtering(Filtering.NEAREST)
    }

    constructor(img: Image, checkRedundancy: Boolean) : this("img", img.width, img.height, 1) {
        create(img, true, checkRedundancy) { _, _ -> }
        filtering(Filtering.NEAREST)
    }

    override val samples = clamp(samples, 1, GFX.maxSamples)
    var owner: Framebuffer? = null

    var internalFormat = 0
    var border = 0

    override var channels: Int = 0

    override fun toString() = "Texture2D(\"$name\"@$pointer, $width x $height x $samples, ${GFX.getName(internalFormat)})"

    /**
     * Pseudo-Reference, such that ImageGPUCache[ref] = this;
     * Only is valid as long as the texture is created ofc.
     *
     * If you need this property for other Texture classes or similar, write me :).
     * */
    var ref: FileReference = InvalidRef
        private set
        get() {
            val numChannels = getNumChannels(internalFormat)
            val hasAlphaChannel = numChannels == 4
            if (field == InvalidRef) field = GPUImage(this, numChannels, hasAlphaChannel).ref
            return field
        }

    private val withMultisampling = samples > 1

    val target = if (withMultisampling) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D
    val state get(): Int = pointer * 4 + isDestroyed.toInt(2) + wasCreated.toInt(1)

    var pointer = 0
    var session = 0

    fun checkSession() {
        if (session != GFXState.session) {
            session = GFXState.session
            pointer = 0
            wasCreated = false
            isDestroyed = false
            locallyAllocated = allocate(locallyAllocated, 0L)
            createdW = 0
            createdH = 0
        }
    }

    fun resize(w: Int, h: Int, type: TargetType) {
        if (w != this.width || h != this.height) {
            this.width = w
            this.height = h
            destroy() // needed?
            reset()
            create(type)
        }
    }

    override var wasCreated = false
    override var isDestroyed = false

    override var filtering = Filtering.TRULY_NEAREST
    override var clamping = Clamping.REPEAT

    // only used for images with exif rotation tag...
    var rotation: ImageTransform? = null

    var locallyAllocated = 0L

    var createdW = 0
    var createdH = 0

    override var isHDR = false

    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    @Deprecated("This feature is problematic, because it doesn't exist on mobile. Use /grayscale.png in your file path instead.")
    fun swizzleMonochrome() {
        swizzle(GL_RED, GL_RED, GL_RED, GL_ONE)
    }

    // could and should be used for roughness/metallic like textures in the future
    fun swizzle(r: Int, g: Int, b: Int, a: Int) {
        TextureHelper.swizzle(target, r, g, b, a)
    }

    fun ensurePointer() {
        checkSession()
        if (isDestroyed) throw RuntimeException("Texture was destroyed")
        if (pointer == 0) {
            check()
            pointer = createTexture()
            if (pointer != 0 && Build.isDebug) {
                synchronized(DebugGPUStorage.tex2d) {
                    DebugGPUStorage.tex2d.add(this)
                }
            }
            // many textures can be created by the console log and the fps viewer constantly xD
            // maybe we should use allocation free versions there xD
            check()
        }
        if (pointer == 0) throw RuntimeException("Could not allocate texture pointer")
    }

    private fun unbindUnpackBuffer() {
        bindBuffer(GL_PIXEL_UNPACK_BUFFER, 0)
    }

    fun setAlignmentAndBuffer(w: Int, dataFormat: Int, dataType: Int, unbind: Boolean) {
        val typeSize = when (dataType) {
            GL_UNSIGNED_BYTE, GL_BYTE -> 1
            GL_UNSIGNED_SHORT, GL_SHORT, GL_HALF_FLOAT -> 2
            GL_UNSIGNED_INT, GL_INT, GL_FLOAT -> 4
            GL_DOUBLE -> 8
            else -> {
                RuntimeException("Unknown data type $dataType").printStackTrace()
                1
            }
        }
        val numChannels = when (dataFormat) {
            GL_RED, GL_RED_INTEGER -> 1
            GL_RG, GL_RG_INTEGER -> 2
            GL_RGB, GL_BGR, GL_RGB_INTEGER -> 3
            GL_RGBA, GL_BGRA, GL_RGBA_INTEGER -> 4
            else -> {
                RuntimeException("Unknown data format ${GFX.getName(dataFormat)}")
                    .printStackTrace()
                1
            }
        }
        setWriteAlignment(w * typeSize * numChannels)
        if (unbind) unbindUnpackBuffer()
    }

    fun upload(internalFormat: Int, dataFormat: Int, dataType: Int, data: Any?, unbind: Boolean = true) {
        if (data is ByteArray) { // helper
            val tmp = bufferPool.createBuffer(data.size)
            tmp.put(data).flip()
            upload(internalFormat, dataFormat, dataType, tmp, unbind)
            bufferPool.returnBuffer(tmp)
            return
        }
        bindBeforeUpload()
        val w = width
        val h = height
        val target = target
        if (w * h <= 0) throw IllegalArgumentException("Cannot create empty texture")
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
                is FloatArray -> glTexSubImage2D(target, 0, 0, 0, w, h, dataFormat, dataType, data)
                is DoubleArray -> glTexSubImage2D(target, 0, 0, 0, w, h, dataFormat, dataType, data)
                else -> throw IllegalArgumentException("${data::class.simpleName} is not supported")
            }
        } else {
            if (withMultisampling) {
                var depthOwner = owner
                while (true) {
                    depthOwner = depthOwner?.depthAttachment ?: break
                }
                val needsFixedSampleLocations = when (depthOwner?.depthBufferType) {
                    DepthBufferType.INTERNAL -> true
                    DepthBufferType.TEXTURE, DepthBufferType.TEXTURE_16 -> !GFX.supportsDepthTextures
                    else -> false
                }
                glTexImage2DMultisample(target, samples, internalFormat, w, h, needsFixedSampleLocations)
                check()
            } else {
                if (data != null) setAlignmentAndBuffer(w, dataFormat, dataType, unbind)
                when (data) {
                    is ByteBuffer -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is ShortBuffer -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is IntBuffer -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is FloatBuffer -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is DoubleBuffer -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is IntArray -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is ShortArray -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is FloatArray -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    is DoubleArray -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, data)
                    null -> glTexImage2D(
                        target, 0, internalFormat, w, h, 0, dataFormat, dataType,
                        null as ByteBuffer?
                    )
                    else -> throw IllegalArgumentException("${data::class.simpleName} is not supported")
                }
                check()
            }
            this.internalFormat = internalFormat
            // todo do we keep this, or do we strive for consistency?
            if (getNumChannels(internalFormat) == 1) {
                swizzleMonochrome()
            }
            createdW = w
            createdH = h
        }
    }

    fun uploadPartially(
        level: Int,
        x: Int, y: Int, w: Int, h: Int,
        dataFormat: Int,
        dataType: Int,
        data: Any,
        unbind: Boolean = true
    ) {
        ensurePointer()
        bindBeforeUpload()
        setAlignmentAndBuffer(w, dataFormat, dataType, unbind)
        when (data) {
            is ByteBuffer -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            is ShortBuffer -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            is IntBuffer -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            is FloatBuffer -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            is DoubleBuffer -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            is IntArray -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            is FloatArray -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            is DoubleArray -> glTexSubImage2D(target, level, x, y, w, h, dataFormat, dataType, data)
            else -> throw IllegalArgumentException("${data::class.simpleName} is not supported")
        }
    }

    fun upload(type: TargetType, data: ByteBuffer?) {
        upload(type.internalFormat, type.uploadFormat, type.fillType, data)
    }

    fun upload(type: TargetType, data: ByteArray?) {
        upload(type.internalFormat, type.uploadFormat, type.fillType, data)
    }

    fun createRGB() = create(TargetType.UInt8x3)
    fun createRGBA() = create(TargetType.UInt8x4)
    fun createFP32() = create(TargetType.Float32x4)

    fun create(type: TargetType) {
        beforeUpload(0, 0)
        upload(type, null as ByteArray?)
        afterUpload(type.isHDR, type.bytesPerPixel, type.channels)
    }

    fun create(type: TargetType, data: Any?) {
        create(type, type, data)
    }

    fun create(creationType: TargetType, uploadType: TargetType, data: Any?) {
        beforeUpload(0, 0)
        upload(creationType.internalFormat, uploadType.uploadFormat, uploadType.fillType, data)
        afterUpload(creationType.isHDR, creationType.bytesPerPixel, uploadType.channels)
    }

    fun create(image: Image, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        width = image.width
        height = image.height
        if (isDestroyed) throw RuntimeException("Texture $name must be reset first")
        val requiredBudget = textureBudgetUsed + width * height
        if ((requiredBudget > textureBudgetTotal && !loadTexturesSync.peek()) || !isGFXThread()) {
            create(image, false, checkRedundancy, callback)
        } else {
            textureBudgetUsed += requiredBudget
            image.createTexture(this, true, checkRedundancy, callback)
        }
    }

    fun setSize1x1() {
        // a warning, because you might not expect your image to be empty, and wonder why its size is 1x1
        LOGGER.warn("Reduced \"$name\" from $width x $height to 1x1, because it was mono-colored")
        width = 1
        height = 1
    }

    fun create(image: Image, sync: Boolean, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        if (sync && isGFXThread()) {
            image.createTexture(this, true, checkRedundancy, callback)
        } else if (isGFXThread() && (width * height > 10_000)) {// large -> avoid the load and create it async
            thread(name = name) {
                image.createTexture(this, false, checkRedundancy, callback)
            }
        } else {
            image.createTexture(this, false, checkRedundancy, callback)
        }
    }

    fun create(image: BufferedImage, sync: Boolean, checkRedundancy: Boolean): (() -> Unit)? {

        width = image.width
        height = image.height
        wasCreated = false

        // use the type to correctly create the image
        val buffer = image.data.dataBuffer
        when (image.type) {
            BufferedImage.TYPE_INT_ARGB -> {
                buffer as DataBufferInt
                val data = buffer.data
                if (sync && isGFXThread()) {
                    createBGRA(data, checkRedundancy)
                    return null
                } else {
                    val data2 = if (checkRedundancy) checkRedundancy(data) else data
                    switchRGB2BGR(data2)
                    return {
                        createRGBA(data2, checkRedundancy)
                    }
                }
            }

            BufferedImage.TYPE_INT_RGB -> {
                buffer as DataBufferInt
                val data = buffer.data
                if (sync && isGFXThread()) {
                    createBGR(data, checkRedundancy)
                    return null
                } else {
                    val data2 = if (checkRedundancy) checkRedundancy(data) else data
                    switchRGB2BGR(data2)
                    return {
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
                    return null
                } else {
                    val data2 = if (checkRedundancy) checkRedundancy(data) else data
                    return {
                        createRGB(data2, false)
                    }
                }
            }

            BufferedImage.TYPE_BYTE_GRAY -> {
                buffer as DataBufferByte
                val data = buffer.data
                // data is already in the correct format; no swizzling needed
                if (sync && isGFXThread()) {
                    createMonochrome(data, checkRedundancy)
                    return null
                } else {
                    val data2 = if (checkRedundancy) checkRedundancy(data) else data
                    return {
                        createMonochrome(data2, false)
                    }
                }
            }

            else -> {
                val data = image.getRGB(0, 0, width, height, intArrayPool[width * height, false, false], 0, width)
                val hasAlpha = image.hasAlphaChannel()
                if (!hasAlpha) {
                    // ensure opacity
                    if (sync && isGFXThread()) {
                        createBGR(data, checkRedundancy)
                        intArrayPool.returnBuffer(data)
                        return null
                    } else {
                        val data2 = if (checkRedundancy) checkRedundancy(data) else data
                        switchRGB2BGR(data2)
                        val buffer2 = bufferPool[data2.size * 4, false, false]
                        val buffer2i = buffer2.asIntBuffer()
                        buffer2i.put(data2)
                        buffer2i.flip()
                        return {
                            createRGB(buffer2i, checkRedundancy)
                            intArrayPool.returnBuffer(data)
                        }
                    }
                } else {
                    if (sync && isGFXThread()) {
                        createBGRA(data, checkRedundancy)
                        intArrayPool.returnBuffer(data)
                        return null
                    } else {
                        val data2 = if (checkRedundancy) checkRedundancy(data) else data
                        switchRGB2BGR(data2)
                        val buffer2 = bufferPool[data2.size * 4, false, false]
                        buffer2.asIntBuffer().put(data2)
                        buffer2.position(0)
                        return {
                            createRGBA(buffer2, false)
                            intArrayPool.returnBuffer(data)
                        }
                    }
                }
            }
        }
    }

    fun overridePartially(data: Any, level: Int, x: Int, y: Int, w: Int, h: Int, type: TargetType) {
        uploadPartially(level, x, y, w, h, type.uploadFormat, type.fillType, data)
        check()
    }

    /*fun createRGBA(buffer: ByteBuffer) {
        beforeUpload(4, buffer.remaining())
        val t0 = Time.nanoTime
        texImage2D(TargetType.UByteTarget4, buffer)
        bufferPool.returnBuffer(buffer)
        val t1 = Time.nanoTime // 0.02s for a single 4k texture
        afterUpload(false, 4)
        val t2 = Time.nanoTime // 1e-6
        if (w * h > 1e4 && (t2 - t0) * 1e-9f > 0.01f) LOGGER.info("Used ${(t1 - t0) * 1e-9f}s + ${(t2 - t1) * 1e-9f}s to upload ${(w * h) / 1e6f} MPixel image to GPU")
    }*/

    fun beforeUpload(channels: Int, size: Int) {
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

    fun afterUpload(isHDR: Boolean, bytesPerPixel: Int, channels: Int) {
        locallyAllocated = allocate(locallyAllocated, width * height * bytesPerPixel.toLong())
        wasCreated = true
        this.isHDR = isHDR
        this.channels = channels
        filtering(filtering)
        clamping(clamping)
        check()
        if (Build.isDebug) glObjectLabel(GL_TEXTURE, pointer, name)
        if (isDestroyed) destroy()
    }

    fun checkRedundancy(data: IntArray): IntArray {
        if (width * height <= 1) return data
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return data
        }
        setSize1x1()
        return intArrayOf(c0)
    }

    fun checkRedundancy(data: IntBuffer) {
        if (width * height <= 1) return
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    fun checkRedundancyMonochrome(data: ByteBuffer) {
        if (data.capacity() <= 1) return
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    fun checkRedundancyMonochrome(data: FloatArray): FloatArray {
        if (data.isEmpty()) return data
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return data
        }
        setSize1x1()
        return floatArrayOf(data[0])
    }

    fun checkRedundancyMonochrome(data: FloatBuffer) {
        if (data.capacity() < 1) return
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    fun checkRedundancyMonochrome(data: ShortBuffer) {
        if (data.capacity() < 1) return
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return
        }
        setSize1x1()
        data.limit(1)
    }

    fun checkRedundancyMonochrome(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        val c0 = data[0]
        for (i in 1 until width * height) {
            if (c0 != data[i]) return data
        }
        setSize1x1()
        return byteArrayOf(c0)
    }

    fun checkRedundancyRGBA(data: FloatArray): FloatArray {
        if (data.size < 4) return data
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        for (i in 4 until width * height * 4 step 4) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return data
        }
        setSize1x1()
        return floatArrayOf(c0, c1, c2, c3)
    }

    fun checkRedundancyRGB(data: FloatArray): FloatArray {
        if (data.size < 3) return data
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        for (i in 3 until width * height * 3 step 3) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2]) return data
        }
        setSize1x1()
        return floatArrayOf(c0, c1, c2)
    }

    fun checkRedundancyRGB(data: FloatBuffer) {
        if (data.capacity() < 3) return
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        for (i in 3 until width * height * 3 step 3) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2]) return
        }
        setSize1x1()
        data.limit(3)
    }

    fun checkRedundancyRGBA(data: FloatBuffer) {
        if (data.capacity() < 4) return
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        for (i in 4 until width * height * 4 step 4) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return
        }
        setSize1x1()
        data.limit(4)
    }

    fun checkRedundancy(data: ByteArray): ByteArray {
        if (data.size < 4) return data
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        for (i in 4 until width * height * 4 step 4) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return data
        }
        setSize1x1()
        return byteArrayOf(c0, c1, c2, c3)
    }

    fun checkRedundancy(data: ByteBuffer) {
        if (data.capacity() < 4) return
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        for (i in 4 until width * height * 4 step 4) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return
        }
        setSize1x1()
        data.limit(4)
    }

    fun checkRedundancyRG(data: ByteArray): ByteArray {
        if (data.size < 2) return data
        val c0 = data[0]
        val c1 = data[1]
        for (i in 2 until width * height * 2 step 2) {
            if (c0 != data[i] || c1 != data[i + 1]) return data
        }
        setSize1x1()
        return byteArrayOf(c0, c1)
    }

    fun checkRedundancyRG(data: FloatArray): FloatArray {
        if (data.size < 2) return data
        val c0 = data[0]
        val c1 = data[1]
        for (i in 2 until width * height * 2 step 2) {
            if (c0 != data[i] || c1 != data[i + 1]) return data
        }
        setSize1x1()
        return floatArrayOf(c0, c1)
    }

    fun checkRedundancyRGB(data: ByteArray): ByteArray {
        if (data.size < 3) return data
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        for (i in 3 until width * height * 3 step 3) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2])
                return data
        }
        setSize1x1()
        return byteArrayOf(c0, c1, c2)
    }

    fun checkRedundancyRGB(data: ByteBuffer) {
        if (data.capacity() < 3) return
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        for (i in 3 until width * height * 3 step 3) {
            if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2])
                return
        }
        setSize1x1()
        data.limit(3)
    }

    fun checkRedundancy(data: ByteBuffer, rgbOnly: Boolean) {
        if (data.capacity() < 4) return
        val c0 = data[0]
        val c1 = data[1]
        val c2 = data[2]
        val c3 = data[3]
        if (rgbOnly) {
            for (i in 4 until width * height * 4 step 4) {
                if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2]) return
            }
        } else {
            for (i in 4 until width * height * 4 step 4) {
                if (c0 != data[i] || c1 != data[i + 1] || c2 != data[i + 2] || c3 != data[i + 3]) return
            }
        }
        setSize1x1()
        data.limit(4)
    }

    fun checkRedundancyRG(data: ByteBuffer) {
        // when rgbOnly, check rgb only?
        if (data.capacity() < 2) return
        val c0 = data[0]
        val c1 = data[1]
        for (i in 2 until width * height * 2 step 2) {
            if (c0 != data[i] || c1 != data[i + 1]) return
        }
        setSize1x1()
        data.limit(2)
    }

    fun createBGRA(data: IntArray, checkRedundancy: Boolean) {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        switchRGB2BGR(data2)
        setWriteAlignment(4 * width)
        upload(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, data2)
        afterUpload(false, 4, 4)
        switchRGB2BGR(data2)
    }

    fun createBGR(data: IntArray, checkRedundancy: Boolean) {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        switchRGB2BGR(data2)
        setWriteAlignment(4 * width)
        upload(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, data2)
        afterUpload(false, 4, 3)
        switchRGB2BGR(data2)
    }

    fun createRGB(data: FloatArray, checkRedundancy: Boolean) {
        beforeUpload(3, data.size)
        val floats2 = if (checkRedundancy) checkRedundancyRGB(data) else data
        setWriteAlignment(12 * width)
        upload(GL_RGB32F, GL_RGB, GL_FLOAT, floats2)
        afterUpload(true, 12, 3)
    }

    fun createRGB(data: FloatBuffer, checkRedundancy: Boolean) {
        beforeUpload(3, data.capacity())
        if (checkRedundancy) checkRedundancyRGB(data)
        setWriteAlignment(12 * width)
        upload(GL_RGB32F, GL_RGB, GL_FLOAT, data)
        afterUpload(true, 12, 3)
    }

    fun createRGB(data: ByteArray, checkRedundancy: Boolean) {
        beforeUpload(3, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        val buffer = bufferPool[data2.size, false, false]
        buffer.put(data2).flip()
        setWriteAlignment(3 * width)
        upload(GL_RGBA8, GL_RGB, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 4, 3)
    }

    fun createRGBA(data: IntBuffer, checkRedundancy: Boolean) {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancy(data)
        if (data.order() != ByteOrder.nativeOrder()) throw RuntimeException("Byte order must be native!")
        setWriteAlignment(4 * width)
        upload(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(false, 4, 4)
    }

    /**
     * warning: will assume RGBA colors, but the engine works with ARGB internally!
     * */
    fun createRGBA(data: IntArray, checkRedundancy: Boolean) {
        beforeUpload(1, data.size)
        if (checkRedundancy) checkRedundancy(data)
        setWriteAlignment(4 * width)
        upload(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(false, 4, 4)
    }

    fun createRGB(data: IntBuffer, checkRedundancy: Boolean) {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancy(data)
        if (data.order() != ByteOrder.nativeOrder()) throw RuntimeException("Byte order must be native!")
        setWriteAlignment(4 * width)
        upload(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(false, 4, 3)
    }

    /**
     * warning: will assume RGBA colors, but the engine works with ARGB internally!
     * */
    fun createRGB(data: IntArray, checkRedundancy: Boolean) {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        setWriteAlignment(4 * width)
        upload(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, data2)
        afterUpload(false, 4, 3)
    }

    fun createTiled(
        creationType: TargetType,
        uploadingType: TargetType,
        dataI: Buffer,
        data1: ByteBuffer?,
        numChannels: Int,
        callback: Callback<ITexture2D>
    ) {
        val width = width
        val height = height
        val tiles = Maths.max(Maths.roundDiv(height, Maths.max(1, (1024) / width)), 1)
        val useTiles = tiles >= 4 && dataI.capacity() > 16
        if (useTiles) {
            GFX.addGPUTask("IntImage", width, height) {
                if (!isDestroyed) {
                    create(creationType)
                    this.channels = numChannels
                    wasCreated = false // mark as non-finished
                } else LOGGER.warn("Image was already destroyed")
            }
            for (y in 0 until tiles) {
                val y0 = WorkSplitter.partition(y, height, tiles)
                val y1 = WorkSplitter.partition(y + 1, height, tiles)
                val dy = y1 - y0
                GFX.addGPUTask("IntImage", width, dy) {
                    if (!isDestroyed) {
                        wasCreated = true // reset to true state
                        dataI.position(y0 * width)
                        dataI.limit(y1 * width)
                        overridePartially(
                            dataI, 0, 0, y0, width, dy,
                            uploadingType
                        )
                        // mark as non-finished again, if we're not done yet
                        if (y1 < height) wasCreated = false
                        else callback.call(this, null)
                    }
                    if (y1 == height) bufferPool.returnBuffer(data1)
                }
            }
        } else {
            GFX.addGPUTask("IntImage", width, height) {
                if (!isDestroyed) {
                    create(creationType, uploadingType, dataI)
                    callback.call(this, null)
                } else LOGGER.warn("Image was already destroyed")
                bufferPool.returnBuffer(data1)
            }
        }
    }

    fun createMonochrome(data: ByteBuffer, checkRedundancy: Boolean) {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancyMonochrome(data)
        upload(TargetType.UInt8x1, data)
        bufferPool.returnBuffer(data)
        afterUpload(false, 1, 1)
    }

    fun createRG(data: ByteArray, checkRedundancy: Boolean) {
        beforeUpload(2, data.size)
        val data2 = if (checkRedundancy) checkRedundancyRG(data) else data
        val buffer = bufferPool[data.size, false, false]
        buffer.put(data2).flip()
        upload(GL_RG, GL_RG, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 2, 2)
    }

    fun createRG(data: FloatArray, checkRedundancy: Boolean) {
        beforeUpload(2, data.size)
        val data2 = if (checkRedundancy) checkRedundancyRG(data) else data
        val buffer = bufferPool[data.size, false, false]
        buffer.asFloatBuffer().put(data2)
        upload(GL_RG, GL_RG, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 8, 2)
    }

    fun createRG(data: ByteBuffer, checkRedundancy: Boolean) {
        beforeUpload(2, data.remaining())
        if (checkRedundancy) checkRedundancyRG(data)
        upload(GL_RG, GL_RG, GL_UNSIGNED_BYTE, data)
        bufferPool.returnBuffer(data)
        afterUpload(false, 2, 2)
    }

    /**
     * creates a monochrome float32 image on the GPU
     * used by SDF
     * */
    fun createMonochrome(data: FloatBuffer, checkRedundancy: Boolean) {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancyMonochrome(data)
        setWriteAlignment(4 * width)
        upload(GL_R32F, GL_RED, GL_FLOAT, data)
        afterUpload(true, 4, 1)
    }

    /**
     * creates a monochrome float32 image on the GPU
     * used by SDF
     * */
    fun createMonochrome(data: FloatArray, checkRedundancy: Boolean) {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancyMonochrome(data) else data
        setWriteAlignment(4 * width)
        upload(GL_R32F, GL_RED, GL_FLOAT, data2)
        afterUpload(true, 4, 1)
    }

    /**
     * creates a monochrome float16 image on the GPU
     * */
    fun createMonochromeFP16(data: FloatBuffer, checkRedundancy: Boolean) {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancyMonochrome(data)
        setWriteAlignment(4 * width)
        upload(GL_R16F, GL_RED, GL_FLOAT, data)
        afterUpload(true, 4, 1)
    }

    /**
     * creates a monochrome float16 image on the GPU
     * */
    fun createMonochromeFP16(data: ShortBuffer, checkRedundancy: Boolean) {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancyMonochrome(data)
        setWriteAlignment(2 * width)
        upload(GL_R16F, GL_RED, GL_HALF_FLOAT, data)
        afterUpload(true, 2, 1)
    }

    fun createBGR(data: ByteArray, checkRedundancy: Boolean) {
        beforeUpload(3, data.size)
        val data2 = if (checkRedundancy) checkRedundancyRGB(data) else data
        val buffer = bufferPool[data2.size, false, false]
        buffer.put(data2).flip()
        switchRGB2BGR3(buffer)
        upload(GL_RGBA8, GL_RGB, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 4, 3)
    }

    fun createBGR(data: ByteBuffer, checkRedundancy: Boolean) {
        beforeUpload(3, data.remaining())
        if (checkRedundancy) checkRedundancyRGB(data)
        switchRGB2BGR3(data)
        upload(GL_RGBA8, GL_RGB, GL_UNSIGNED_BYTE, data)
        bufferPool.returnBuffer(data)
        afterUpload(false, 4, 3)
    }

    fun createMonochrome(data: ByteArray, checkRedundancy: Boolean) {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancyMonochrome(data) else data
        val buffer = bufferPool[data2.size, false, false]
        buffer.put(data2).flip()
        upload(TargetType.UInt8x1, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 1, 1)
    }

    fun createRGBA(data: FloatArray, checkRedundancy: Boolean) {
        beforeUpload(4, data.size)
        val data2 = if (checkRedundancy && width * height > 1) checkRedundancyRGBA(data) else data
        val byteBuffer = bufferPool[data2.size * 4, false, false]
        byteBuffer.asFloatBuffer().put(data2)
        // rgba32f as internal format is extremely important... otherwise the value is cropped
        upload(TargetType.Float32x4, byteBuffer)
        bufferPool.returnBuffer(byteBuffer)
        afterUpload(true, 16, 4)
    }

    fun createRGBA(data: FloatBuffer, buffer: ByteBuffer, checkRedundancy: Boolean) {
        beforeUpload(4, data.capacity())
        if (checkRedundancy && width * height > 1) checkRedundancyRGBA(data)
        // rgba32f as internal format is extremely important... otherwise the value is cropped
        upload(TargetType.Float32x4, buffer)
        afterUpload(true, 16, 4)
    }

    fun createBGRA(data: ByteArray, checkRedundancy: Boolean) {
        checkSize(4, data.size)
        val data2 = if (checkRedundancy) checkRedundancy(data) else data
        val buffer = bufferPool[data2.size, false, false]
        buffer.put(data2).flip()
        beforeUpload(4, buffer.remaining())
        switchRGB2BGR4(buffer)
        upload(GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 4, 4)
    }

    fun createBGRA(buffer: ByteBuffer, checkRedundancy: Boolean) {
        if (checkRedundancy) checkRedundancy(buffer)
        beforeUpload(4, buffer.remaining())
        switchRGB2BGR4(buffer)
        upload(GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        bufferPool.returnBuffer(buffer)
        afterUpload(false, 4, 4)
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
        for (i in 0 until width * height * 4 step 4) {
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
        upload(TargetType.UInt8x4, data)
        bufferPool.returnBuffer(data)
        afterUpload(false, 4, 4)
    }

    fun createRGB(data: ByteBuffer, checkRedundancy: Boolean) {
        beforeUpload(3, data.remaining())
        if (checkRedundancy) checkRedundancy(data, true)
        // texImage2D(TargetType.UByteTarget3, buffer)
        setWriteAlignment(3 * width)
        upload(GL_RGBA8, GL_RGB, GL_UNSIGNED_BYTE, data)
        bufferPool.returnBuffer(data)
        afterUpload(false, 4, 3)
    }

    fun createDepth(lowQuality: Boolean = false) {
        create(if (lowQuality) TargetType.DEPTH16 else TargetType.DEPTH32F)
    }

    /**
     * texture must be bound!
     * */
    fun ensureFilterAndClamping(nearest: Filtering, clamping: Clamping) {
        // ensure being bound?
        if (nearest != this.filtering) filtering(nearest)
        if (clamping != this.clamping) clamping(clamping)
    }

    private fun clamping(clamping: Clamping) {
        if (!withMultisampling && this.clamping != clamping) {
            TextureHelper.clamping(target, clamping.mode, border)
            this.clamping = clamping
        }
    }

    var autoUpdateMipmaps = true

    private fun filtering(filtering: Filtering) {
        if (withMultisampling) {
            this.filtering = Filtering.TRULY_NEAREST
            // multisample textures only support nearest filtering;
            // they don't accept the command to be what they are either
            return
        }
        if (!hasMipmap && filtering.needsMipmap && (width > 1 || height > 1)) {
            val t0 = Time.nanoTime
            glGenerateMipmap(target)
            // MipmapCalculator.generateMipmaps(this)
            val t1 = Time.nanoTime
            if (t1 - t0 > MILLIS_TO_NANOS) {
                LOGGER.warn("glGenerateMipmap took ${((t1 - t0).toFloat() / MILLIS_TO_NANOS).f1()} ms for $width x $height")
            }
            hasMipmap = true
            if (GFX.supportsAnisotropicFiltering) {
                val anisotropy = GFX.anisotropy
                glTexParameteri(target, GL_TEXTURE_LOD_BIAS, 0)
                glTexParameterf(target, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy)
            }
            // whenever the base mipmap is changed, the mipmaps will be updated :)
            // todo it seems like this needs to be called manually in WebGL
            glTexParameteri(target, GL14.GL_GENERATE_MIPMAP, if (autoUpdateMipmaps) GL_TRUE else GL_FALSE)
        }
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, filtering.min)
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, filtering.mag)
        // todo only set this, if it is a depth texture
        this.filtering = filtering
    }

    override var depthFunc: DepthMode? = null
        set(value) {
            if (field != value) {
                field = value
                if (GFX.supportsDepthTextures) {
                    bindBeforeUpload()
                    val mode = if (value == null) GL_NONE else GL_COMPARE_REF_TO_TEXTURE
                    glTexParameteri(target, GL_TEXTURE_COMPARE_MODE, mode)
                    if (value != null) glTexParameteri(target, GL_TEXTURE_COMPARE_FUNC, value.id)
                } // else we can't use them properly anyway, because they are not supported
            }
        }

    var hasMipmap = false

    fun bindBeforeUpload() {
        if (pointer == 0) throw RuntimeException("Pointer must be defined")
        boundTextures[boundTextureSlot] = 0
        bindTexture(target, pointer)
    }

    override fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean {
        checkSession()
        if (pointer != 0 && wasCreated) {
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
        } else throw IllegalStateException(
            if (isDestroyed) "Cannot bind destroyed texture!, $name"
            else "Cannot bind non-created texture!, $name"
        )
    }

    override fun destroy() {
        wasCreated = false
        isDestroyed = true
        val pointer = pointer
        if (pointer != 0) {
            if (Build.isDebug) synchronized(DebugGPUStorage.tex2d) {
                DebugGPUStorage.tex2d.remove(this)
            }
            synchronized(texturesToDelete) {
                // allocation counter is removed a bit early, shouldn't be too bad
                locallyAllocated = allocate(locallyAllocated, 0L)
                texturesToDelete.add(pointer)
            }
        }
        this.pointer = 0
    }

    private fun checkSize(channels: Int, size: Int) {
        if (size < width * height * channels) throw IllegalArgumentException("Incorrect size, $width*$height*$channels vs ${size}!")
        if (size > width * height * channels) LOGGER.warn("$size != $width*$height*$channels")
    }

    fun reset() {
        isDestroyed = false
    }

    fun isBoundToSlot(slot: Int): Boolean {
        return slot in boundTextures.indices && boundTextures[slot] == pointer
    }

    override fun wrapAsFramebuffer(): IFramebuffer {
        return object : IFramebuffer {
            override val name: String get() = this@Texture2D.name
            override val pointer: Int get() = -1
            override val width: Int get() = this@Texture2D.width
            override val height: Int get() = this@Texture2D.height
            override val samples: Int get() = this@Texture2D.samples
            override val numTextures: Int get() = 1
            override fun ensure() {}
            override fun checkSession() = this@Texture2D.checkSession()
            override fun getTargetType(slot: Int) = throw NotImplementedError()
            override fun bindDirectly() = throw NotImplementedError()
            override fun bindDirectly(w: Int, h: Int) = throw NotImplementedError()
            override fun destroy() = this@Texture2D.destroy()

            override fun attachFramebufferToDepth(name: String, targetCount: Int, fpTargets: Boolean) =
                throw NotImplementedError()

            override fun attachFramebufferToDepth(name: String, targets: Array<TargetType>) =
                throw NotImplementedError()

            override fun bindTextureI(index: Int, offset: Int, nearest: Filtering, clamping: Clamping) {
                if (offset == 0) this@Texture2D.bind(index, nearest, clamping)
                else throw IndexOutOfBoundsException()
            }

            override fun bindTextures(offset: Int, nearest: Filtering, clamping: Clamping) {
                this@Texture2D.bind(0, nearest, clamping)
            }

            override fun getTextureI(index: Int) =
                if (index == 0) this@Texture2D else throw IndexOutOfBoundsException()

            override val depthTexture = null
        }
    }

    override fun createImage(flipY: Boolean, withAlpha: Boolean): IntImage {
        val buffer = IntArray(width * height)
        useFrame(this, 0) {
            glFlush()
            glFinish()
            glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        }
        switchRGB2BGR(buffer)
        val image = IntImage(width, height, buffer, channels > 3)
        if (flipY) image.flipY()
        return image
    }

    companion object {

        @JvmField
        var alwaysBindTexture = false

        @JvmField
        var wasModifiedInComputePipeline = false

        @JvmStatic
        fun BufferedImage.hasAlphaChannel() = colorModel.hasAlpha()

        @JvmField
        val bufferPool = ByteBufferPool(64)

        @JvmField
        val byteArrayPool = ByteArrayPool(64)

        @JvmField
        val intArrayPool = IntArrayPool(64)

        @JvmField
        val floatArrayPool = FloatArrayPool(64)

        @JvmStatic
        fun freeUnusedEntries() {
            bufferPool.freeUnusedEntries()
            byteArrayPool.freeUnusedEntries()
            intArrayPool.freeUnusedEntries()
            floatArrayPool.freeUnusedEntries()
        }

        @JvmStatic
        @Docs("Method for debugging by unloading all textures")
        fun unbindAllTextures() {
            for (i in maxBoundTextures - 1 downTo 0) {
                whiteTexture.bind(i)
            }
        }

        @JvmStatic
        @Docs("Remove all pooled entries")
        fun gc() {
            bufferPool.gc()
            byteArrayPool.gc()
            intArrayPool.gc()
            floatArrayPool.gc()
        }

        @JvmField
        var allocated = 0L

        @JvmStatic
        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated += newValue - oldValue
            return newValue
        }

        @JvmField
        var boundTextureSlot = 0

        @JvmField
        val boundTextures = IntArray(64)

        init {
            boundTextures.fill(-1)
        }

        @JvmField
        val tmp4i = IntArray(4)

        @JvmField
        val tmp4f = FloatArray(4)

        @JvmStatic
        fun invalidateBinding() {
            boundTextureSlot = -1
            activeSlot(0)
            boundTextures.fill(-1)
        }

        @JvmStatic
        fun activeSlot(index: Int) {
            if (index < 0 || index >= maxBoundTextures)
                throw IllegalArgumentException("Texture index $index out of allowed bounds")
            if (alwaysBindTexture || index != boundTextureSlot) {
                glActiveTexture(GL_TEXTURE0 + index)
                boundTextureSlot = index
            }
        }

        /**
         * bind the texture, the slot doesn't matter
         * @return whether the texture was actively bound
         * */
        @JvmStatic
        fun bindTexture(mode: Int, pointer: Int): Boolean {
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

        @JvmStatic
        private val LOGGER = LogManager.getLogger(Texture2D::class)

        @JvmField
        val textureBudgetTotal = DefaultConfig["gpu.textureBudget", 1_000_000L]

        @JvmField
        var textureBudgetUsed = 0L

        @JvmField
        val texturesToDelete = ArrayList<Int>()

        @JvmStatic
        fun resetBudget() {
            textureBudgetUsed = 0L
        }

        // val isLittleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN

        @JvmStatic
        private var creationSession = -1

        @JvmStatic
        private var creationIndex = 0

        @JvmStatic
        private val creationIndices = IntArray(16)

        @JvmStatic
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

        @JvmStatic
        fun switchRGB2BGR(values: IntArray) {
            // convert argb to abgr
            for (i in values.indices) {
                values[i] = convertABGR2ARGB(values[i])
            }
        }

        @JvmStatic
        fun switchRGB2BGR(values: IntBuffer) {
            // convert argb to abgr
            for (i in 0 until values.limit()) {
                values.put(i, convertABGR2ARGB(values[i]))
            }
        }

        @JvmStatic
        fun switchRGB2BGR3(values: ByteBuffer) {
            // convert rgb to bgr
            val pos = values.position()
            for (i in pos until pos + values.remaining() step 3) {
                val tmp = values[i]
                values.put(i, values[i + 2])
                values.put(i + 2, tmp)
            }
        }

        @JvmStatic
        fun switchRGB2BGR4(values: ByteBuffer) {
            // convert rgba to bgra
            val pos = values.position()
            for (i in pos until pos + values.remaining() step 4) {
                val tmp = values[i]
                values.put(i, values[i + 2])
                values.put(i + 2, tmp)
            }
        }

        @JvmStatic
        fun destroyTextures() {
            if (texturesToDelete.isNotEmpty()) {
                unbindAllTextures()
                synchronized(texturesToDelete) {
                    // unbind old textures
                    glDeleteTextures(texturesToDelete.toIntArray())
                    texturesToDelete.clear()
                }
            }
        }

        @JvmStatic
        private fun getAlignment(w: Int): Int {
            return when {
                w and 7 == 0 -> 8
                w and 3 == 0 -> 4
                w and 1 == 0 -> 2
                else -> 1
            }
        }

        @JvmStatic
        fun setReadAlignment(w: Int) {
            glPixelStorei(GL_PACK_ALIGNMENT, getAlignment(w))
        }

        @JvmStatic
        fun setWriteAlignment(w: Int) {
            glPixelStorei(GL_UNPACK_ALIGNMENT, getAlignment(w))
        }

        fun getNumChannels(format: Int): Int {
            return when (format) {
                0 -> 0
                GL_R8, GL_R8I, GL_R8UI, GL_R16F, GL_R16I, GL_R16UI, GL_R32F, GL_R32I, GL_R32UI -> 1
                GL_RG8, GL_RG8I, GL_RG8UI, GL_RG16F, GL_RG16I, GL_RG16UI, GL_RG32F, GL_RG32I, GL_RG32UI -> 2
                GL_RGB8, GL_RGB8I, GL_RGB8UI, GL_RGB16F, GL_RGB16I, GL_RGB16UI, GL_RGB32F, GL_RGB32I, GL_RGB32UI -> 3
                else -> 4
            }
        }

        fun getNumberType(format: Int): Int {
            return when (format) {
                GL_R8, GL_RG8, GL_RGB8, GL_RGBA8 -> GL_UNSIGNED_BYTE.inv()
                GL_R8UI, GL_RG8UI, GL_RGB8UI, GL_RGBA8UI -> GL_UNSIGNED_BYTE
                GL_R8I, GL_RG8I, GL_RGB8I, GL_RGBA8I -> GL_BYTE
                GL_R16, GL_RG16, GL_RGB16, GL_RGBA16 -> GL_UNSIGNED_SHORT.inv()
                GL_R16UI, GL_RG16UI, GL_RGB16UI, GL_RGBA16UI -> GL_UNSIGNED_SHORT
                GL_R16I, GL_RG16I, GL_RGB16I, GL_RGBA16I -> GL_SHORT
                GL_R32I, GL_RG32I, GL_RGB32I, GL_RGBA32I -> GL_INT
                GL_R32UI, GL_RG32UI, GL_RGB32UI, GL_RGBA32UI -> GL_UNSIGNED_INT
                GL_R16F, GL_RG16F, GL_RGB16F, GL_RGBA16F -> GL_HALF_FLOAT
                GL_R32F, GL_RG32F, GL_RGB32F, GL_RGBA32F -> GL_FLOAT
                else -> 0
            }
        }
    }
}