package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.utils.Color.rgba
import java.io.InputStream

object RGBFrame : CPUFrame() {

    override fun load(w: Int, h: Int, input: InputStream): Image {
        return IntImage(w, h, IntArray(w * h) {
            val r = input.read()
            val g = input.read()
            val b = input.read()
            rgba(r, g, b, 255)
        }, false)
    }

}