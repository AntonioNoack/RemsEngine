package me.anno.gpu.texture

import me.anno.Build
import me.anno.Time
import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.Docs
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.check
import me.anno.gpu.GFX.isGFXThread
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.GFX.maxBoundTextures
import me.anno.gpu.GFXState
import me.anno.gpu.GLNames
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.buffer.OpenGLBuffer.Companion.bindBuffer
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Redundancy.checkRedundancyX1
import me.anno.gpu.texture.Redundancy.checkRedundancyX2
import me.anno.gpu.texture.Redundancy.checkRedundancyX3
import me.anno.gpu.texture.Redundancy.checkRedundancyX4
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.Image
import me.anno.image.ImageTransform
import me.anno.image.raw.FloatImage
import me.anno.image.raw.GPUImage
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.clamp
import me.anno.utils.Color.convertARGB2ABGR
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.Callback
import me.anno.utils.hpc.WorkSplitter
import me.anno.utils.pooling.Pools
import me.anno.utils.types.Floats.f1
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL46C.GL_BGR
import org.lwjgl.opengl.GL46C.GL_BGRA
import org.lwjgl.opengl.GL46C.GL_BYTE
import org.lwjgl.opengl.GL46C.GL_COMPARE_REF_TO_TEXTURE
import org.lwjgl.opengl.GL46C.GL_DOUBLE
import org.lwjgl.opengl.GL46C.GL_FLOAT
import org.lwjgl.opengl.GL46C.GL_HALF_FLOAT
import org.lwjgl.opengl.GL46C.GL_INT
import org.lwjgl.opengl.GL46C.GL_NONE
import org.lwjgl.opengl.GL46C.GL_PACK_ALIGNMENT
import org.lwjgl.opengl.GL46C.GL_PIXEL_UNPACK_BUFFER
import org.lwjgl.opengl.GL46C.GL_R16F
import org.lwjgl.opengl.GL46C.GL_R32F
import org.lwjgl.opengl.GL46C.GL_RED
import org.lwjgl.opengl.GL46C.GL_RED_INTEGER
import org.lwjgl.opengl.GL46C.GL_RG
import org.lwjgl.opengl.GL46C.GL_RGB
import org.lwjgl.opengl.GL46C.GL_RGB32F
import org.lwjgl.opengl.GL46C.GL_RGBA
import org.lwjgl.opengl.GL46C.GL_RGBA8
import org.lwjgl.opengl.GL46C.GL_RGBA_INTEGER
import org.lwjgl.opengl.GL46C.GL_RGB_INTEGER
import org.lwjgl.opengl.GL46C.GL_RG_INTEGER
import org.lwjgl.opengl.GL46C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT
import org.lwjgl.opengl.GL46C.GL_SHORT
import org.lwjgl.opengl.GL46C.GL_TEXTURE
import org.lwjgl.opengl.GL46C.GL_TEXTURE0
import org.lwjgl.opengl.GL46C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL46C.GL_TEXTURE_2D_MULTISAMPLE
import org.lwjgl.opengl.GL46C.GL_TEXTURE_COMPARE_FUNC
import org.lwjgl.opengl.GL46C.GL_TEXTURE_COMPARE_MODE
import org.lwjgl.opengl.GL46C.GL_TEXTURE_LOD_BIAS
import org.lwjgl.opengl.GL46C.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL46C.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL46C.GL_UNPACK_ALIGNMENT
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_SHORT
import org.lwjgl.opengl.GL46C.glActiveTexture
import org.lwjgl.opengl.GL46C.glBindTexture
import org.lwjgl.opengl.GL46C.glDeleteTextures
import org.lwjgl.opengl.GL46C.glGenTextures
import org.lwjgl.opengl.GL46C.glGenerateMipmap
import org.lwjgl.opengl.GL46C.glGetTexImage
import org.lwjgl.opengl.GL46C.glMemoryBarrier
import org.lwjgl.opengl.GL46C.glObjectLabel
import org.lwjgl.opengl.GL46C.glPixelStorei
import org.lwjgl.opengl.GL46C.glTexImage2D
import org.lwjgl.opengl.GL46C.glTexImage2DMultisample
import org.lwjgl.opengl.GL46C.glTexParameterf
import org.lwjgl.opengl.GL46C.glTexParameteri
import org.lwjgl.opengl.GL46C.glTexSubImage2D
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.DoubleBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicLong

