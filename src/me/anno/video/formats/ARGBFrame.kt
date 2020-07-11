package me.anno.video.formats

import me.anno.gpu.GFX
import me.anno.gpu.Shader
import me.anno.gpu.texture.Texture2D
import me.anno.video.Frame
import me.anno.video.LastFrame
import java.io.InputStream
import java.lang.RuntimeException


class ARGBFrame(w: Int, h: Int): Frame(w,h){

    val argb = Texture2D(w, h, 1)

    override fun load(input: InputStream){
        val s0 = w*h*4
        val data = input.readNBytes(s0)
        if(data.isEmpty()) throw LastFrame()
        if(data.size < s0) throw RuntimeException("not enough data, only ${data.size} of $s0")
        GFX.addGPUTask {
            argb.create(data)
            isLoaded = true
            15
        }
    }

    override fun get3DShader(): Shader = GFX.shader3DARGB

    override fun bind(offset: Int, nearestFiltering: Boolean) {
        argb.bind(offset, nearestFiltering)
    }

    override fun destroy(){
        argb.destroy()
    }

}