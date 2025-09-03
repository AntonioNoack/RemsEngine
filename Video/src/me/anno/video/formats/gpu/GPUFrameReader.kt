package me.anno.video.formats.gpu

import me.anno.cache.IgnoredException
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.io.files.FileReference
import me.anno.utils.assertions.assertFail
import me.anno.utils.async.Callback
import me.anno.video.ffmpeg.FrameReader
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

class GPUFrameReader(
    file: FileReference, frame0: Int, bufferLength: Int,
    nextFrameCallback: (GPUFrame) -> Unit,
    finishedCallback: (List<GPUFrame>) -> Unit
) : FrameReader<GPUFrame>(file, frame0, bufferLength, nextFrameCallback, finishedCallback) {

    override fun readFrame(w: Int, h: Int, frameIndex: Int, input: InputStream, callback: Callback<GPUFrame>) {
        var frame: GPUFrame? = null
        try {
            frame = createGPUFrame(w, h, frameIndex, codec, file)
            frame.load(input, callback)
            return // load-method must call callback
        } catch (_: EOFException) {
            // e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (_: IgnoredException) {
        } catch (e: Exception) {
            e.printStackTrace()
        }
        frame?.destroy()
        return callback.err(null)
    }

    override fun destroy() {
        synchronized(frames) {
            if (frames.isNotEmpty()) {
                val f0 = frames[0]
                // delete them over time? it seems like it's really expensive on my Envy x360 xD
                for (frame in frames) {
                    addGPUTask("GPUFrameReader.destroy()", f0.width, f0.height) { frame.destroy() }
                }
            }
            frames.clear()
            isDestroyed = true
        }
    }

    companion object {
        fun createGPUFrame(w: Int, h: Int, frameIndex: Int, codec: String, file: FileReference?): GPUFrame {
            val frame = when (codec) {
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
                else -> assertFail("Unsupported Codec $codec for $file")
            }
            frame.frameIndex = frameIndex
            return frame
        }
    }
}