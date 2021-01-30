package me.anno.video

import me.anno.objects.Audio
import me.anno.utils.Sleep.sleepShortly
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.concurrent.thread

class VideoAudioCreator(
    val videoCreator: VideoCreator,
    durationSeconds: Double, sampleRate: Int, audioSources: List<Audio>,
    val motionBlurSteps: Int, val shutterPercentage: Float,
    val output: File
): AudioCreator(durationSeconds, sampleRate, audioSources) {

    fun start() {
        thread { run() }
    }

    fun run() {
        val vbt = VideoBackgroundTask(videoCreator, camera, motionBlurSteps, shutterPercentage)
        vbt.start()
        // wait for the task to finish
        while (!vbt.isDone) {
            sleepShortly()
        }
        if (audioSources.isEmpty()) {
            if (output != videoCreator.output) {
                output.delete()
                videoCreator.output.renameTo(output)
            }
            LOGGER.info("No audio found, saved result to $output.")
            onFinished()
        } else {
            createOrAppendAudio(output, videoCreator)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(VideoAudioCreator::class)
    }

}