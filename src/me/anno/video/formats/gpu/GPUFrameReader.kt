package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.io.files.FileReference
import me.anno.utils.ShutdownException
import me.anno.video.LastFrame
import me.anno.video.formats.FrameReader
import java.io.IOException
import java.io.InputStream

class GPUFrameReader(
    file: FileReference,
    frame0: Int,
    bufferLength: Int
) : FrameReader<GPUFrame>(file, frame0, bufferLength) {

    override fun readFrame(w: Int, h: Int, input: InputStream): GPUFrame? {
        var frame: GPUFrame? = null
        try {
            frame = when (codec) {
                // yuv
                "I420" -> I420Frame(w, h)
                "444P" -> I444Frame(w, h)
                // rgb
                "ARGB" -> ARGBFrame(w, h)
                "BGRA" -> BGRAFrame(w, h)
                "RGBA" -> RGBAFrame(w, h)
                "RGB" -> RGBFrame(w, h)
                "BGR" -> BGRFrame(w, h)
                // bw
                "Y4" -> Y4Frame(w, h)
                "Y800" -> Y4Frame(w, h) // seems correct, awkward, that it has the same name
                // todo PAL
                else -> throw RuntimeException("Unsupported Codec $codec for $file")
            }
            frame.load(input)
            return frame
        } catch (_: LastFrame) {

        } catch (_: IOException) {

        } catch (_: ShutdownException) {

        } catch (e: Exception) {
            e.printStackTrace()
        }
        frame?.destroy()
        return null
    }

    override fun destroy() {
        synchronized(frames) {
            if (frames.isNotEmpty()) {
                val f0 = frames[0]
                // delete them over time? it seems like it's really expensive on my Envy x360 xD
                for (frame in frames) {
                    GFX.addGPUTask("GPUFrameReader.destroy()", f0.w, f0.h) { frame.destroy() }
                }
            }
            frames.clear()
            isDestroyed = true
        }
    }

}