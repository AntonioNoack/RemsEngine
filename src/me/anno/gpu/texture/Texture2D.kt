package me.anno.gpu.texture

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.TextureLib.invisibleTexture
import me.anno.gpu.framebuffer.TargetType
import me.anno.objects.modes.RotateJPEG
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.GL_TEXTURE_2D_MULTISAMPLE
import org.lwjgl.opengl.GL32.glTexImage2DMultisample
import java.awt.image.BufferedImage
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.concurrent.thread

class Texture2D(override var w: Int, override var h: Int, val samples: Int): ITexture2D {

    constructor(img: BufferedImage): this(img.width, img.height, 1){
        create(img, true)
        filtering(GPUFiltering.NEAREST)
    }

    val withMultisampling get() = samples > 1

    val tex2D = if(withMultisampling) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D
    var state: Pair<Texture2D, Int>? = null

    var pointer = -1
    var isCreated = false
    var filtering = GPUFiltering.TRULY_NEAREST
    var clamping = Clamping.CLAMP

    // only used for images with exif rotation tag...
    var rotation: RotateJPEG? = null

    fun setSize(width: Int, height: Int){
        w = width
        h = height
    }

    fun ensurePointer(){
        if(pointer < 0) {
            pointer = glGenTextures()
            state = this to pointer
            // many textures can be created by the console log and the fps viewer constantly xD
            // maybe we should use allocation free versions there xD
        }
        if(pointer <= 0) throw RuntimeException()
    }

    fun create(){
        ensurePointer()
        forceBind()
        if(withMultisampling){
            glTexImage2DMultisample(tex2D, samples, GL_RGBA8, w, h, false)
        } else {
            glTexImage2D(tex2D, 0, GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)
        }
        filtering(filtering)
        clamping(clamping)
        isCreated = true
    }


    fun create(type: TargetType){
        ensurePointer()
        forceBind()
        if(withMultisampling){
            glTexImage2DMultisample(tex2D, samples, type.type0, w, h, false)
        } else {
            glTexImage2D(tex2D, 0, type.type0, w, h, 0, type.type1, type.fillType, null as ByteBuffer?)
        }
        filtering(filtering)
        clamping(clamping)
        isCreated = true
    }

    fun createFP32(){
        GFX.check()
        ensurePointer()
        forceBind()
        GFX.check()
        if(withMultisampling){
            glTexImage2DMultisample(tex2D, samples, GL_RGBA32F, w, h, false)
        } else {
            glTexImage2D(tex2D, 0, GL_RGBA32F, w, h, 0, GL_RGBA, GL_FLOAT, null as ByteBuffer?)
        }
        // LOGGER.info("FP32 $w $h $samples")
        GFX.check()
        filtering(filtering)
        clamping(clamping)
        isCreated = true
        GFX.check()
    }

    fun create(createImage: () -> BufferedImage, forceSync: Boolean){
        val requiredBudget = textureBudgetUsed + w * h
        if(requiredBudget > textureBudgetTotal || Thread.currentThread() != GFX.glThread){
            if(forceSync){
                GFX.addGPUTask(1000){
                    create(createImage(), true)
                }
            } else {
                thread { create(createImage(), false) }
            }
        } else {
            textureBudgetUsed += requiredBudget
            create(createImage(), true)
        }
    }

    fun create(img: BufferedImage, sync: Boolean){
        w = img.width
        h = img.height
        val intData = img.getRGB(0, 0, w, h, null, 0, img.width)
        if(ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN){
            for(i in intData.indices){// argb -> abgr
                val argb = intData[i]
                val r = (argb and 0xff0000).shr(16)
                val b = (argb and 0xff).shl(16)
                intData[i] = argb and 0xff00ff00.toInt() or r or b
            }
        } else {
            for(i in intData.indices){// argb -> rgba
                val argb = intData[i]
                val a = argb.shr(24) and 255
                val rgb = argb.and(0xffffff) shl 8
                intData[i] = rgb or a
            }
        }
        if(sync) uploadData(intData)
        else GFX.addGPUTask(500){
            uploadData(intData)
        }
    }

    fun uploadData(intData: IntArray){
        GFX.check()
        ensurePointer()
        forceBind()
        glTexImage2D(tex2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, intData)
        isCreated = true
        GFX.check()
        filtering(filtering)
        clamping(clamping)
        GFX.check()
    }

