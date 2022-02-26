package me.anno.video

import me.anno.Engine
import me.anno.audio.streams.AudioStream
import me.anno.audio.streams.AudioStreamRaw.Companion.bufferSize
import me.anno.io.files.FileReference
import me.anno.maths.Maths.clamp
import me.anno.utils.process.BetterProcessBuilder
import me.anno.video.Codecs.audioCodecByExtension
import me.anno.video.ffmpeg.FFMPEG
import me.anno.video.ffmpeg.FFMPEGUtils.processOutput
import org.apache.logging.log4j.LogManager
import java.io.DataOutputStream
import java.io.IOException
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.roundToInt

abstract class AudioCreator(
    private val durationSeconds: Double,
    val sampleRate: Int
) {

    val startTime = Engine.gameTime
    var onFinished = {}

    abstract fun hasStreams(): Boolean

    abstract fun createStreams(): List<AudioStream>

    fun createOrAppendAudio(output: FileReference, videoCreatorOutput: FileReference?, deleteVCO: Boolean) {

        output.delete()

        // todo allow different audio codecs (if required...)
        // quality:
        // libopus > libvorbis >= libfdk_aac > libmp3lame >= eac3/ac3 > aac > libtwolame > vorbis (dont) > mp2 > wmav2/wmav1 (dont)
        val audioCodec = audioCodecByExtension(output.extension) ?: return

        // http://crazedmuleproductions.blogspot.com/2005/12/using-ffmpeg-to-combine-audio-and.html
        // ffmpeg -i video.mp4 -i audio.wav -c:v copy -c:a aac output.mp4
        // add -shortest to use shortest...
        val rawFormat = "s16be"// signed, 16 bit, big endian
        val channels = "2" // stereo
        val audioEncodingArguments = if (videoCreatorOutput != null) {
            arrayListOf(
                "-i", videoCreatorOutput.absolutePath,
                "-f", rawFormat,
                "-ar", sampleRate.toString(),
                "-ac", channels,
                "-i", "pipe:0", // output stream
                "-c:v", "copy", // video is just copied 1:1
                "-c:a", audioCodec,
                "-map", "0:v:0", // map first video stream to output
                "-map", "1:a:0", // map second audio stream to output
                "-shortest", // audio may be 0.999 buffers longer
                output.absolutePath
            )
        } else {
            arrayListOf(
                "-f", rawFormat,
                "-ar", sampleRate.toString(),
                "-ac", channels,
                "-i", "pipe:0", // output stream
                "-c:a", audioCodec,
                output.absolutePath
            )
        }

        val builder = BetterProcessBuilder(FFMPEG.ffmpegPathString, audioEncodingArguments.size + 2, true)
        if (audioEncodingArguments.isNotEmpty()) builder += "-hide_banner"
        builder += audioEncodingArguments

        val process = builder.start()
        val targetFPS = 60.0
        val totalFrameCount = (targetFPS * durationSeconds).toLong()
        thread(name = "AudioOutputListener") {
            processOutput(LOGGER, "Audio", startTime, targetFPS, totalFrameCount, process.errorStream) {
                onFinished()
            }
        }

        val audioOutput = DataOutputStream(process.outputStream.buffered())
        createAudio(audioOutput)

        LOGGER.info(if (videoCreatorOutput != null) "Saved video with audio to $output" else "Saved audio to $output")

        // delete the temporary file
        //
        if (videoCreatorOutput != null && deleteVCO) {
            // temporary file survives sometimes
            // -> kill it at the end at the very least
            if (!videoCreatorOutput.delete()) videoCreatorOutput.deleteOnExit()
        }
        onFinished()

    }

    fun createAudio(audioOutput: DataOutputStream) {

        // todo automatically fade-in/fade-out the audio at the start and end?

        // val totalSampleCount = (durationSeconds * sampleRate).roundToInt()

        // collect all audio from all audio sources
        // todo optimize to use only playing ones (if not too complex)

        try {


            val sliceDuration = playbackSliceDuration
            val bufferSize = (sliceDuration * sampleRate * 2).roundToInt()
            val bufferCount = ceil(durationSeconds / sliceDuration).toLong()

            val streams = createStreams()

            var intBuffer: IntArray? = null

            for (bufferIndex in 0 until bufferCount) {
                for (stream in streams) {
                    stream.requestNextBuffer(bufferIndex, 0)
                }
                if (streams.size == 1) {
                    // no sum required
                    // write the data to ffmpeg
                    val buffer = streams[0].getNextBuffer()
                    for (i in 0 until buffer.capacity()) {
                        audioOutput.writeShort(buffer[i].toInt())
                    }
                } else {
                    for (j in streams.indices) {
                        val buffer = streams[j].getNextBuffer()
                        if (j == 0) {
                            if (intBuffer == null || intBuffer.size != buffer.capacity()) {
                                intBuffer = IntArray(buffer.capacity())
                            } else {
                                intBuffer.fill(0)
                            }
                        }
                        intBuffer!!
                        if (buffer.capacity() != bufferSize) throw RuntimeException("${buffer.capacity()} vs $bufferSize")
                        for (i in 0 until bufferSize) {
                            intBuffer[i] += buffer[i].toInt()
                        }
                    }
                    if (intBuffer != null) {
                        val min = Short.MIN_VALUE.toInt()
                        val max = Short.MAX_VALUE.toInt()
                        for (i in 0 until bufferSize) {
                            audioOutput.writeShort(clamp(intBuffer[i], min, max))
                        }
                    } else {
                        // no stream was available
                        for (i in 0 until bufferSize) {
                            audioOutput.writeShort(0)
                        }
                    }

                }
            }

            audioOutput.flush()
            audioOutput.close()

        } catch (e: IOException) {
            val msg = e.message!!
            // pipe has been ended will be thrown, if we write more audio bytes than required
            // this really isn't an issue xD
            if ("pipe has been ended" !in msg.lowercase(Locale.getDefault()) &&
                "pipe is being closed" !in msg.lowercase(Locale.getDefault())
            ) throw e
        }

    }

    companion object {
        const val playbackSampleRate = 48000

        // 1024 (48Hz .. 48kHz) or 2048? (24Hz .. 48kHz)
        val playbackSliceDuration = bufferSize.toDouble() / playbackSampleRate
        private val LOGGER = LogManager.getLogger(AudioCreator::class)
    }

}