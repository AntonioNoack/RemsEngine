package me.anno.video.formats.cpu

import me.anno.image.raw.ByteImage
import me.anno.utils.types.InputStreams.readNBytes2
import java.io.InputStream

object RGBFrame : CPUFrame() {
    override fun load(w: Int, h: Int, input: InputStream) =
        ByteImage(w, h, ByteImage.Format.RGB, input.readNBytes2(w * h * 3, true))
}