package me.anno.video.ffmpeg

import me.anno.Engine
import me.anno.audio.openal.SoundBuffer
import me.anno.audio.streams.AudioStream
import me.anno.cache.AsyncCacheData
import me.anno.cache.IgnoredException
import me.anno.io.BufferedIO.useBuffered
import me.anno.io.Streams.readNBytes2
import me.anno.io.files.FileReference
import me.anno.utils.ShutdownException
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.Buffers.flip16
import org.apache.logging.log4j.LogManager
import org.lwjgl.openal.AL11.AL_FORMAT_MONO16
import org.lwjgl.openal.AL11.AL_FORMAT_STEREO16
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.concurrent.thread

class FFMPEGAudio(
    file: FileReference?, val channels: Int, val sampleRate: Int, val duration: Double,
    val result: AsyncCacheData<SoundBuffer>
) : FFMPEGStream(file, false) {
    // audio should be fast -> not limited

    companion object {
        private val LOGGER = LogManager.getLogger(FFMPEGAudio::class)
    }

    init {
        if (channels !in 1..2) throw IllegalStateException("Unsupported number of audio channels: $channels")
    }

    override fun process(process: Process, arguments: List<String>, callback: () -> Unit) {
        // ("starting process for audio $sampleRate x $length")
        // (arguments)
        thread(name = "${file?.name}:error-stream") {
            val out = process.errorStream.bufferedReader()
            val parser = FFMPEGMetaParser()
            try {
                while (!Engine.shutdown) {
                    val line = out.readLine() ?: break
                    parser.parseLine(line, this)
                }
            } catch (_: ShutdownException) {
                // ...
            }
            out.close()
        }
        process.inputStream.useBuffered().use { input: InputStream ->
            val frameCount = (sampleRate * duration).toInt()
            val buffer = SoundBuffer()
            result.value = try {
                val (bytes, shorts, stereo) = readRAW(input, channels, frameCount)
                if (bytes.limit() == 0) { // EOF
                    ByteBufferPool.free(bytes)
                    LOGGER.warn("No data was available for $file")
                    null
                } else {
                    buffer.loadRaw16(
                        shorts, bytes, sampleRate,
                        if (stereo) AL_FORMAT_STEREO16 else AL_FORMAT_MONO16
                    )
                    buffer
                }
            } catch (_: IgnoredException) {
                null
            }
            callback()
        }
    }

    fun readRAW(input: InputStream, channels: Int, frameCount: Int): Triple<ByteBuffer, ShortBuffer, Boolean> {
        val sampleCount = frameCount * channels
        val size = sampleCount * 2
        val bytes = AudioStream.byteBufferPool.get(size, clear = false, exactMatchesOnly = false)
        val ex = input.readNBytes2(size, bytes, false)
        if (ex is Exception) throw ex
        if (bytes.position() > 0) bytes.flip()
        val shorts = bytes.asShortBuffer()
        if (shorts.order() == ByteOrder.BIG_ENDIAN) {
            bytes.flip16()
        }
        return Triple(bytes, shorts, channels == 2)
    }

    override fun destroy() {}
}