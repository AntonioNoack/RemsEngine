package me.anno.video.formats

import me.anno.gpu.GFX
import me.anno.gpu.Shader
import me.anno.gpu.texture.Texture2D
import me.anno.video.Frame
import me.anno.video.LastFrame
import java.io.InputStream
import java.lang.RuntimeException


class ARGBFrame(w: Int, h: Int): Frame(w,h){

    val argb = Texture2D(w,h)

    fun load(input: InputStream){
        val s0 = w*h*4
        val data = input.readNBytes(s0)
        if(data.isEmpty()) throw LastFrame()
        if(data.size < s0) throw RuntimeException("not enough data, only ${data.size} of $s0")
        GFX.addTask { argb.create(data); 15 }
    }

    override fun get3DShader(): Shader = GFX.shader3DARGB

    override fun bind(offset: Int){
        argb.bind(offset)
    }

    override fun destroy(){
        argb.destroy()
    }

}