package me.anno.video.formats.gpu

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Texture2D
import me.anno.io.Streams.readNBytes2
import me.anno.io.files.FileReference
import me.anno.utils.assertions.assertEquals
import me.anno.utils.async.Callback
import me.anno.utils.pooling.Pools
import me.anno.video.ffmpeg.FrameReader
import org.apache.logging.log4j.LogManager
import java.io.InputStream

class BGRATextureReader(
    file: FileReference, frame0: Int, bufferLength: Int,
    nextFrameCallback: (Texture2D) -> Unit,
    finishedCallback: (List<Texture2D>) -> Unit,
) : FrameReader<Texture2D>(file, frame0, bufferLength, nextFrameCallback, finishedCallback) {

    override fun readFrame(w: Int, h: Int, frameIndex: Int, input: InputStream, callback: Callback<Texture2D>) {
        try {
            assertEquals("BGRA", codec)
            loadBGRA(input, w, h, callback)
            // load-method must call callback
        } catch (e: Exception) {
            return callback.err(e)
        }
    }

    private fun loadBGRA(input: InputStream, width: Int, height: Int, callback: Callback<Texture2D>) {
        try {
            val data = input.readNBytes2(width * height * 4, Pools.byteBufferPool)
                ?: return callback.err(null)

            val name = file?.absolutePath ?: "BGRATextureReader"
            val rgb = Texture2D(name, width, height, 1)
            addGPUTask(name, width, height) {
                if (!isDestroyed && !rgb.isDestroyed) {
                    rgb.createBGRA(data, false)
                } else LogManager.getLogger(this::class)
                    .warn("Frame is already destroyed!")
                callback.ok(rgb)
            }
        } catch (e: Exception) {
            callback.err(e)
        }
    }

    override fun destroy() {
        synchronized(frames) {
            if (frames.isNotEmpty()) {
                val f0 = frames[0]
                // delete them over time? it seems like it's really expensive on my Envy x360 xD
                for (frame in frames) {
                    addGPUTask("Texture2DReader.destroy()", f0.width, f0.height) { frame.destroy() }
                }
            }
            frames.clear()
            isDestroyed = true
        }
    }

}