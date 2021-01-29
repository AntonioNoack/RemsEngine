package me.anno.video.formats

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib.shader3DBGRA
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.input.readNBytes2
import me.anno.video.LastFrame
import me.anno.video.VFrame
import java.io.InputStream


class BGRAFrame(w: Int, h: Int) : VFrame(w, h, 1) {

    private val bgra = Texture2D("bgra-frame", w, h, 1)

    override fun load(input: InputStream) {
        val s0 = w * h * 4
        val data = input.readNBytes2(s0)
        if (data.isEmpty()) throw LastFrame()
        if (data.size < s0) throw RuntimeException("not enough data, only ${data.size} of $s0")
        GFX.addGPUTask(w, h) {
            bgra.createRGBA(data)
            isLoaded = true
        }
    }

    override fun get3DShader() = shader3DBGRA

    override fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping) {
        bgra.bind(offset, nearestFiltering, clamping)
    }

    override fun destroy() {
        bgra.destroy()
    }

}