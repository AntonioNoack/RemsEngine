package me.anno.video.formats

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib.shader3DRGBA
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.readNBytes2
import me.anno.video.LastFrame
import me.anno.video.VFrame
import java.io.EOFException
import java.io.InputStream

class RGBFrame(w: Int, h: Int) : VFrame(w, h, -1) {

    private val rgb = Texture2D("rgb-frame", w, h, 1)

    override fun load(input: InputStream) {
        val s0 = w * h
        val data = ByteArray(s0 * 4)
        val srcData = input.readNBytes2(s0 * 3)
        if (srcData.isEmpty()) throw LastFrame()
        if (srcData.size < data.size) throw EOFException()
        var j = 0
        var k = 0
        val alpha = 255.toByte()
        println("loaded rgb")
        for (i in 0 until s0) {
            data[j++] = srcData[k++]
            data[j++] = srcData[k++]
            data[j++] = srcData[k++]
            data[j++] = alpha // offset is required
        }
        GFX.addGPUTask(w, h) {
            rgb.createRGBA(data)
            isLoaded = true
        }
    }

    override fun get3DShader() = shader3DRGBA

    override fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping) {
        rgb.bind(offset, nearestFiltering, clamping)
    }

    override fun destroy() {
        rgb.destroy()
    }

}