package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.io.files.FileReference
import me.anno.video.ffmpeg.FrameReader
import me.anno.video.formats.cpu.RGBFrames.loadARGBFrame
import me.anno.video.formats.cpu.RGBFrames.loadBGRAFrame
import me.anno.video.formats.cpu.RGBFrames.loadBGRFrame
import me.anno.video.formats.cpu.RGBFrames.loadRGBAFrame
import me.anno.video.formats.cpu.RGBFrames.loadRGBFrame
import me.anno.video.formats.cpu.RGBFrames.loadY4Frame
import me.anno.video.formats.cpu.YUVFrames.loadI420Frame
import me.anno.video.formats.cpu.YUVFrames.loadI444Frame
import java.io.IOException
import java.io.InputStream

class CPUFrameReader(
    file: FileReference, frame0: Int, bufferLength: Int,
    nextFrameCallback: (Image) -> Unit,
    finishedCallback: (List<Image>) -> Unit
) : FrameReader<Image>(file, frame0, bufferLength, nextFrameCallback, finishedCallback) {

    override fun readFrame(w: Int, h: Int, input: InputStream): Image? {
        try {
            val frame = when (codec) {
                // yuv
                "I420" -> loadI420Frame(w, h, input)
                "444P" -> loadI444Frame(w, h, input)
                // rgb
                "ARGB" -> loadARGBFrame(w, h, input)
                "BGRA" -> loadBGRAFrame(w, h, input)
                "RGBA" -> loadRGBAFrame(w, h, input)
                "RGB" -> loadRGBFrame(w, h, input)
                "BGR", "BGR[24]" -> loadBGRFrame(w, h, input)
                // bw
                "Y4", "Y800" -> loadY4Frame(w, h, input) // seems correct, awkward, that it has the same name
                else -> throw RuntimeException("Unsupported Codec $codec for $file")
            }
            return frame
        } catch (_: IOException) {

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun destroy() {
        frames.clear()
        isDestroyed = true
    }
}