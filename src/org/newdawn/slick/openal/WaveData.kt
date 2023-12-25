package org.newdawn.slick.openal

import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.pooling.ByteBufferPool.Companion.allocateDirect
import me.anno.utils.types.InputStreams.readNBytes2
import org.apache.logging.log4j.LogManager.getLogger
import org.apache.logging.log4j.Logger
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioSystem.getAudioInputStream

// still used for loading wav files
// (when not working with ffmpeg)
class WaveData private constructor(var data: ByteBuffer?, val format: Int, val sampleRate: Int) {

    fun destroy() {
        ByteBufferPool.free(data)
        data = null
    }

    companion object {
        @JvmStatic
        private val LOGGER: Logger = getLogger(WaveData::class)
        @JvmStatic
        fun create(input: InputStream): WaveData? {
            return try {
                val ais = getAudioInputStream(input)
                val audioFormat = ais.format
                if (audioFormat.channels !in 1..2)
                    throw IOException("Only mono or stereo is supported")
                if (audioFormat.sampleSizeInBits != 8 && audioFormat.sampleSizeInBits != 16)
                    throw IOException("Illegal sample size")
                // hopefully this is correct... should be :)
                val openALFormat = 4349 + audioFormat.channels * 2 + (audioFormat.sampleSizeInBits shr 8)
                // FFMPEG writes the wrong amount of frames into the file
                // we have to correct that; luckily we know the amount of frames
                val frameLength = ais.frameLength.toInt()
                val byteLength = audioFormat.channels * frameLength * audioFormat.sampleSizeInBits / 8
                val buffer = allocateDirect(byteLength)
                // if mono, or little endian, directly read into buffer
                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN || audioFormat.sampleRate.toInt() == 8) {
                    ais.readNBytes2(byteLength, buffer, false)
                } else {// flip byte order
                    val byteArray = ais.readNBytes2(byteLength, false)
                    val src = ByteBuffer.wrap(byteArray)
                    src.order(ByteOrder.LITTLE_ENDIAN)
                    val dstShort = buffer.asShortBuffer()
                    dstShort.put(src.asShortBuffer())
                    buffer.flip()
                }
                val waveData = WaveData(buffer, openALFormat, audioFormat.sampleRate.toInt())
                try {
                    ais.close()
                } catch (ignored: IOException) {
                }
                return waveData
            } catch (e: IOException) {
                LOGGER.warn("Unable to create from inputStream", e)
                e.printStackTrace()
                null
            }
        }
    }
}