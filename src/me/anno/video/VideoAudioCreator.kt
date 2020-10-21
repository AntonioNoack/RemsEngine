package me.anno.video

import me.anno.audio.AudioStream
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.studio.RemsStudio.root
import me.anno.utils.clamp
import org.apache.logging.log4j.LogManager
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.roundToInt

class VideoAudioCreator(
    val videoCreator: VideoCreator, val sampleRate: Int, val audioSources: List<Audio>, val output: File
) {

    var onFinished = {}

    lateinit var camera: Camera

    fun start() {
        thread { run() }
    }

    fun run() {
        val vbt = VideoBackgroundTask(videoCreator)
        camera = vbt.camera
        vbt.start()
        // wait for the task to finish
        while (!vbt.isDone) {
            Thread.sleep(1)
        }
        if (audioSources.isEmpty()) {
            if(output != videoCreator.output){
                output.delete()
                videoCreator.output.renameTo(output)
            }
            LOGGER.info("No audio found, saved result to $output.")
            onFinished()
        } else {
            appendAudio()
            onFinished()
        }
    }

    lateinit var audioOutput: DataOutputStream

    fun appendAudio() {

        output.delete()

        // http://crazedmuleproductions.blogspot.com/2005/12/using-ffmpeg-to-combine-audio-and.html
        // ffmpeg -i video.mp4 -i audio.wav -c:v copy -c:a aac output.mp4
        // add -shortest to use shortest...
        val audioEncodingArguments = arrayListOf(
            "-i", videoCreator.output.absolutePath,
            "-f", "s16be",
            "-ar", "$sampleRate",
            "-ac", "2",
            "-i", "pipe:0",
            "-c:v", "copy",
            "-c:a", "aac", // todo allow different audio codecs (if required...)
            "-shortest", // audio may be 0.999 buffers longer
            output.absolutePath
        )

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

        audioOutput = DataOutputStream(process.outputStream.buffered())

        createAudio()

        // delete the temporary file
        videoCreator.output.delete()

    }



    fun createAudio() {

        // todo automatically fade-in/fade-out the audio at the start and end?

        val durationSeconds = videoCreator.totalFrameCount / videoCreator.fps
        val totalSampleCount = (durationSeconds * sampleRate).roundToInt()

        // collect all audio from all audio sources
        // todo optimize to use only playing ones (if not too complex)

        try {

            val sliceDuration = AudioStream.playbackSliceDuration
            val sliceSampleCount = (sliceDuration * sampleRate).roundToInt()

            val buffer = ShortBuffer.allocate(sliceSampleCount * 2)

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
                    Thread.sleep(1)
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

        LOGGER.info("Saved video with audio to $output")

    }

    companion object {
        private val LOGGER = LogManager.getLogger(VideoAudioCreator::class)
    }

}