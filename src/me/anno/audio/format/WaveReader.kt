package me.anno.audio.format

import me.anno.utils.readNBytes
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.InputStream
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

/**
 * a class to read wav files specifically from ffmpeg,
 * therefore it doesn't need to be flexible
 *
 * I would have used the Slick library, but it depends on suns AudioSystem,
 * which has a bug previous to Java 9
 * */
// audio crashes at 25s for no reason -.-
// -> MemoryUtil.memFree(toFree) was the cause
// reason, probably: GC frees buffer, even tho it's required...
// file : File(OS.home, Pictures\Video Projects\UE4 AI Bottleneck.mp4)
// only in Java 8 (100%, 3x), not in Java 12 (0%, 2x)
class WaveReader(val input: InputStream, frameCount: Int) {
    
    companion object {
        var debug = false
        private val LOGGER = LogManager.getLogger(WaveReader::class)
    }

    val stereoPCM: ShortBuffer

    init {

        if(debug) LOGGER.info("size: $frameCount")

        val tag = readTag()
        if(tag != "RIFF") throw RuntimeException()

        val fileSize = readInt()
        if(debug) LOGGER.info("size: $fileSize")

        val fileType = readTag()
        if(fileType != "WAVE") throw RuntimeException()

        val formatTag = readTag()
        if(formatTag != "fmt ") throw RuntimeException()

        val bitsPerSampleEncoded = readInt()
        if(debug) LOGGER.info("$bitsPerSampleEncoded bits/sample encoded")

        val formatType = readShort()
        if(debug) LOGGER.info("format: $formatType, 1=pcm")

        val channels = readShort()
        if(debug) LOGGER.info("channels: $channels")

        val sampleRate = readInt()
        if(debug) LOGGER.info("sample rate: $sampleRate")

        val controlValue = readInt()
        if(debug) LOGGER.info("control value: $controlValue = ${sampleRate * bitsPerSampleEncoded * channels / 8}")

        val bytesPerChannelSample = readShort()
        if(debug) LOGGER.info("bytes per channel sample: $bytesPerChannelSample = ${bitsPerSampleEncoded * channels / 8}")

        val bitsPerSample2 = readShort()
        if(debug) LOGGER.info("$bitsPerSample2 bits/sample raw")

        var dataTag = readTag()
        while(dataTag != "data"){
            when(dataTag){
                "LIST" -> {
                    val length = readInt()
                    when(val type = readTag()){
                        "INFO" -> {// we don't really care...
                            // val data = readNBytes(length - 4)
                            input.skip(length - 4L)
                        }
                        else -> throw RuntimeException("Unknown wav chunk: LIST.$type")
                    }
                }
                else -> throw RuntimeException("Unknown wav chunk: $dataTag")
            }
            dataTag = readTag()
        }

        val dataSize = readInt()
        if(debug) LOGGER.info("size of the data $dataSize")

        // allocating $frameCount * 4 bytes

        val byteBuffer = ByteBuffer.allocateDirect(frameCount * 4)
            .order(ByteOrder.nativeOrder())

        stereoPCM = byteBuffer.asShortBuffer()
        stereoPCM.position(0)
        try {
            when(channels){
                1 -> {// duplicate the data for left+right
                    for(i in 0 until frameCount){
                        val value = readShort().toShort()
                        stereoPCM.put(value)
                        stereoPCM.put(value)
                    }
                }
                2 -> {// copy 1:1
                    for(i in 0 until 2 * frameCount){
                        stereoPCM.put(readShort().toShort())
                    }
                }
                else -> throw RuntimeException("Unsupported count of audio channels: $channels")
            }
        } catch (e: EOFException){}
        stereoPCM.position(0)

    }

    fun readTag() = String(readNBytes(4))

    fun readNBytes(n: Int) = input.readNBytes(n)

    fun readShort(): Int {
        val a = input.read()
        val b = input.read()
        if(a < 0 || b < 0) throw EOFException()
        return a + b.shl(8)
    }

    fun readInt(): Int {
        return readShort() + readShort().shl(16)
    }

}