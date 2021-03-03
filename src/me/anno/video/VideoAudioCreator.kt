package me.anno.video

import me.anno.objects.Audio
import me.anno.objects.Transform
import me.anno.utils.Sleep.waitUntil
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.concurrent.thread

class VideoAudioCreator(
    val videoCreator: VideoCreator,
    scene: Transform,
    durationSeconds: Double, sampleRate: Int, audioSources: List<Audio>,
    val motionBlurSteps: Int, val shutterPercentage: Float,
    val output: File
) : AudioCreator(scene, durationSeconds, videoCreator.totalFrameCount, sampleRate, audioSources) {

    fun start() {
        thread { run() }
    }

    fun run() {
        val vbt = VideoBackgroundTask(videoCreator, scene, camera, motionBlurSteps, shutterPercentage)
        vbt.start()
        // wait for the task to finish
        waitUntil { vbt.isDone }
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