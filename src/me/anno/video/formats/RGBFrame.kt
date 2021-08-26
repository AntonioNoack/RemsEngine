package me.anno.video.formats

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib.shader3DRGBA
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.video.VFrame
import java.io.EOFException
import java.io.InputStream

open class RGBFrame(w: Int, h: Int) : VFrame(w, h, -1) {

    val rgb = Texture2D("rgb-frame", w, h, 1)

    override fun load(input: InputStream) {
        val s0 = w * h
        val data = Texture2D.byteBufferPool[s0 * 4, false]
        data.position(0)
        for (i in 0 until s0) {
            val r = input.read()
            val g = input.read()
            val b = input.read()
            if (r < 0 || g < 0 || b < 0) throw EOFException()
            data.put(r.toByte())
            data.put(g.toByte())
            data.put(b.toByte())
            data.put(-1) // offset is required
        }
        data.position(0)
        creationLimiter.acquire()
        GFX.addGPUTask(w, h) {
            rgb.createRGB(data, true)
            creationLimiter.release()
        }
    }

    override fun get3DShader() = shader3DRGBA
    override fun getTextures(): List<Texture2D> = listOf(rgb)

    override fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping) {
        rgb.bind(offset, nearestFiltering, clamping)
    }

}