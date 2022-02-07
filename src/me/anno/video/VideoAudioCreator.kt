package me.anno.video

import me.anno.io.files.FileReference
import me.anno.utils.Sleep.waitUntil
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread

open class VideoAudioCreator(
    val videoCreator: VideoCreator,
    val videoBackgroundTask: VideoBackgroundTask,
    val audioCreator: AudioCreator,
    val output: FileReference,
) {

    fun start() {
        thread(name = "VideoAudioCreator") { run() }
    }

    fun run() {
        val videoTask = videoBackgroundTask
        videoTask.start()
        // wait for the task to finish
        waitUntil(true) { videoTask.isDone }
        if (audioCreator.hasStreams()) {
            audioCreator.createOrAppendAudio(output, videoCreator.output, true)
        } else {
            if (output != videoCreator.output) {
                output.delete()
                videoCreator.output.renameTo(output)
            }
            LOGGER.info("No audio found, saved result to $output.")
            audioCreator.onFinished()
        }
    }

    var onFinished: () -> Unit
        get() = audioCreator.onFinished
        set(value) {
            audioCreator.onFinished = value
        }

    companion object {
        private val LOGGER = LogManager.getLogger(VideoAudioCreator::class)
    }

}