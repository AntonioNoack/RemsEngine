package me.anno.video.formats

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib.shader3DRGBA
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.NearestMode
import me.anno.gpu.texture.Texture2D
import me.anno.video.VFrame
import me.anno.video.LastFrame
import java.io.EOFException
import java.io.InputStream

class RGBFrame(w: Int, h: Int): VFrame(w,h){

    val rgb = Texture2D(w, h, 1)

    override fun load(input: InputStream){
        val s0 = w*h
        val data = ByteArray(s0 * 4)
        var j = 0
        for(i in 0 until s0){
            val r0 = input.read()
            if(r0 < 0) throw if(j == 0) LastFrame() else EOFException()
            data[j++] = r0.toByte()
            data[j++] = input.read().toByte()
            data[j++] = input.read().toByte()
            data[j++] = 255.toByte() // offset is required
        }
        GFX.addGPUTask(w, h){
            rgb.createRGBA(data)
            isLoaded = true
        }
    }

    override fun get3DShader() = shader3DRGBA

    override fun bind(offset: Int, nearestFiltering: NearestMode, clampMode: ClampMode){
        rgb.bind(offset, nearestFiltering, clampMode)
    }

    override fun destroy(){
        rgb.destroy()
    }

}