open class Texture2D(
    override val name: String,
    final override var width: Int,
    final override var height: Int,
    samples: Int
) : ITexture2D {

    constructor(img: Image, checkRedundancy: Boolean) : this("img", img, checkRedundancy)
    constructor(name: String, img: Image, checkRedundancy: Boolean) : this(name, img.width, img.height, 1) {
        create(img, true, checkRedundancy) { _, _ -> filtering(Filtering.NEAREST) }
    }

    override val samples = clamp(samples, 1, GFX.maxSamples)
    var owner: Framebuffer? = null

    var border = 0

    override var channels: Int = 0

    override fun toString() =
        "Texture2D(\"$name\"@$pointer, $width x $height x $samples, ${GLNames.getName(internalFormat)})"

    /**
     * Pseudo-Reference, such that ImageGPUCache[ref] = this;
     * Only is valid as long as the texture is created ofc.
     *
     * If you need this property for other Texture classes or similar, write me :).
     * */
    var ref: FileReference = InvalidRef
        private set
        get() {
            val numChannels = TextureHelper.getNumChannels(internalFormat)
            val hasAlphaChannel = numChannels == 4
            if (field == InvalidRef) field = GPUImage(this, numChannels, hasAlphaChannel).ref
            return field
        }

    private val withMultisampling = samples > 1

    val target = if (withMultisampling) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D
    val state get(): Int = if (isCreated()) pointer else 0

    var pointer = 0
    var session = 0

    override fun checkSession() {
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

    override var locallyAllocated = 0L
    override var internalFormat = 0

    var createdW = 0
    var createdH = 0

    override var isHDR = false

    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun ensurePointer() {
        checkSession()
        assertFalse(isDestroyed, "Texture was destroyed")
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
                RuntimeException("Unknown data format ${GLNames.getName(dataFormat)}")
                    .printStackTrace()
                1
            }
        }
        setWriteAlignment(w * typeSize * numChannels)
        if (unbind) unbindUnpackBuffer()
    }

    fun upload(internalFormat: Int, dataFormat: Int, dataType: Int, data: Any?, unbind: Boolean = true) {
        if (data is ByteArray) { // helper
            val tmp = Pools.byteBufferPool.createBuffer(data.size)
            tmp.put(data).flip()
            upload(internalFormat, dataFormat, dataType, tmp, unbind)
            Pools.byteBufferPool.returnBuffer(tmp)
            return
        }
        bindBeforeUpload()
        val w = width
        val h = height
        val target = target
        assertTrue(w > 0 && h > 0) { "Cannot create empty texture, $w x $h" }
        check()
        if (createdW == w && createdH == h && data != null && !withMultisampling) {
            uploadPartial(target, w, h, dataFormat, dataType, unbind, data)
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
                uploadFull(target, internalFormat, w, h, dataFormat, dataType, unbind, data)
                check()
            }
            this.internalFormat = internalFormat
            createdW = w
            createdH = h
        }
    }

    private fun uploadPartial(
        target: Int,
        w: Int, h: Int, dataFormat: Int, dataType: Int,
        unbind: Boolean, data: Any
    ) {
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
    }

    private fun uploadFull(
        target: Int, internalFormat: Int,
        w: Int, h: Int, dataFormat: Int, dataType: Int,
        unbind: Boolean, data: Any?
    ) {
        // extracted into a separate function, so we get a little more space horizontally (2 tabs)
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
            null -> glTexImage2D(target, 0, internalFormat, w, h, 0, dataFormat, dataType, null as ByteBuffer?)
            else -> throw IllegalArgumentException("${data::class.simpleName} is not supported")
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
        check()
        ensurePointer()
        check()
        bindBeforeUpload()
        check()
        setAlignmentAndBuffer(w, dataFormat, dataType, unbind)
        check()
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
        check()
    }

    fun upload(type: TargetType, data: ByteBuffer?) {
        upload(type.internalFormat, type.uploadFormat, type.fillType, data)
    }

    fun upload(type: TargetType, data: ByteArray?) {
        upload(type.internalFormat, type.uploadFormat, type.fillType, data)
    }

    fun create(type: TargetType): Texture2D {
        beforeUpload(0, 0)
        upload(type, null as ByteArray?)
        afterUpload(type.isHDR, type.bytesPerPixel, type.channels)
        return this
    }

    fun create(type: TargetType, data: Any?): Texture2D {
        return create(type, type, data)
    }

    fun create(creationType: TargetType, uploadType: TargetType, data: Any?): Texture2D {
        beforeUpload(0, 0)
        upload(creationType.internalFormat, uploadType.uploadFormat, uploadType.fillType, data)
        afterUpload(creationType.isHDR, creationType.bytesPerPixel, uploadType.channels)
        return this
    }

    fun create(image: Image, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        width = image.width
        height = image.height
        assertFalse(isDestroyed) { "Texture $name must be reset first" }
        if (!isGFXThread() || (!requestBudget(width * height) && !loadTexturesSync.peek())) {
            create(image, false, checkRedundancy, callback)
        } else {
            image.createTexture(this, true, checkRedundancy, callback)
        }
    }

    fun setSize1x1() {
        // a warning, because you might not expect your image to be empty, and wonder why its size is 1x1
        LOGGER.warn("Reduced \"$name\" from $width x $height to 1 x 1, because it was mono-colored")
        width = 1
        height = 1
    }

    fun create(image: Image, sync: Boolean, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        image.createTexture(this, sync, checkRedundancy, callback)
    }

    fun overridePartially(data: Any, level: Int, x: Int, y: Int, w: Int, h: Int, type: TargetType) {
        uploadPartially(level, x, y, w, h, type.uploadFormat, type.fillType, data)
        check()
    }

    fun beforeUpload(channels: Int, size: Int) {
        assertFalse(isDestroyed, "Texture is already destroyed, call reset() if you want to stream it")
        checkSize(channels, size)
        check()
        ensurePointer()
        bindBeforeUpload()
    }

    fun afterUpload(isHDR: Boolean, bytesPerPixel: Int, channels: Int) {
        locallyAllocated = allocate(locallyAllocated, width * height * bytesPerPixel.toLong())
        wasCreated = true
        hasMipmap = false
        this.isHDR = isHDR
        this.channels = channels
        check()
        filtering(filtering)
        check()
        clamping(clamping, true)
        check()
        if (Build.isDebug) glObjectLabel(GL_TEXTURE, pointer, name)
        if (isDestroyed) destroy()
    }

    fun createBGRA(data: IntArray, checkRedundancy: Boolean): Texture2D {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancyX4(data) else data
        convertARGB2ABGR(data2)
        setWriteAlignment(4 * width)
        upload(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, data2)
        afterUpload(false, 4, 4)
        convertARGB2ABGR(data2)
        return this
    }

    fun createBGR(data: IntArray, checkRedundancy: Boolean): Texture2D {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancyX4(data) else data
        convertARGB2ABGR(data2)
        setWriteAlignment(4 * width)
        upload(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, data2)
        afterUpload(false, 4, 3)
        convertARGB2ABGR(data2)
        return this
    }

    fun createRGB(data: FloatArray, checkRedundancy: Boolean): Texture2D {
        beforeUpload(3, data.size)
        val floats2 = if (checkRedundancy) checkRedundancyX3(data) else data
        setWriteAlignment(12 * width)
        upload(GL_RGB32F, GL_RGB, GL_FLOAT, floats2)
        afterUpload(true, 12, 3)
        return this
    }

    fun createRGBA(data: IntBuffer, checkRedundancy: Boolean): Texture2D {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancyX4(data)
        assertEquals(ByteOrder.nativeOrder(), data.order(), "Byte order must be native!")
        setWriteAlignment(4 * width)
        upload(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(false, 4, 4)
        return this
    }

    /**
     * warning: will assume RGBA colors, but the engine works with ARGB internally!
     * */
    fun createRGBA(data: IntArray, checkRedundancy: Boolean): Texture2D {
        beforeUpload(1, data.size)
        if (checkRedundancy) checkRedundancyX4(data)
        setWriteAlignment(4 * width)
        upload(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, data)
        afterUpload(false, 4, 4)
        return this
    }

    /**
     * warning: will assume RGBA colors, but the engine works with ARGB internally!
     * */
    fun createRGB(data: IntArray, checkRedundancy: Boolean): Texture2D {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancyX4(data) else data
        setWriteAlignment(4 * width)
        upload(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, data2)
        afterUpload(false, 4, 3)
        return this
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
            addGPUTask("IntImage.createTiled[0]", width, height) {
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
                addGPUTask("IntImage.createTiled[$y/$tiles]", width, dy) {
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
                    if (y1 == height) Pools.byteBufferPool.returnBuffer(data1)
                }
            }
        } else {
            addGPUTask("IntImage.create", width, height) {
                if (!isDestroyed) {
                    create(creationType, uploadingType, dataI)
                    callback.call(this, null)
                } else LOGGER.warn("Image was already destroyed")
                Pools.byteBufferPool.returnBuffer(data1)
            }
        }
    }

    fun createMonochrome(data: ByteBuffer, checkRedundancy: Boolean): Texture2D {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancyX1(data)
        upload(TargetType.UInt8x1, data)
        Pools.byteBufferPool.returnBuffer(data)
        afterUpload(false, 1, 1)
        return this
    }

    fun createRG(data: ByteBuffer, checkRedundancy: Boolean): Texture2D {
        beforeUpload(2, data.remaining())
        if (checkRedundancy) checkRedundancyX2(data)
        upload(GL_RG, GL_RG, GL_UNSIGNED_BYTE, data)
        Pools.byteBufferPool.returnBuffer(data)
        afterUpload(false, 2, 2)
        return this
    }

    /**
     * creates a monochrome float32 image on the GPU
     * used by SDF
     * */
    fun createMonochrome(data: FloatBuffer, checkRedundancy: Boolean): Texture2D {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancyX1(data)
        setWriteAlignment(4 * width)
        upload(GL_R32F, GL_RED, GL_FLOAT, data)
        afterUpload(true, 4, 1)
        return this
    }

    /**
     * creates a monochrome float32 image on the GPU
     * used by SDF
     * */
    fun createMonochrome(data: FloatArray, checkRedundancy: Boolean): Texture2D {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancyX1(data) else data
        setWriteAlignment(4 * width)
        upload(GL_R32F, GL_RED, GL_FLOAT, data2)
        afterUpload(true, 4, 1)
        return this
    }

    /**
     * creates a monochrome float16 image on the GPU
     * */
    fun createMonochromeFP16(data: FloatBuffer, checkRedundancy: Boolean): Texture2D {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancyX1(data)
        setWriteAlignment(4 * width)
        upload(GL_R16F, GL_RED, GL_FLOAT, data)
        afterUpload(true, 4, 1)
        return this
    }

    /**
     * creates a monochrome float16 image on the GPU
     * */
    fun createMonochromeFP16(data: ShortBuffer, checkRedundancy: Boolean): Texture2D {
        beforeUpload(1, data.remaining())
        if (checkRedundancy) checkRedundancyX1(data)
        setWriteAlignment(2 * width)
        upload(GL_R16F, GL_RED, GL_HALF_FLOAT, data)
        afterUpload(true, 2, 1)
        return this
    }

    fun createBGR(data: ByteBuffer, checkRedundancy: Boolean): Texture2D {
        beforeUpload(3, data.remaining())
        if (checkRedundancy) checkRedundancyX3(data)
        convertRGB2BGR3(data)
        setWriteAlignment(3 * width)
        upload(GL_RGBA8, GL_RGB, GL_UNSIGNED_BYTE, data)
        Pools.byteBufferPool.returnBuffer(data)
        afterUpload(false, 4, 3)
        return this
    }

    fun createMonochrome(data: ByteArray, checkRedundancy: Boolean): Texture2D {
        beforeUpload(1, data.size)
        val data2 = if (checkRedundancy) checkRedundancyX1(data) else data
        val buffer = Pools.byteBufferPool[data2.size, false, false]
        buffer.put(data2).flip()
        upload(TargetType.UInt8x1, buffer)
        Pools.byteBufferPool.returnBuffer(buffer)
        afterUpload(false, 1, 1)
        return this
    }

    fun createRGBA(data: FloatArray, checkRedundancy: Boolean): Texture2D {
        beforeUpload(4, data.size)
        val data2 = if (checkRedundancy && width * height > 1) checkRedundancyX4(data) else data
        val byteBuffer = Pools.byteBufferPool[data2.size * 4, false, false]
        byteBuffer.asFloatBuffer().put(data2)
        // rgba32f as internal format is extremely important... otherwise the value is cropped
        upload(TargetType.Float32x4, byteBuffer)
        Pools.byteBufferPool.returnBuffer(byteBuffer)
        afterUpload(true, 16, 4)
        return this
    }

    fun createRGBA(data: FloatBuffer, buffer: ByteBuffer, checkRedundancy: Boolean): Texture2D {
        beforeUpload(4, data.capacity())
        if (checkRedundancy && width * height > 1) checkRedundancyX4(data)
        // rgba32f as internal format is extremely important... otherwise the value is cropped
        upload(TargetType.Float32x4, buffer)
        afterUpload(true, 16, 4)
        return this
    }

    fun createBGRA(buffer: ByteBuffer, checkRedundancy: Boolean): Texture2D {
        if (checkRedundancy) checkRedundancyX4(buffer)
        beforeUpload(4, buffer.remaining())
        convertRGB2BGR4(buffer)
        upload(GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        Pools.byteBufferPool.returnBuffer(buffer)
        afterUpload(false, 4, 4)
        return this
    }

    fun createRGBA(data: ByteArray, checkRedundancy: Boolean): Texture2D {
        checkSize(4, data.size)
        val data2 = if (checkRedundancy) checkRedundancyX4(data) else data
        val buffer = Pools.byteBufferPool[data2.size, false, false]
        buffer.put(data2).flip()
        return createRGBA(buffer, false)
    }

    fun createARGB(data: ByteBuffer, checkRedundancy: Boolean): Texture2D {
        checkSize(4, data.remaining())
        if (checkRedundancy) checkRedundancyX4(data)
        for (i in 0 until width * height * 4 step 4) {
            val tmp = data[i]
            data.put(data[i + 1]) // r
            data.put(data[i + 2]) // g
            data.put(data[i + 3]) // b
            data.put(tmp) // a
        }
        data.flip()
        return createRGBA(data, false)
    }

    /** creates the texture, and returns the buffer */
    fun createRGBA(data: ByteBuffer, checkRedundancy: Boolean): Texture2D {
        beforeUpload(4, data.remaining())
        if (checkRedundancy) checkRedundancyX4(data, false)
        upload(TargetType.UInt8x4, data)
        Pools.byteBufferPool.returnBuffer(data)
        afterUpload(false, 4, 4)
        return this
    }

    fun createRGB(data: ByteBuffer, checkRedundancy: Boolean): Texture2D {
        beforeUpload(3, data.remaining())
        if (checkRedundancy) checkRedundancyX3(data)
        setWriteAlignment(3 * width)
        upload(GL_RGBA8, GL_RGB, GL_UNSIGNED_BYTE, data)
        Pools.byteBufferPool.returnBuffer(data)
        afterUpload(false, 4, 3)
        return this
    }

    /**
     * texture must be bound!
     * */
    fun ensureFilterAndClamping(nearest: Filtering, clamping: Clamping) {
        // ensure being bound?
        if (nearest != this.filtering) filtering(nearest)
        if (clamping != this.clamping) clamping(clamping, false)
    }

    private fun clamping(clamping: Clamping, force: Boolean) {
        if (!withMultisampling && (this.clamping != clamping || force)) {
            TextureHelper.clamping(target, clamping.mode, border)
            this.clamping = clamping
        }
    }

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
                val dt = ((t1 - t0).toFloat() / MILLIS_TO_NANOS)
                LOGGER.warn("glGenerateMipmap took {} ms for {}", dt.f1(), this)
            }
            hasMipmap = true
            if (GFX.supportsAnisotropicFiltering) {
                val anisotropy = GFX.anisotropy
                glTexParameteri(target, GL_TEXTURE_LOD_BIAS, 0)
                glTexParameterf(target, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy)
            }
        }
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, filtering.min)
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, filtering.mag)
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
        assertNotEquals(0, pointer, "Pointer must be defined")
        boundTextures[boundTextureSlot] = 0
        bindTexture(target, pointer)
    }

    override fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean {
        checkSession()
        assertFalse(isDestroyed, "Cannot bind destroyed texture!")
        assertNotEquals(0, pointer, "Cannot bind non-created texture!")
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

    override fun createImage(flipY: Boolean, withAlpha: Boolean): IntImage {
        bindBeforeUpload()
        val buffer = IntArray(width * height)
        glGetTexImage(target, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        convertARGB2ABGR(buffer)
        val image = IntImage(width, height, buffer, withAlpha && channels > 3)
        if (flipY) image.flipY()
        return image
    }

    fun getFloatPixels(dst: FloatImage) {
        bindBeforeUpload()
        assertTrue(dst.data.size >= width * height * dst.numChannels)
        val nc1 = when (dst.numChannels) {
            1 -> GL_RED
            2 -> GL_RG
            3 -> GL_RGB
            4 -> GL_RGBA
            else -> throw IllegalArgumentException()
        }
        glGetTexImage(target, 0, nc1, GL_FLOAT, dst.data)
    }

    companion object {

        @JvmField
        var alwaysBindTexture = false

        @JvmField
        var wasModifiedInComputePipeline = false

        @JvmStatic
        @Docs("Method for debugging by unloading all textures")
        fun unbindAllTextures() {
            for (i in maxBoundTextures - 1 downTo 0) {
                whiteTexture.bind(i)
            }
        }

        @JvmField
        val allocated = AtomicLong()

        @JvmStatic
        fun allocate(oldValue: Long, newValue: Long): Long {
            allocated.addAndGet(newValue - oldValue)
            return newValue
        }

        @JvmField
        var boundTextureSlot = 0

        @JvmField
        val boundTextures = IntArray(64)

        @JvmField
        val boundTargets = IntArray(64)

        init {
            boundTextures.fill(-1)
        }

        fun requestBudget(requested: Int): Boolean {
            return requestBudget(requested.toLong())
        }

        fun requestBudget(requested: Long): Boolean {
            val requiredBudget = textureBudgetUsed + requested
            val total = textureBudgetTotal
            val totalHalf = total.shr(1)
            if (requiredBudget <= total || (requested > totalHalf && textureBudgetUsed < totalHalf)) {
                textureBudgetUsed = requiredBudget
                return true
            } else {
                return false
            }
        }

        fun getBindState(slot: Int): Long {
            return boundTargets[slot] + boundTextures[slot].toLong().shl(16)
        }

        fun restoreBindState(slot: Int, state: Long) {
            activeSlot(slot)
            val pointer = state.shr(16).toInt()
            if (pointer >= 0) bindTexture(state.toInt().and(0xffff), pointer)
        }

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
        fun bindTexture(target: Int, pointer: Int): Boolean {
            if (wasModifiedInComputePipeline) {
                glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
                wasModifiedInComputePipeline = false
            }
            return if (alwaysBindTexture || boundTextures[boundTextureSlot] != pointer) {
                boundTextures[boundTextureSlot] = pointer
                boundTargets[boundTextureSlot] = target
                glBindTexture(target, pointer)
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
        fun convertRGB2BGR3(values: ByteBuffer) {
            // convert rgb to bgr
            val pos = values.position()
            for (i in pos until pos + values.remaining() step 3) {
                val tmp = values[i]
                values.put(i, values[i + 2])
                values.put(i + 2, tmp)
            }
        }

        @JvmStatic
        fun convertRGB2BGR4(values: ByteBuffer) {
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
                // LOGGER.info("Deleting ${texturesToDelete.size} textures")
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
    }
}