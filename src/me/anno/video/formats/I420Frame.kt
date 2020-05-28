package me.anno.video.formats

import me.anno.gpu.GFX
import me.anno.gpu.Shader
import me.anno.gpu.texture.Texture2D
import me.anno.video.Frame
import me.anno.video.LastFrame
import java.io.InputStream
import java.lang.RuntimeException


class I420Frame(w: Int, h: Int): Frame(w,h){

    val y = Texture2D(w,h)
    val u = Texture2D(w/2,h/2)
    val v = Texture2D(w/2,h/2)

    fun load(input: InputStream){
        val s0 = w*h
        val s1 = (w/2)*(h/2)
        val yData = input.readNBytes(s0)
        if(yData.isEmpty()) throw LastFrame()
        if(yData.size < s0) throw RuntimeException("not enough data, only ${yData.size} of $s0")
        GFX.addTask { y.createMonochrome(yData); 10 }
        val uData = input.readNBytes(s1)
        if(uData.size < s1) throw RuntimeException("not enough data, only ${uData.size} of $s1")
        GFX.addTask { u.createMonochrome(uData); 10 }
        val vData = input.readNBytes(s1)
        if(vData.size < s1) throw RuntimeException("not enough data, only ${vData.size} of $s1")
        GFX.addTask { v.createMonochrome(vData); isLoaded = true; 10 }
    }

    override fun get3DShader(): Shader = GFX.shader3DYUV

    override fun bind(offset: Int){
        u.bind(offset+1)
        v.bind(offset+2)
        y.bind(offset)
    }

    override fun destroy(){
        y.destroy()
        u.destroy()
        v.destroy()
    }

}