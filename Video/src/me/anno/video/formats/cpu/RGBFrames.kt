package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.image.raw.ByteImage
import me.anno.utils.types.InputStreams.readNBytes2
import java.io.InputStream

object RGBFrames {

    fun loadBGRAFrame(w: Int, h: Int, input: InputStream): Image {
        val bytes = input.readNBytes2(w * h * 4, true)
        val image = ByteImage(w, h, ByteImage.Format.BGRA, bytes)
        image.hasAlphaChannel = (bytes.indices step 4).any { bytes[it + 3].toInt() != -1 }
        return image
    }

    fun loadBGRFrame(w: Int, h: Int, input: InputStream): Image {
        return ByteImage(w, h, ByteImage.Format.BGR, input.readNBytes2(w * h * 3, true))
    }

    fun loadARGBFrame(w: Int, h: Int, input: InputStream): Image {
        val bytes = input.readNBytes2(w * h * 4, true)
        val image = ByteImage(w, h, ByteImage.Format.ARGB, bytes)
        image.hasAlphaChannel = (bytes.indices step 4).any { bytes[it].toInt() != -1 }
        return image
    }

    fun loadRGBFrame(w: Int, h: Int, input: InputStream): Image {
        return ByteImage(w, h, ByteImage.Format.RGB, input.readNBytes2(w * h * 3, true))
    }

    fun loadRGBAFrame(w: Int, h: Int, input: InputStream): Image {
        val bytes = input.readNBytes2(w * h * 4, true)
        val image = ByteImage(w, h, ByteImage.Format.RGBA, bytes)
        image.hasAlphaChannel = (bytes.indices step 4).any { bytes[it + 3].toInt() != 1 }
        return image
    }

    fun loadY4Frame(w: Int, h: Int, input: InputStream): Image {
        return ByteImage(w, h, ByteImage.Format.R, input.readNBytes2(w * h, true))
    }
}