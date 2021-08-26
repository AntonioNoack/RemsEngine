package me.anno.video

import me.anno.audio.AudioStreamRaw.Companion.playbackSliceDuration
import me.anno.gpu.GFX
import me.anno.io.files.FileReference
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.studio.rems.RemsStudio
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.process.BetterProcessBuilder
import me.anno.video.Codecs.audioCodecByExtension
import me.anno.video.FFMPEGUtils.processOutput
import org.apache.logging.log4j.LogManager
import java.io.DataOutputStream
import java.io.IOException
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.ceil

open class AudioCreator(
    val scene: Transform,
    private val durationSeconds: Double,
    private val sampleRate: Int,
    val audioSources: List<Audio>
) {

    val startTime = GFX.gameTime

    var onFinished = {}

    val camera: Camera

    init {
        val cameras = scene.listOfAll.filterIsInstance<Camera>()
        camera = cameras.firstOrNull() ?: RemsStudio.nullCamera ?: Camera()
    }

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
            processOutput(LOGGER, "Audio", startTime, targetFPS, totalFrameCount, process.errorStream)
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
            val bufferCount = ceil(durationSeconds / sliceDuration).toLong()

            val streams = audioSources.map { BufferStream(it, sampleRate, camera) }

            for (bufferIndex in 0 until bufferCount) {
                streams.forEach { it.requestNextBuffer(bufferIndex, 0) }
                val buffers = streams.map { it.getAndReplace() }
                val buffer = buffers.first()
                // write the data to ffmpeg
                val size = buffer.capacity()
                if (buffers.size == 1) {// no sum required
                    for (i in 0 until size) {
                        audioOutput.writeShort(buffer[i].toInt())
                    }
                } else {
                    for (i in 0 until size) {
                        val sum = buffers.sumOf { it[i].toInt() }
                        audioOutput.writeShort(clamp(sum, Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()))
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