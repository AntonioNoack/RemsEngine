package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.image.raw.ByteImage
import me.anno.image.raw.ByteImageFormat
import me.anno.io.Streams.readNBytes2
import java.io.InputStream

object RGBFrames {

    fun loadBGRAFrame(w: Int, h: Int, input: InputStream): Image {
        val bytes = input.readNBytes2(w * h * 4, true)
        val image = ByteImage(w, h, ByteImageFormat.BGRA, bytes)
        image.hasAlphaChannel = (bytes.indices step 4).any { bytes[it + 3].toInt() != -1 }
        return image
    }

    fun loadBGRFrame(w: Int, h: Int, input: InputStream): Image {
        return ByteImage(w, h, ByteImageFormat.BGR, input.readNBytes2(w * h * 3, true))
    }

    fun loadARGBFrame(w: Int, h: Int, input: InputStream): Image {
        val bytes = input.readNBytes2(w * h * 4, true)
        val image = ByteImage(w, h, ByteImageFormat.ARGB, bytes)
        image.hasAlphaChannel = (bytes.indices step 4).any { bytes[it].toInt() != -1 }
        return image
    }

    fun loadRGBFrame(w: Int, h: Int, input: InputStream): Image {
        return ByteImage(w, h, ByteImageFormat.RGB, input.readNBytes2(w * h * 3, true))
    }

    fun loadRGBAFrame(w: Int, h: Int, input: InputStream): Image {
        val bytes = input.readNBytes2(w * h * 4, true)
        val image = ByteImage(w, h, ByteImageFormat.RGBA, bytes)
        image.hasAlphaChannel = (bytes.indices step 4).any { bytes[it + 3].toInt() != 1 }
        return image
    }

    fun loadY4Frame(w: Int, h: Int, input: InputStream): Image {
        return ByteImage(w, h, ByteImageFormat.R, input.readNBytes2(w * h, true))
    }
}