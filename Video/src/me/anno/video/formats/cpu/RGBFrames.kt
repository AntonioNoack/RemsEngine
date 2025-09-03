package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.image.raw.ByteImage
import me.anno.image.raw.ByteImageFormat
import me.anno.io.Streams.readNBytes2
import me.anno.utils.pooling.Pools
import java.io.InputStream

object RGBFrames {

    private fun InputStream.readNBytes3(size: Int): ByteArray? {
        val bytes = Pools.byteArrayPool[size, false, false]
        val check = readNBytes2(size, bytes, true)
        if (check == null) Pools.byteArrayPool.returnBuffer(bytes)
        return check
    }

    fun loadBGRAFrame(w: Int, h: Int, input: InputStream): Image? {
        val bytes = input.readNBytes3(w * h * 4) ?: return null
        val image = ByteImage(w, h, ByteImageFormat.BGRA, bytes)
        image.hasAlphaChannel = (bytes.indices step 4).any { bytes[it + 3].toInt() != -1 }
        return image
    }

    fun loadBGRFrame(w: Int, h: Int, input: InputStream): Image? {
        val bytes = input.readNBytes3(w * h * 3) ?: return null
        return ByteImage(w, h, ByteImageFormat.BGR, bytes)
    }

    fun loadARGBFrame(w: Int, h: Int, input: InputStream): Image? {
        val bytes = input.readNBytes3(w * h * 4) ?: return null
        val image = ByteImage(w, h, ByteImageFormat.ARGB, bytes)
        image.hasAlphaChannel = (bytes.indices step 4).any { bytes[it].toInt() != -1 }
        return image
    }

    fun loadRGBFrame(w: Int, h: Int, input: InputStream): Image? {
        val bytes = input.readNBytes3(w * h * 3) ?: return null
        return ByteImage(w, h, ByteImageFormat.RGB, bytes)
    }

    fun loadRGBAFrame(w: Int, h: Int, input: InputStream): Image? {
        val bytes = input.readNBytes3(w * h * 4) ?: return null
        val image = ByteImage(w, h, ByteImageFormat.RGBA, bytes)
        image.hasAlphaChannel = (bytes.indices step 4).any { bytes[it + 3].toInt() != 1 }
        return image
    }

    fun loadY4Frame(w: Int, h: Int, input: InputStream): Image? {
        val bytes = input.readNBytes3(w * h) ?: return null
        return ByteImage(w, h, ByteImageFormat.R, bytes)
    }
}