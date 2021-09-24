package me.anno.video.formats.cpu

import me.anno.gpu.texture.Texture2D.Companion.bufferPool
import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.utils.input.Input.readNBytes2
import java.io.InputStream

object I420Frame : CPUFrame() {

    override fun load(w: Int, h: Int, input: InputStream): Image {

        val s0 = w * h

        val yData = input.readNBytes2(s0, bufferPool)

        // this is correct, confirmed by example
        val w2 = (w + 1) / 2
        val h2 = (h + 1) / 2

        val s1 = w2 * h2
        val uData = input.readNBytes2(s1, bufferPool)
        val vData = input.readNBytes2(s1, bufferPool)

        val data = IntArray(w * h) {
            val xi = it % w
            val yi = it / w
            val i2 = (xi / 2) + (yi / 2) * w2
            // todo uv-interpolation
            yuv2rgb(yData[it], uData[i2], vData[i2])
        }

        bufferPool.returnBuffer(yData)
        bufferPool.returnBuffer(uData)
        bufferPool.returnBuffer(vData)

        return IntImage(w, h, data, false)

    }

}