package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.utils.Color.rgba
import java.io.InputStream

object BGRAFrame : CPUFrame() {

    override fun load(w: Int, h: Int, input: InputStream): Image {
        return IntImage(w, h, IntArray(w * h) {
            val b = input.read()
            val g = input.read()
            val r = input.read()
            val a = input.read()
            rgba(r, g, b, a)
        }, true)
    }

}