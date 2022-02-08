package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.io.files.FileReference
import me.anno.video.LastFrame
import me.anno.video.formats.FrameReader
import java.io.IOException
import java.io.InputStream

class CPUFrameReader(
    file: FileReference,
    frame0: Int,
    bufferLength: Int
) : FrameReader<Image>(file, frame0, bufferLength) {

    override fun readFrame(w: Int, h: Int, input: InputStream): Image? {
        try {
            val frame = when (codec) {
                // yuv
                "I420" -> I420Frame
                "444P" -> I444Frame
                // rgb
                "ARGB" -> ARGBFrame
                "BGRA" -> BGRAFrame
                "RGBA" -> RGBAFrame
                "RGB" -> RGBFrame
                "BGR" -> BGRFrame
                // bw
                "Y4" -> Y4Frame
                "Y800" -> Y4Frame // seems correct, awkward, that it has the same name
                // todo PAL: todo find video with PAL output
                else -> throw RuntimeException("Unsupported Codec $codec for $file")
            }
            return frame.load(w, h, input)
        } catch (e: LastFrame) {

        } catch (e: IOException) {

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