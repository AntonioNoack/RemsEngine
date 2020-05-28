package me.anno.gpu.texture

import me.anno.gpu.GFX
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL30.*
import java.awt.image.BufferedImage
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Texture2D(val w: Int, val h: Int){

    constructor(img: BufferedImage): this(img.width, img.height){
        create(img)
        filtering(true)
    }

    var pointer = -1

    fun ensurePointer(){
        if(pointer < 0) pointer = glGenTextures()
    }

    fun create(){
        ensurePointer()
        bind()
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)
    }

    fun createFP32(){
        ensurePointer()
        bind()
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, w, h, 0, GL_RGBA, GL_FLOAT, null as ByteBuffer?)
    }

    fun create(img: BufferedImage){
        ensurePointer()
        bind()
        val intData = img.getRGB(0, 0, w, h, null, 0, img.width)
        GFX.check()
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
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, intData)
        GFX.check()
        filtering(true)
        GFX.check()
    }

    fun createMonochrome(data: ByteArray){
        if(w*h != data.size) throw RuntimeException("incorrect size!")
        ensurePointer()
        bind()
        GFX.check()
        val byteBuffer = ByteBuffer
            .allocateDirect(data.size)
            .position(0)
            .put(data)
            .position(0)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, w, h, 0, GL11.GL_RED, GL_UNSIGNED_BYTE, byteBuffer)
        filtering(false)
        GFX.check()
    }

    fun create(data: ByteArray){
        if(w*h*4 != data.size) throw RuntimeException("incorrect size!")
        ensurePointer()
        bind()
        GFX.check()
        val byteBuffer = ByteBuffer
            .allocateDirect(data.size)
            .position(0)
            .put(data)
            .position(0)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer)
        filtering(false)
        GFX.check()
    }

    fun createRGB(data: ByteArray){
        if(w*h*3 != data.size) throw RuntimeException("incorrect size!")
        ensurePointer()
        bind()
        GFX.check()
        val byteBuffer = ByteBuffer
            .allocateDirect(data.size)
            .position(0)
            .put(data)
            .position(0)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w, h, 0, GL_RGB, GL_UNSIGNED_BYTE, byteBuffer)
        filtering(false)
        GFX.check()
    }

    fun filtering(nearest: Boolean){
        val type = if(nearest) GL_NEAREST else GL_LINEAR
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, type)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, type)
    }

    fun bind(){
        glBindTexture(GL_TEXTURE_2D, pointer)
    }

    fun bind(index: Int){
        glActiveTexture(GL_TEXTURE0 + index)
        glBindTexture(GL_TEXTURE_2D, pointer)
    }

    fun destroy(){
        if(pointer > -1) glDeleteTextures(pointer)
    }

}