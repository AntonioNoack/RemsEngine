package me.anno.video

import me.anno.animation.AnimatedProperty
import me.anno.io.files.FileReference
import me.anno.remsstudio.objects.Audio
import me.anno.remsstudio.objects.Transform
import me.anno.utils.Sleep.waitUntil
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread

class VideoAudioCreator(
    val videoCreator: VideoCreator,
    scene: Transform,
    durationSeconds: Double, sampleRate: Int, audioSources: List<Audio>,
    val motionBlurSteps: AnimatedProperty<Int>, val shutterPercentage: AnimatedProperty<Float>,
    val output: FileReference
) : AudioCreator(scene, durationSeconds, sampleRate, audioSources) {

    fun start() {
        thread(name = "VideoAudioCreator") { run() }
    }

    fun run() {
        val vbt = VideoBackgroundTask(videoCreator, scene, camera, motionBlurSteps, shutterPercentage)
        vbt.start()
        // wait for the task to finish
        waitUntil(true) { vbt.isDone }
        if (audioSources.isEmpty()) {
            if (output != videoCreator.output) {
                output.delete()
                videoCreator.output.renameTo(output)
            }
            LOGGER.info("No audio found, saved result to $output.")
            onFinished()
        } else {
            createOrAppendAudio(output, videoCreator.output, true)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(VideoAudioCreator::class)
    }

}