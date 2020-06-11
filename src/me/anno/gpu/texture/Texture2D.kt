package me.anno.gpu.texture

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL30.*
import java.awt.image.BufferedImage
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlin.math.min

class Texture2D(var w: Int, var h: Int){

    constructor(img: BufferedImage): this(img.width, img.height){
        create(img, true)
        filtering(true)
    }

    var pointer = -1
    var isCreated = false
    var isFilteredNearest = false

    fun ensurePointer(){
        if(pointer < 0) pointer = glGenTextures()
        if(pointer <= 0) throw RuntimeException()
    }

    fun create(){
        ensurePointer()
        forceBind()
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)
        filtering(isFilteredNearest)
        isCreated = true
    }

    fun createFP32(){
        ensurePointer()
        forceBind()
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, w, h, 0, GL_RGBA, GL_FLOAT, null as ByteBuffer?)
        filtering(isFilteredNearest)
        isCreated = true
    }

    fun create(createImage: () -> BufferedImage){
        val requiredBudget = textureBudgetUsed + w * h
        if(requiredBudget > textureBudgetTotal){
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
        else GFX.addTask {
            uploadData(intData)
            1
        }
    }

    fun uploadData(intData: IntArray){
        GFX.check()
        ensurePointer()
        forceBind()
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, intData)
        isCreated = true
        GFX.check()
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun createMonochrome(data: ByteArray){
        if(w*h != data.size) throw RuntimeException("incorrect size!")
        ensurePointer()
        forceBind()
        GFX.check()
        val byteBuffer = ByteBuffer
            .allocateDirect(data.size)
            .position(0)
            .put(data)
            .position(0)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, w, h, 0, GL11.GL_RED, GL_UNSIGNED_BYTE, byteBuffer)
        isCreated = true
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun create(data: FloatArray){
        if(w*h*4 != data.size) throw RuntimeException("incorrect size!")
        ensurePointer()
        forceBind()
        GFX.check()
        val byteBuffer = ByteBuffer
            .allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .position(0)
        val floatBuffer = byteBuffer.asFloatBuffer()
            .put(data)
            .position(0)
        // rgba32f as internal format is extremely important... otherwise the value is cropped
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, w, h, 0, GL_RGBA, GL_FLOAT, floatBuffer)
        isCreated = true
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun create(data: ByteArray){
        if(w*h*4 != data.size) throw RuntimeException("incorrect size!")
        ensurePointer()
        forceBind()
        GFX.check()
        val byteBuffer = ByteBuffer
            .allocateDirect(data.size)
            .position(0)
            .put(data)
            .position(0)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer)
        isCreated = true
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun ensureFiltering(nearest: Boolean){
        if(nearest != isFilteredNearest) filtering(nearest)
    }

    fun filtering(nearest: Boolean){
        if(nearest){
            val type = GL_NEAREST
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, type)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, type)
        } else {
            if(!hasMipmap){
                glGenerateMipmap(GL_TEXTURE_2D)
                hasMipmap = true
                if(GFX.supportsAnisotropicFiltering){
                    val anisotropy = GFX.anisotropy
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0)
                    glTexParameterf(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy)
                }
            }
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        }
        isFilteredNearest = nearest
    }

    var hasMipmap = false

    fun clamping(repeat: Boolean){
        val type = if(repeat) GL_REPEAT else GL_CLAMP_TO_EDGE
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, type)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, type)
    }

    fun forceBind(){
        if(pointer == -1) throw RuntimeException()
        glBindTexture(GL_TEXTURE_2D, pointer)
    }

    fun bind(nearest: Boolean){
        if(pointer > -1 && isCreated){
            glBindTexture(GL_TEXTURE_2D, pointer)
            ensureFiltering(nearest)
        } else GFX.invisibleTexture.bind(true)
    }

    fun bind(index: Int, nearest: Boolean){
        glActiveTexture(GL_TEXTURE0 + index)
        bind(nearest)
    }

    fun destroy(){
        if(pointer > -1) glDeleteTextures(pointer)
    }

    fun createDepth(){
        ensurePointer()
        forceBind()
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, w, h, 0, GL_DEPTH_COMPONENT,	GL_FLOAT, 0)
        filtering(isFilteredNearest)
        clamping(false)
        GFX.check()
    }

    companion object {
        val textureBudgetTotal = DefaultConfig["gpu.textureBudget", 1_000_000]
        var textureBudgetUsed = 0
    }

}