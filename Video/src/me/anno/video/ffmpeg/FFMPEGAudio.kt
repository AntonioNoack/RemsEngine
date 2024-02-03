package me.anno.video.ffmpeg

import me.anno.Engine
import me.anno.audio.openal.SoundBuffer
import me.anno.cache.AsyncCacheData
import me.anno.cache.IgnoredException
import me.anno.io.BufferedIO.useBuffered
import me.anno.io.Streams.readLE16
import me.anno.io.files.FileReference
import me.anno.utils.ShutdownException
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.InputStreams.readNBytes2
import org.apache.logging.log4j.LogManager
import org.lwjgl.openal.AL11.AL_FORMAT_MONO16
import org.lwjgl.openal.AL11.AL_FORMAT_STEREO16
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.concurrent.thread

class FFMPEGAudio(file: FileReference?, val channels: Int, val sampleRate: Int, val duration: Double) :
    FFMPEGStream(file, false) {
    // audio should be fast -> not limited

    companion object {
        private val LOGGER = LogManager.getLogger(FFMPEGAudio::class)
    }

    override fun process(process: Process, vararg arguments: String) {
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
            input.mark(1)
            if (input.read() >= 0) {
                input.reset()
                val buffer = SoundBuffer()
                try {
                    val (bytes, shorts, stereo) = readRAW(input, channels, frameCount)
                    buffer.loadRaw16(
                        shorts, bytes, sampleRate,
                        if (stereo) AL_FORMAT_STEREO16 else AL_FORMAT_MONO16
                    )
                    result.value = buffer
                } catch (_: IgnoredException) {
                    result.value = null
                }
            } else {// EOF
                LOGGER.warn("No data was available for $file")
                result.value = null
            }
        }
    }

    val result = AsyncCacheData<SoundBuffer>()

    fun readRAW(input: InputStream, channels: Int, frameCount: Int): Triple<ByteBuffer, ShortBuffer, Boolean> {

        val sampleCount = frameCount * channels
        val size = sampleCount * 2
        val bytes = ByteBufferPool.allocateDirect(size)
        val shorts = bytes.asShortBuffer()
        if (channels !in 1..2) throw RuntimeException("Unsupported number of audio channels: $channels")

        when (channels) {
            1 -> {

                if (shorts.order() == ByteOrder.LITTLE_ENDIAN) {// fast path
                    input.readNBytes2(size, bytes, false)
                } else {// if our program is ever executed on a big endian machine
                    for (i in 0 until sampleCount) {
                        shorts.put(input.readLE16().toShort())
                    }
                }

                if (bytes.position() > 0) bytes.flip()
                if (shorts.position() > 0) shorts.flip()

                return Triple(bytes, shorts, false)
            }
            2 -> {

                if (shorts.order() == ByteOrder.LITTLE_ENDIAN) {// fast path
                    input.readNBytes2(size, bytes, false)
                } else {// if our program is ever executed on a big endian machine
                    for (i in 0 until sampleCount) {
                        shorts.put(input.readLE16().toShort())
                    }
                }

                if (bytes.position() > 0) bytes.flip()
                if (shorts.position() > 0) shorts.flip()

                return Triple(bytes, shorts, true)
            }
            else -> throw IllegalStateException()
        }
    }

    override fun destroy() {}
}