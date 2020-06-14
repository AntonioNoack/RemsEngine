package me.anno.video.formats

import me.anno.gpu.GFX
import me.anno.gpu.Shader
import me.anno.gpu.texture.Texture2D
import me.anno.video.Frame
import me.anno.video.LastFrame
import java.io.InputStream
import java.lang.RuntimeException


class I420Frame(iw: Int, ih: Int): Frame(iw,ih){

    // this is correct, confirmed by example
    val w2 = (w+1)/2
    val h2 = (h+1)/2

    val y = Texture2D(w, h)
    val u = Texture2D(w2, h2)
    val v = Texture2D(w2, h2)

    fun load(input: InputStream){
        val s0 = w * h
        val s1 = w2 * h2
        val yData = input.readNBytes(s0)
        if(yData.isEmpty()) throw LastFrame()
        if(yData.size < s0) throw RuntimeException("not enough data, only ${yData.size} of $s0")
        GFX.addTask {
            y.createMonochrome(yData)
            10
        }
        val uData = input.readNBytes(s1)
        if(uData.size < s1) throw RuntimeException("not enough data, only ${uData.size} of $s1")
        GFX.addTask {
            u.createMonochrome(uData)
            10
        }
        val vData = input.readNBytes(s1)
        if(vData.size < s1) throw RuntimeException("not enough data, only ${vData.size} of $s1")
        GFX.addTask {
            v.createMonochrome(vData)
            isLoaded = true
            10
        }
    }

    override fun get3DShader(): Shader = GFX.shader3DYUV

    override fun bind(offset: Int, nearestFiltering: Boolean){
        v.bind(offset+2, nearestFiltering)
        u.bind(offset+1, nearestFiltering)
        y.bind(offset, nearestFiltering)
    }

    override fun destroy(){
        y.destroy()
        u.destroy()
        v.destroy()
    }

}