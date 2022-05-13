package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.image.raw.ByteImage
import me.anno.utils.types.InputStreams.readNBytes2
import java.io.InputStream

object Y4Frame : CPUFrame() {

    override fun load(w: Int, h: Int, input: InputStream): Image {
        return ByteImage(w, h, 1, input.readNBytes2(w * h, true))
    }

}