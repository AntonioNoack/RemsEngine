package org.newdawn.slick.openal

import me.anno.utils.pooling.ByteBufferPool.Companion.allocateDirect
import org.apache.logging.log4j.LogManager.getLogger
import org.apache.logging.log4j.Logger
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException

// still used for loading wav files
// (when not working with ffmpeg)
class WaveData private constructor(val data: ByteBuffer, val format: Int, val sampleRate: Int) {

    fun dispose() {
        data.clear()
    }

    companion object {

        private val LOGGER: Logger = getLogger(WaveData::class.java)

        fun create(path: URL): WaveData? {
            return try {
                create(AudioSystem.getAudioInputStream(BufferedInputStream(path.openStream())))
            } catch (var2: Exception) {
                LOGGER.info("Unable to create from: $path")
                var2.printStackTrace()
                null
            }
        }

        fun create(path: String): WaveData? {
            val url = WaveData::class.java.classLoader.getResource(path) ?: return null
            return create(url)
        }

        @Throws(IOException::class, UnsupportedAudioFileException::class)
        private fun getAudioInputStream(`is`: InputStream): AudioInputStream {
            /*try {
            return new WaveFloatFileReader().getAudioInputStream(is);// we know the format
        } catch (UnsupportedAudioFileException e){
            return AudioSystem.getAudioInputStream(is);
        }*/
            return AudioSystem.getAudioInputStream(`is`)
        }

        /*public static WaveData create(InputStream is, int frameCount) {
            try {
                // new WaveReader(is, frameCount);
                WaveReader.INSTANCE.readWAV(is, frameCount);
                // return create(getAudioInputStream(is), frameCount);
            } catch (Exception e) {
                LOGGER.warn("Unable to create from inputstream", e);
                e.printStackTrace();
                return null;
            }
        }*/

        fun create(input: InputStream): WaveData? {
            return try {
                create(getAudioInputStream(input), -1)
            } catch (e: Exception) {
                LOGGER.warn("Unable to create from inputStream", e)
                e.printStackTrace()
                null
            }
        }

        fun create(buffer: ByteArray?): WaveData? {
            return try {
                create(AudioSystem.getAudioInputStream(BufferedInputStream(ByteArrayInputStream(buffer))))
            } catch (e: Exception) {
                LOGGER.warn("Unable to create from byte[]", e)
                e.printStackTrace()
                null
            }
        }

        fun create(buffer: ByteBuffer): WaveData? {
            return try {
                val bytes: ByteArray
                if (buffer.hasArray()) {
                    bytes = buffer.array()
                } else {
                    bytes = ByteArray(buffer.capacity())
                    buffer[bytes]
                }
                create(bytes)
            } catch (var2: Exception) {
                var2.printStackTrace()
                null
            }
        }

        fun create(ais: AudioInputStream, frameCount: Int): WaveData? {
            val audioFormat = ais.format
            val format = when (audioFormat.channels) {
                1 -> when (audioFormat.sampleSizeInBits) {
                    8 -> 4352
                    16 -> 4353
                    else -> throw RuntimeException("Illegal sample size")
                }
                2 -> when (audioFormat.sampleSizeInBits) {
                    8 -> 4354
                    16 -> 4355
                    else -> throw RuntimeException("Illegal sample size")
                }
                else -> throw RuntimeException("Only mono or stereo is supported")
            }

            // FFMPEG writes the wrong amount of frames into the file
            // we have to correct that; luckily we know the amount of frames
            val frameLength = if (frameCount > -1) frameCount else ais.frameLength.toInt()
            val byteLength = audioFormat.channels * frameLength * audioFormat.sampleSizeInBits / 8
            val buf = ByteArray(byteLength)
            var targetLength = 0
            try {
                var length: Int
                while (ais.read(buf, targetLength, buf.size - targetLength)
                        .also { length = it } != -1 && targetLength < buf.size
                ) {
                    targetLength += length
                }
            } catch (var10: IOException) {
                return null
            }
            val buffer = convertAudioBytes(buf, audioFormat.sampleSizeInBits == 16)
            val waveData = WaveData(buffer, format, audioFormat.sampleRate.toInt())
            try {
                ais.close()
            } catch (ignored: IOException) {
            }
            return waveData
        }

        private fun convertAudioBytes(audio_bytes: ByteArray, two_bytes_data: Boolean): ByteBuffer {
            val dst = allocateDirect(audio_bytes.size)
            // the source is little endian, correct?
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN || !two_bytes_data) {

                // it's fine from the start
                dst.put(audio_bytes)
            } else {
                val src = ByteBuffer.wrap(audio_bytes)
                src.order(ByteOrder.LITTLE_ENDIAN)
                val dstShort = dst.asShortBuffer()
                dstShort.put(src.asShortBuffer())
            }
            dst.flip()
            return dst
        }
    }
}