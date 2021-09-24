package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.utils.Color
import java.io.InputStream

object ARGBFrame : CPUFrame() {

    override fun load(w: Int, h: Int, input: InputStream): Image {
        return IntImage(w, h, IntArray(w * h) {
            val a = input.read()
            val r = input.read()
            val g = input.read()
            val b = input.read()
            Color.rgba(r, g, b, a)
        }, true)
    }

}