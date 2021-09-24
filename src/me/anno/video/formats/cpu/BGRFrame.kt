package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.utils.Color
import java.io.InputStream

object BGRFrame : CPUFrame() {

    override fun load(w: Int, h: Int, input: InputStream): Image {
        return IntImage(w, h, IntArray(w * h) {
            val b = input.read()
            val g = input.read()
            val r = input.read()
            Color.rgba(r, g, b, 255)
        }, true)
    }

}