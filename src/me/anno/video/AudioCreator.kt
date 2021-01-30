package me.anno.video

import me.anno.audio.AudioStream
import me.anno.audio.effects.SoundPipeline
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.root
import me.anno.utils.Sleep.sleepShortly
import org.apache.logging.log4j.LogManager
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.ceil

open class AudioCreator(
    private val durationSeconds: Double,
    private val sampleRate: Int,
    val audioSources: List<Audio>
) {

    var onFinished = {}

    val camera: Camera
    init {
        val cameras = root.listOfAll.filterIsInstance<Camera>()
        camera = cameras.firstOrNull() ?: RemsStudio.nullCamera ?: Camera()
    }

    fun createOrAppendAudio(output: File, videoCreator: VideoCreator?) {

        output.delete()

        // todo allow different audio codecs (if required...)
        val audioCodec = when (output.extension.toLowerCase()) {
            "mp3" -> "libmp3lame"
            else -> "aac"
        }

        // http://crazedmuleproductions.blogspot.com/2005/12/using-ffmpeg-to-combine-audio-and.html
        // ffmpeg -i video.mp4 -i audio.wav -c:v copy -c:a aac output.mp4
        // add -shortest to use shortest...
        val audioEncodingArguments = if (videoCreator != null) {
            listOf(
                "-i", videoCreator.output.absolutePath,
                "-f", "s16be",
                "-ar", "$sampleRate",
                "-ac", "2", // stereo
                "-i", "pipe:0",
                "-c:v", "copy",
                "-c:a", audioCodec,
                "-shortest", // audio may be 0.999 buffers longer
                output.absolutePath
            )
        } else {
            listOf(
                "-f", "s16be",
                "-ar", "$sampleRate",
                "-ac", "2", // stereo
                "-i", "pipe:0",
                "-c:a", audioCodec,
                output.absolutePath
            )
        }

        val args = ArrayList<String>(audioEncodingArguments.size + 2)
        args += FFMPEG.ffmpegPathString
        if (audioEncodingArguments.isNotEmpty()) args += "-hide_banner"
        args += audioEncodingArguments
        val process = ProcessBuilder(args).start()
        thread {
            val out = process.errorStream.bufferedReader()
            while (true) {
                val line = out.readLine() ?: break
                LOGGER.info("[FFMPEG-Debug]: $line")
            }
        }

        val audioOutput = DataOutputStream(process.outputStream.buffered())
        createAudio(audioOutput)

        LOGGER.info(if (videoCreator != null) "Saved video with audio to $output" else "Saved audio to $output")

        // delete the temporary file
        videoCreator?.output?.delete()
        onFinished()

    }

    fun createAudio(audioOutput: DataOutputStream) {

        // todo automatically fade-in/fade-out the audio at the start and end?

        // val totalSampleCount = (durationSeconds * sampleRate).roundToInt()

        // collect all audio from all audio sources
        // todo optimize to use only playing ones (if not too complex)

        try {

            val sliceDuration = AudioStream.playbackSliceDuration
            val sampleBuffers =
                ceil(playbackSampleRate * AudioStream.playbackSliceDuration / SoundPipeline.bufferSize).toInt()
            val sampleCount = sampleBuffers * SoundPipeline.bufferSize

            val buffer = ShortBuffer.allocate(sampleCount * 2)

            val bufferCount = ceil(durationSeconds / sliceDuration).toLong()

            val streamFillCounter = AtomicInteger()
            val streams = audioSources.map {
                BufferStream(it, sampleRate, buffer, camera, streamFillCounter)
            }

            for (bufferIndex in 0 until bufferCount) {
                streamFillCounter.set(0)
                val startTime = bufferIndex * sliceDuration
                streams.forEach {
                    it.requestNextBuffer(startTime, bufferIndex)
                }
                while (streamFillCounter.get() < streams.size) {
                    sleepShortly()
                }
                // write the data to ffmpeg
                val size = buffer.capacity()
                for (i in 0 until size) {
                    audioOutput.writeShort(buffer[i].toInt())
                }
            }

            audioOutput.flush()
            audioOutput.close()

        } catch (e: IOException) {
            val msg = e.message!!
            // pipe has been ended will be thrown, if we write more audio bytes than required
            // this really isn't an issue xD
            if ("pipe has been ended" !in msg.toLowerCase() &&
                "pipe is being closed" !in msg.toLowerCase()
            ) {
                throw e
            }
        }

    }

    companion object {
        const val playbackSampleRate = 48000
        private val LOGGER = LogManager.getLogger(AudioCreator::class)
    }

}