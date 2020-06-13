package me.anno.gpu.texture

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D.Companion.textureBudgetTotal
import me.anno.gpu.texture.Texture2D.Companion.textureBudgetUsed
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL30.*
import java.awt.image.BufferedImage
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.concurrent.thread
import kotlin.math.min

class Texture3D(val w: Int, val h: Int, val d: Int){

    constructor(img: BufferedImage, depth: Int): this(img.width/depth, img.height, depth){
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
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA8, w, h, d, 0, GL11.GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)
        filtering(isFilteredNearest)
        isCreated = true
    }

    fun createFP32(){
        ensurePointer()
        forceBind()
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, w, h, d, 0, GL_RGBA, GL_FLOAT, null as ByteBuffer?)
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
        val intData = img.getRGB(0, 0, img.width, img.height, null, 0, img.width)
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
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA8, w, h, d, 0, GL_RGBA, GL_UNSIGNED_BYTE, intData)
        isCreated = true
        GFX.check()
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun createMonochrome(data: ByteArray){
        if(w*h*d != data.size) throw RuntimeException("incorrect size!")
        ensurePointer()
        forceBind()
        GFX.check()
        val byteBuffer = ByteBuffer
            .allocateDirect(data.size)
            .position(0)
            .put(data)
            .position(0)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_R8, w, h, d, 0, GL11.GL_RED, GL_UNSIGNED_BYTE, byteBuffer)
        isCreated = true
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun create(data: FloatArray){
        if(w*h*d*4 != data.size) throw RuntimeException("incorrect size!")
        val byteBuffer = ByteBuffer
            .allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .position(0)
        val floatBuffer = byteBuffer.asFloatBuffer()
            .put(data)
            .position(0)
        create(floatBuffer)
    }

    fun create(floatBuffer: FloatBuffer){
        ensurePointer()
        forceBind()
        GFX.check()
        // rgba32f as internal format is extremely important... otherwise the value is cropped
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, w, h, d, 0, GL_RGBA, GL_FLOAT, floatBuffer)
        isCreated = true
        filtering(isFilteredNearest)
        GFX.check()
    }

    fun create(data: ByteArray){
        if(w*h*d*4 != data.size) throw RuntimeException("incorrect size!")
        ensurePointer()
        forceBind()
        GFX.check()
        val byteBuffer = ByteBuffer
            .allocateDirect(data.size)
            .position(0)
            .put(data)
            .position(0)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA, w, h, d, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer)
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
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, type)
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, type)
        } else {
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        }
        isFilteredNearest = nearest
    }

    fun clamping(repeat: Boolean){
        val type = if(repeat) GL_REPEAT else GL_CLAMP_TO_EDGE
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, type)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, type)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, type)
    }

    fun forceBind(){
        if(pointer == -1) throw RuntimeException()
        glBindTexture(GL_TEXTURE_3D, pointer)
    }

    fun bind(nearest: Boolean){
        if(pointer > -1 && isCreated){
            glBindTexture(GL_TEXTURE_3D, pointer)
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

}