    fun createMonochrome(data: ByteArray){
        if(w * h != data.size) throw RuntimeException("incorrect size!")
        GFX.check()
        ensurePointer()
        forceBind()
        GFX.check()
        val byteBuffer = ByteBuffer.allocateDirect(data.size)
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.position(0)
        glTexImage2D(tex2D, 0, GL_RED, w, h, 0, GL_RED, GL_UNSIGNED_BYTE, byteBuffer)
        isCreated = true
        filtering(filtering)
        clamping(clamping)
        GFX.check()
    }

    fun create(data: FloatArray){
        if(w*h*4 != data.size) throw RuntimeException("incorrect size!")
        val byteBuffer = ByteBuffer
            .allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
        byteBuffer.position(0)
        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(data)
        floatBuffer.position(0)
        create(floatBuffer)
    }

    fun create(floatBuffer: FloatBuffer){
        ensurePointer()
        forceBind()
        GFX.check()
        // rgba32f as internal format is extremely important... otherwise the value is cropped
        glTexImage2D(tex2D, 0, GL_RGBA32F, w, h, 0, GL_RGBA, GL_FLOAT, floatBuffer)
        isCreated = true
        filtering(filtering)
        GFX.check()
    }

    fun createRGBA(data: ByteArray){
        if(w*h*4 != data.size) throw RuntimeException("incorrect size!")
        val byteBuffer = ByteBuffer.allocateDirect(data.size)
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.position(0)
        createRGBA(byteBuffer)
    }

    fun createRGBA(byteBuffer: ByteBuffer){
        if(w*h*4 != byteBuffer.capacity()) throw RuntimeException("incorrect size!")
        ensurePointer()
        forceBind()
        GFX.check()
        glTexImage2D(tex2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer)
        isCreated = true
        filtering(filtering)
        clamping(clamping)
        GFX.check()
    }

    fun ensureFilterAndClamping(nearest: GPUFiltering, clamping: Clamping){
        if(nearest != this.filtering) filtering(nearest)
        if(clamping != this.clamping) clamping(clamping)
    }

    private fun clamping(clamping: Clamping){
        if(!withMultisampling){
            this.clamping = clamping
            val type = clamping.mode
            glTexParameteri(tex2D, GL_TEXTURE_WRAP_S, type)
            glTexParameteri(tex2D, GL_TEXTURE_WRAP_T, type)
        }
    }

    private fun filtering(nearest: GPUFiltering){
        if(withMultisampling){
            this.filtering = GPUFiltering.TRULY_NEAREST
            // multisample textures only support nearest filtering;
            // they don't accept the command to be what they are either
            return
        }
        if(!hasMipmap && nearest.needsMipmap){
            glGenerateMipmap(tex2D)
            hasMipmap = true
            if(GFX.supportsAnisotropicFiltering){
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

    fun forceBind(){
        if(pointer == -1) throw RuntimeException()
        glBindTexture(tex2D, pointer)
    }

    override fun bind(nearest: GPUFiltering, clamping: Clamping){
        if(pointer > -1 && isCreated){
            glBindTexture(tex2D, pointer)
            ensureFilterAndClamping(nearest, clamping)
        } else invisibleTexture.bind(invisibleTexture.filtering, invisibleTexture.clamping)
    }

    override fun bind(index: Int, nearest: GPUFiltering, clamping: Clamping){
        glActiveTexture(GL_TEXTURE0 + index)
        bind(nearest, clamping)
    }

    override fun destroy(){
        if(pointer > -1) glDeleteTextures(pointer)
    }

    fun createDepth(){
        ensurePointer()
        forceBind()
        if(withMultisampling){
            glTexImage2DMultisample(tex2D, samples, GL_DEPTH_COMPONENT32, w, h, false)
        } else {
            glTexImage2D(tex2D, 0, GL_DEPTH_COMPONENT, w, h, 0, GL_DEPTH_COMPONENT,	GL_FLOAT, 0)
        }
        filtering(filtering)
        clamping(Clamping.CLAMP)
        GFX.check()
    }

    companion object {
        val LOGGER = LogManager.getLogger(Texture2D::class)
        val textureBudgetTotal = DefaultConfig["gpu.textureBudget", 1_000_000]
        var textureBudgetUsed = 0
    }

}