package me.anno.video.formats.cpu

import me.anno.image.raw.ByteImage
import me.anno.utils.types.InputStreams.readNBytes2
import java.io.InputStream

object BGRAFrame : CPUFrame() {
    override fun load(w: Int, h: Int, input: InputStream) =
        ByteImage(w, h, ByteImage.Format.BGRA, input.readNBytes2(w * h * 4, true))
}