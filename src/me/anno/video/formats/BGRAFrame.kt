package me.anno.video.formats

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib.shader3DBGRA
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.Texture2D
import me.anno.utils.readNBytes2
import me.anno.video.VFrame
import me.anno.video.LastFrame
import java.io.InputStream
import java.lang.RuntimeException


class BGRAFrame(w: Int, h: Int): VFrame(w,h){

    val bgra = Texture2D(w, h, 1)

    override fun load(input: InputStream){
        val s0 = w*h*4
        val data = input.readNBytes2(s0)
        if(data.isEmpty()) throw LastFrame()
        if(data.size < s0) throw RuntimeException("not enough data, only ${data.size} of $s0")
        GFX.addGPUTask(w, h){
            bgra.createRGBA(data)
            isLoaded = true
        }
    }

    override fun get3DShader() = shader3DBGRA

    override fun bind(offset: Int, nearestFiltering: Boolean, clampMode: ClampMode){
        bgra.bind(offset, nearestFiltering, clampMode)
    }

    override fun destroy(){
        bgra.destroy()
    }

}