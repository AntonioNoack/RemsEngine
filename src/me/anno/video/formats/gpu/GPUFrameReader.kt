package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.io.files.FileReference
import me.anno.utils.ShutdownException
import me.anno.video.LastFrame
import me.anno.video.formats.FrameReader
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

class GPUFrameReader(
    file: FileReference, frame0: Int, bufferLength: Int,
    nextFrameCallback: (GPUFrame) -> Unit,
    finishedCallback: (List<GPUFrame>) -> Unit
) : FrameReader<GPUFrame>(file, frame0, bufferLength, nextFrameCallback, finishedCallback) {

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
                "BGR", "BGR[24]" -> BGRFrame(w, h)
                // bw
                "Y4", "Y800" -> Y4Frame(w, h) // seems correct, awkward, that it has the same name
                // to do PAL: to do decode somehow (if still needed; ico is no longer being loaded with ffmpeg); sample: pictures/fav128.ico
                else -> throw RuntimeException("Unsupported Codec $codec for $file")
            }
            frame.load(input)
            return frame
        } catch (e: EOFException) {
            // e.printStackTrace()
        } catch (e: LastFrame) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
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
                    GFX.addGPUTask("GPUFrameReader.destroy()", f0.width, f0.height) { frame.destroy() }
                }
            }
            frames.clear()
            isDestroyed = true
        }
    }

}