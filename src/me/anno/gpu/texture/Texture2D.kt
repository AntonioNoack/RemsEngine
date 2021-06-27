package me.anno.gpu.texture

import me.anno.cache.data.ICacheData
import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.GFX.glThread
import me.anno.gpu.GFX.isGFXThread
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.framebuffer.TargetType
import me.anno.objects.modes.RotateJPEG
import me.anno.utils.Threads.threadWithName
import me.anno.utils.pooling.ByteArrayPool
import me.anno.utils.pooling.ByteBufferPool
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.GL_TEXTURE_2D_MULTISAMPLE
import org.lwjgl.opengl.GL32.glTexImage2DMultisample
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferInt
import java.nio.*

open class Texture2D(
    val name: String,
    override var w: Int,
    override var h: Int,
    val samples: Int
) : ICacheData, ITexture2D {

    constructor(img: BufferedImage) : this("img", img.width, img.height, 1) {
        create(img, true)
        filtering(GPUFiltering.NEAREST)
    }

    override fun toString() = "$name $w $h $samples"

    private val withMultisampling get() = samples > 1

    private val tex2D = if (withMultisampling) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D
    var state: Triple<Texture2D, Int, Boolean>? = null

    var pointer = -1

    var isCreated = false
    var isDestroyed = false

    var filtering = GPUFiltering.TRULY_NEAREST
    var clamping = Clamping.CLAMP

    // only used for images with exif rotation tag...
    var rotation: RotateJPEG? = null

    var locallyAllocated = 0L

    var createdW = 0
    var createdH = 0

    fun setSize(width: Int, height: Int) {
        w = width
        h = height
    }

    fun ensurePointer() {
        if (isDestroyed) throw RuntimeException("Destroyed")
        if (pointer < 0) {
            GFX.check()
            pointer = createTexture()
            state = Triple(this, pointer, isCreated)
            // many textures can be created by the console log and the fps viewer constantly xD
            // maybe we should use allocation free versions there xD
            GFX.check()
        }
        if (pointer <= 0) throw RuntimeException()
    }

    // todo is BGRA really faster than RGBA? we should try that and maybe use it...

    fun texImage2D(w: Int, h: Int, type: TargetType, data: ByteBuffer?) {
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
        ensurePointer()
        bindBeforeUpload()
        locallyAllocated = allocate(locallyAllocated, w * h * 4L)
        if (withMultisampling) {
            glTexImage2DMultisample(tex2D, samples, GL_RGBA8, w, h, false)
        } else texImage2D(w, h, TargetType.UByteTarget4, null)
        filtering(filtering)
        clamping(clamping)
        isCreated = true
        if (isDestroyed) destroy()
    }

    fun create(type: TargetType) {
        ensurePointer()
        bindBeforeUpload()
        locallyAllocated = allocate(locallyAllocated, w * h * type.bytesPerPixel.toLong())
        if (withMultisampling) {
            glTexImage2DMultisample(tex2D, samples, type.type0, w, h, false)
        } else texImage2D(w, h, type, null)
        filtering(filtering)
        clamping(clamping)
        isCreated = true
        GFX.check()
    }

    fun createFP32() {
        GFX.check()
        ensurePointer()
        bindBeforeUpload()
        GFX.check()
        locallyAllocated = allocate(locallyAllocated, w * h * 32L)
        if (withMultisampling) {
            glTexImage2DMultisample(tex2D, samples, GL_RGBA32F, w, h, false)
        } else texImage2D(w, h, TargetType.FloatTarget4, null)
        GFX.check()
        filtering(filtering)
        clamping(clamping)
        isCreated = true
        GFX.check()
    }

    fun create(name: String, createImage: () -> BufferedImage, forceSync: Boolean) {
        val requiredBudget = textureBudgetUsed + w * h
        if ((requiredBudget > textureBudgetTotal && !loadTexturesSync.peek()) || Thread.currentThread() != glThread) {
            if (forceSync) {
                GFX.addGPUTask(1000) {
                    create(createImage(), true)
                }
            } else {
                threadWithName("Create Image") {
                    create(createImage(), false)
                }
            }
        } else {
            textureBudgetUsed += requiredBudget
            create(createImage(), true)
        }
    }

    // todo this function is ok, but a later function is broken (@Text)
    fun create(img: BufferedImage, sync: Boolean) {
        w = img.width
        h = img.height
        isCreated = false
        val black = black
        val hasAlpha = img.colorModel.hasAlpha()
        val intData = when (val buffer = img.raster.dataBuffer) {
            is DataBufferByte -> {
                try {
                    val buffer2 = getBuffer(buffer.data)
                    // ensure it's opaque
                    if (!hasAlpha) {
                        for (i in 0 until w * h) buffer2.put(i * 4 + 3, -1)
                    }
                    if (sync) uploadData(buffer2)
                    else GFX.addGPUTask(w, h) {
                        uploadData(buffer2)
                    }
                    return
                } catch (e: Exception) {
                    img.getRGB(0, 0, w, h, null, 0, w)
                }
            }
            is DataBufferInt -> buffer.data
            else -> {// said to be slow; I indeed had lags in HomeDesigner
                img.getRGB(0, 0, w, h, null, 0, w)
            }
        }
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            if (hasAlpha) {
                for (i in intData.indices) {// argb -> abgr
                    val argb = intData[i]
                    val r = (argb and 0xff0000).shr(16)
                    val b = (argb and 0xff).shl(16)
                    val ag = argb and 0xff00ff00.toInt()
                    intData[i] = ag or r or b
                }
            } else {
                for (i in intData.indices) {// argb -> abgr
                    val argb = intData[i]
                    val r = (argb and 0xff0000).shr(16)
                    val b = (argb and 0xff).shl(16)
                    val g = argb and 0xff00
                    intData[i] = black or g or r or b
                }
            }
        } else {
            if (hasAlpha) {
                for (i in intData.indices) {// argb -> rgba
                    val argb = intData[i]
                    val a = argb.shr(24) and 255
                    val rgb = argb.and(0xffffff) shl 8
                    intData[i] = rgb or a
                }
            } else {
                for (i in intData.indices) {// argb -> rgba
                    val argb = intData[i]
                    val rgb = argb.and(0xffffff) shl 8
                    intData[i] = rgb or 0xff
                }
            }
        }
        if (sync && Thread.currentThread() == glThread) {
            uploadData(intData)
        } else GFX.addGPUTask(w, h) {
            uploadData(intData)
        }
    }

    fun getBuffer(bytes: ByteArray): ByteBuffer {
        val buffer = byteBufferPool[w * h * 4, false]
        when (bytes.size / (w * h)) {
            1 -> {
                for (i in 0 until w * h) {
                    val c = bytes[i]
                    buffer.put(c)
                    buffer.put(c)
                    buffer.put(c)
                    buffer.put(-1)
                }
            }
            3 -> {
                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                    for (i in 0 until w * h) {
                        buffer.put(bytes[i * 3 + 2])
                        buffer.put(bytes[i * 3 + 1])
                        buffer.put(bytes[i * 3])
                        buffer.put(-1)
                    }
                } else {
                    for (i in 0 until w * h) {
                        buffer.put(bytes[i * 3])
                        buffer.put(bytes[i * 3 + 1])
                        buffer.put(bytes[i * 3 + 2])
                        buffer.put(-1)
                    }
                }
            }
            4 -> {
                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                    for (i in 0 until w * h) {
                        buffer.put(bytes[i * 4 + 3])
                        buffer.put(bytes[i * 4 + 2])
                        buffer.put(bytes[i * 4 + 1])
                        buffer.put(bytes[i * 4])
                    }
                } else {
                    for (i in 0 until w * h) {
                        buffer.put(bytes[i * 4 + 3])
                        buffer.put(bytes[i * 4])
                        buffer.put(bytes[i * 4 + 1])
                        buffer.put(bytes[i * 4 + 2])
                    }
                }
            }
            else -> throw RuntimeException("Not matching sizes! ${w * h} vs ${bytes.size}")
        }
        buffer.position(0)
        return buffer
    }

    fun uploadData(buffer: ByteBuffer) {
        GFX.check()
        val t0 = System.nanoTime()
        ensurePointer()
        bindBeforeUpload()
        locallyAllocated = allocate(locallyAllocated, w * h * 4L)
        texImage2D(w, h, TargetType.UByteTarget4, buffer)
        // glTexImage2D(tex2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        byteBufferPool.returnBuffer(buffer)
        val t1 = System.nanoTime() // 0.02s for a single 4k texture
        isCreated = true
        filtering(filtering)
        clamping(clamping)
        val t2 = System.nanoTime() // 1e-6
        if (w * h > 1e4 && (t2 - t0) * 1e-9f > 0.01f) LOGGER.info("Used ${(t1 - t0) * 1e-9f}s + ${(t2 - t1) * 1e-9f}s to upload ${(w * h) / 1e6f} MPixel image to GPU")
        GFX.check()
    }

    /*fun uploadData2(data: ByteBuffer, callback: () -> Unit) {

        val pbo = GL15.glGenBuffers()
        val type = GL21.GL_PIXEL_UNPACK_BUFFER

        GL15.glBindBuffer(type, pbo)
        GL15.glBufferData(type, w * h * 4L, GL_STREAM_DRAW)
        val mappedBuffer = GL15.glMapBuffer(type, GL15.GL_WRITE_ONLY)!!

        //threadWithName("Texture2D::uploadData2") {

        val startPosition = mappedBuffer.position()
        mappedBuffer.put(data)
        mappedBuffer.position(startPosition)

        //GFX.addGPUTask(1) {
        GL15.glBindBuffer(type, pbo)
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

    fun toByteBuffer(ints: IntArray): ByteBuffer {
        val buffer = byteBufferPool[ints.size * 4, false]
        for (i in ints) buffer.putInt(i)
        buffer.position(0)
        return buffer
    }

    fun uploadData(ints: IntArray) {
        // if(h > 20) println("using ints: $w $h ${ints.size}")
        GFX.check()
        ensurePointer()
        bindBeforeUpload()
        locallyAllocated = allocate(locallyAllocated, w * h * 4L)
        glTexImage2D(tex2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, ints)
        /*uploadData2(toByteBuffer(ints)) {
            GFX.check()
        }*/
        isCreated = true
        filtering(filtering)
        clamping(clamping)
        GFX.check()
    }

    fun createRGBA(ints: IntBuffer) {
        checkSize(1, ints.capacity())
        GFX.check()
        ensurePointer()
        bindBeforeUpload()
        locallyAllocated = allocate(locallyAllocated, w * h * 4L)
        glTexImage2D(tex2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, ints)
        isCreated = true
        filtering(filtering)
        clamping(clamping)
        GFX.check()
    }

    fun createRGB(ints: IntBuffer) {
        checkSize(1, ints.capacity())
        GFX.check()
        ensurePointer()
        bindBeforeUpload()
        locallyAllocated = allocate(locallyAllocated, w * h * 3L)
        glTexImage2D(tex2D, 0, GL_RGB8, w, h, 0, GL_RGB, GL_UNSIGNED_BYTE, ints)
        isCreated = true
        filtering(filtering)
        clamping(clamping)
        GFX.check()
    }

    fun createMonochrome(data: ByteBuffer) {
        checkSize(1, data)
        GFX.check()
        ensurePointer()
        bindBeforeUpload()
        GFX.check()
        locallyAllocated = allocate(locallyAllocated, w * h.toLong())
        texImage2D(w, h, TargetType.UByteTarget1, data)
        // glTexImage2D(tex2D, 0, GL_RED, w, h, 0, GL_RED, GL_UNSIGNED_BYTE, data)
        byteBufferPool.returnBuffer(data)
        isCreated = true
        filtering(filtering)
        clamping(clamping)
        GFX.check()
    }

    /**
     * creates a monochrome float32 image on the GPU
     * used by SDF
     * */
    fun createMonochrome(data: FloatBuffer) {
        checkSize(1, data)
        GFX.check()
        ensurePointer()
        bindBeforeUpload()
        GFX.check()
        locallyAllocated = allocate(locallyAllocated, w * h * 4L)
        glTexImage2D(tex2D, 0, GL_R32F, w, h, 0, GL_RED, GL_FLOAT, data)
        isCreated = true
        filtering(filtering)
        clamping(clamping)
        GFX.check()
    }

    fun createMonochrome(data: ByteArray) {
        checkSize(1, data.size)
        GFX.check()
        ensurePointer()
        bindBeforeUpload()
        GFX.check()
        val byteBuffer = byteBufferPool[data.size, false]
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.position(0)
        locallyAllocated = allocate(locallyAllocated, w * h.toLong())
        texImage2D(w, h, TargetType.UByteTarget1, byteBuffer)
        // glTexImage2D(tex2D, 0, GL_RED, w, h, 0, GL_RED, GL_UNSIGNED_BYTE, byteBuffer)
        byteBufferPool.returnBuffer(byteBuffer)
        isCreated = true
        filtering(filtering)
        clamping(clamping)
        GFX.check()
    }

    fun create(data: FloatArray) {
        checkSize(4, data.size)
        val byteBuffer = byteBufferPool[data.size * 4, false]
            .order(ByteOrder.nativeOrder())
        byteBuffer.position(0)
        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(data)
        floatBuffer.position(0)
        create(floatBuffer, byteBuffer)
    }

    fun create(buffer: FloatBuffer, byteBuffer: ByteBuffer?) {
        ensurePointer()
        bindBeforeUpload()
        GFX.check()
        // rgba32f as internal format is extremely important... otherwise the value is cropped
        locallyAllocated = allocate(locallyAllocated, w * h * 4L)
        texImage2D(w, h, TargetType.FloatTarget4, byteBuffer)
        // glTexImage2D(tex2D, 0, GL_RGBA32F, w, h, 0, GL_RGBA, GL_FLOAT, buffer)
        byteBufferPool.returnBuffer(byteBuffer)
        isCreated = true
        filtering(filtering)
        GFX.check()
    }

    fun createRGBA(data: ByteArray) {
        checkSize(4, data.size)
        val byteBuffer = byteBufferPool[data.size, false]
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.position(0)
        createRGBA(byteBuffer)
    }

    fun createRGBA(buffer: ByteBuffer) {
        checkSize(4, buffer)
        ensurePointer()
        bindBeforeUpload()
        GFX.check()
        locallyAllocated = allocate(locallyAllocated, w * h * 4L)
        texImage2D(w, h, TargetType.UByteTarget4, buffer)
        // glTexImage2D(tex2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        byteBufferPool.returnBuffer(buffer)
        isCreated = true
        filtering(filtering)
        clamping(clamping)
        GFX.check()
    }

    fun ensureFilterAndClamping(nearest: GPUFiltering, clamping: Clamping) {
        if (nearest != this.filtering) filtering(nearest)
        if (clamping != this.clamping) clamping(clamping)
    }

    private fun clamping(clamping: Clamping) {
        if (!withMultisampling) {
            this.clamping = clamping
            val type = clamping.mode
            glTexParameteri(tex2D, GL_TEXTURE_WRAP_S, type)
            glTexParameteri(tex2D, GL_TEXTURE_WRAP_T, type)
        }
    }

    private fun filtering(nearest: GPUFiltering) {
        if (withMultisampling) {
            this.filtering = GPUFiltering.TRULY_NEAREST
            // multisample textures only support nearest filtering;
            // they don't accept the command to be what they are either
            return
        }
        if (!hasMipmap && nearest.needsMipmap) {
            glGenerateMipmap(tex2D)
            hasMipmap = true
            if (GFX.supportsAnisotropicFiltering) {
                val anisotropy = GFX.anisotropy
                glTexParameteri(tex2D, GL_TEXTURE_LOD_BIAS, 0)
                glTexParameterf(tex2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy)
            }
            glTexParameteri(tex2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        }
        glTexParameteri(tex2D, GL_TEXTURE_MIN_FILTER, nearest.min)
        glTexParameteri(tex2D, GL_TEXTURE_MAG_FILTER, nearest.mag)
        this.filtering = nearest
    }

    var hasMipmap = false

    private fun bindBeforeUpload() {
        if (pointer == -1) throw RuntimeException()
        bindTexture(tex2D, pointer)
    }

    override fun bind(nearest: GPUFiltering, clamping: Clamping): Boolean {
        if (pointer > -1 && isCreated) {
            val result = bindTexture(tex2D, pointer)
            ensureFilterAndClamping(nearest, clamping)
            return result
        } else throw IllegalStateException("Cannot bind non-created texture!")
    }

    override fun bind(index: Int, nearest: GPUFiltering, clamping: Clamping): Boolean {
        activeSlot(index)
        return bind(nearest, clamping)
    }

    fun bind() = bind(filtering, clamping)
    fun bind(index: Int) = bind(index, filtering, clamping)

    override fun destroy() {
        isCreated = false
        isDestroyed = true
        val pointer = pointer
        if (pointer > -1) {
            if (!isGFXThread()) {
                GFX.addGPUTask(1){
                    locallyAllocated = allocate(locallyAllocated, 0L)
                    texturesToDelete.add(pointer)
                }
            } else {
                locallyAllocated = allocate(locallyAllocated, 0L)
                texturesToDelete.add(pointer)
            }
        }
        this.pointer = -1
    }

    fun createDepth() {
        ensurePointer()
        bindBeforeUpload()
        locallyAllocated = allocate(locallyAllocated, w * h * 4L)
        if (withMultisampling) {
            glTexImage2DMultisample(tex2D, samples, GL_DEPTH_COMPONENT32, w, h, false)
        } else {
            glTexImage2D(tex2D, 0, GL_DEPTH_COMPONENT, w, h, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0)
        }
        filtering(filtering)
        clamping(Clamping.CLAMP)
        GFX.check()
    }

    private fun checkSize(channels: Int, buffer: Buffer) {
        checkSize(channels, buffer.remaining())
    }

    private fun checkSize(channels: Int, size: Int) {
        if (size < w * h * channels) throw IllegalArgumentException("Incorrect size, ${w * h * channels} vs ${size}!")
    }

    companion object {

        val byteBufferPool = ByteBufferPool(64, true)
        val byteArrayPool = ByteArrayPool(64, true)

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

        fun bindTexture(mode: Int, pointer: Int): Boolean {
            if (pointer < 0) throw IllegalArgumentException("Pointer must be valid")
            return if (boundTextures[boundTextureSlot] != pointer) {
                boundTextures[boundTextureSlot] = pointer
                glBindTexture(mode, pointer)
                true
            } else false
        }

        private val LOGGER = LogManager.getLogger(Texture2D::class)
        val textureBudgetTotal = DefaultConfig["gpu.textureBudget", 1_000_000]
        var textureBudgetUsed = 0
        val texturesToDelete = ArrayList<Int>()

        private var creationIndex = 0
        private val creationIndices = IntArray(16)
        private fun createTexture(): Int {
            if (creationIndex == 0 || creationIndex == creationIndices.size) {
                creationIndex = 0
                glGenTextures(creationIndices)
                GFX.check()
            }
            return creationIndices[creationIndex++]
        }

        fun destroyTextures() {
            if (texturesToDelete.isNotEmpty()) {
                glDeleteTextures(texturesToDelete.toIntArray())
                texturesToDelete.clear()
            }
        }

    }

}