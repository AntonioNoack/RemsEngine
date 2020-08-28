package me.anno.gpu.texture

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.TextureLib.invisibleTexture
import me.anno.input.Input.keysDown
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL30
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
        filtering(true)
    }

    val withMultisampling get() = samples > 1

    val tex2D = if(withMultisampling) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D

    var pointer = -1
    var isCreated = false
    var isFilteredNearest = false
    var isVirtual = false

    fun setSize(width: Int, height: Int){
        w = width
        h = height
    }

    fun ensurePointer(){
        if(pointer < 0) {
            pointer = glGenTextures()
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
        filtering(isFilteredNearest)
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
        filtering(isFilteredNearest)
        isCreated = true
        GFX.check()
    }

    fun create(createImage: () -> BufferedImage){
        val requiredBudget = textureBudgetUsed + w * h
        if(requiredBudget > textureBudgetTotal || Thread.currentThread() != GFX.glThread){
            thread { create(createImage(), false) }
        } else {
            textureBudgetUsed = requiredBudget
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
        else GFX.addGPUTask {
            uploadData(intData)
            1
        }
    }

    fun uploadData(intData: IntArray){
        GFX.check()
        ensurePointer()
        forceBind()
        glTexImage2D(tex2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, intData)
        isCreated = true
        GFX.check()
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun createMonochrome(data: ByteArray){
        if(w*h != data.size) throw RuntimeException("incorrect size!")
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
        filtering(isFilteredNearest)
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
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun create(data: ByteArray){
        if(w*h*4 != data.size) throw RuntimeException("incorrect size!")
        ensurePointer()
        forceBind()
        GFX.check()
        val byteBuffer = ByteBuffer.allocateDirect(data.size)
        byteBuffer.position(0)
        byteBuffer.put(data)
        byteBuffer.position(0)
        glTexImage2D(tex2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer)
        isCreated = true
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun ensureFiltering(nearest: Boolean){
        if(nearest != isFilteredNearest) filtering(nearest)
    }

    fun filtering(nearest: Boolean){
        if(withMultisampling){
            isFilteredNearest = true
            // multisample textures only support nearest filtering;
            // they don't accept the command to be what they are either
            return
        }
        synchronized(this){
            // todo switching back from nearest to linear doesn't work for textures
            if(nearest){
                val type = GL_NEAREST
                glTexParameteri(tex2D, GL_TEXTURE_MAG_FILTER, type)
                glTexParameteri(tex2D, GL_TEXTURE_MIN_FILTER, type)
            } else {
                if(!hasMipmap){
                    glGenerateMipmap(tex2D)
                    hasMipmap = true
                    if(GFX.supportsAnisotropicFiltering){
                        val anisotropy = GFX.anisotropy
                        glTexParameteri(tex2D, GL_TEXTURE_LOD_BIAS, 0)
                        glTexParameterf(tex2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy)
                    }
                }
                glTexParameteri(tex2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
                glTexParameteri(tex2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
            }
            isFilteredNearest = nearest
        }
    }

    var hasMipmap = false

    fun clamping(repeat: Boolean){
        if(!withMultisampling){
            val type = if(repeat) GL_REPEAT else GL_CLAMP_TO_EDGE
            glTexParameteri(tex2D, GL_TEXTURE_WRAP_S, type)
            glTexParameteri(tex2D, GL_TEXTURE_WRAP_T, type)
        }
    }

    fun forceBind(){
        if(pointer == -1) throw RuntimeException()
        glBindTexture(tex2D, pointer)
    }

    override fun bind(nearest: Boolean){
        if(pointer > -1 && isCreated){
            glBindTexture(tex2D, pointer)
            ensureFiltering(nearest)
        } else invisibleTexture.bind(true)
    }

    override fun bind(index: Int, nearest: Boolean){
        glActiveTexture(GL_TEXTURE0 + index)
        bind(nearest)
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
            glTexImage2D(tex2D, 0, GL_DEPTH_COMPONENT32, w, h, 0, GL_DEPTH_COMPONENT,	GL_FLOAT, 0)
        }
        filtering(isFilteredNearest)
        clamping(false)
        GFX.check()
    }

    companion object {
        val LOGGER = LogManager.getLogger(Texture2D::class)
        val textureBudgetTotal = DefaultConfig["gpu.textureBudget", 1_000_000]
        var textureBudgetUsed = 0
    }

}