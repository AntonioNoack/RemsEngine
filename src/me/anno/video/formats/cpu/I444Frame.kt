package me.anno.video.formats.cpu

import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.utils.input.Input.readNBytes2
import java.io.InputStream

// this seems to work, and to be correct
object I444Frame : CPUFrame() {

    override fun load(w: Int, h: Int, input: InputStream): Image {

        val s0 = w * h

        val yData = input.readNBytes2(s0, Texture2D.bufferPool)
        val uData = input.readNBytes2(s0, Texture2D.bufferPool)
        val vData = input.readNBytes2(s0, Texture2D.bufferPool)

        val data = IntArray(w * h) {
            yuv2rgb(yData[it], uData[it], vData[it])
        }

        Texture2D.bufferPool.returnBuffer(yData)
        Texture2D.bufferPool.returnBuffer(uData)
        Texture2D.bufferPool.returnBuffer(vData)

        return IntImage(w, h, data, false)

    }

}