package me.anno.video.formats

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib.shader3DARGB
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.readNBytes2
import me.anno.video.LastFrame
import me.anno.video.VFrame
import java.io.InputStream

class ARGBFrame(w: Int, h: Int) : VFrame(w, h, 0) {

    private val argb = Texture2D("argb-frame", w, h, 1)

    override fun load(input: InputStream) {
        val s0 = w * h * 4
        val data = input.readNBytes2(s0)
        if (data.isEmpty()) throw LastFrame()
        if (data.size < s0) throw RuntimeException("not enough data, only ${data.size} of $s0")
        GFX.addGPUTask(w, h) {
            // the data actually still is argb and shuffling is needed
            // to convert it into rgba (needs to be done in the shader (or by a small preprocessing step of the data))
            argb.createRGBA(data)
            isLoaded = true
        }
    }

    override fun get3DShader() = shader3DARGB

    override fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping) {
        argb.bind(offset, nearestFiltering, clamping)
    }

    override fun destroy() {
        argb.destroy()
    }

}