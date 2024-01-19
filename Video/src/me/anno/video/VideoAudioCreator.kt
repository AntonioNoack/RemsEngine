package me.anno.video

import me.anno.io.files.FileReference
import me.anno.utils.ShutdownException
import me.anno.utils.Sleep.waitUntil
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread

@Suppress("unused")
open class VideoAudioCreator(
    val videoCreator: VideoCreator,
    val videoBackgroundTask: VideoBackgroundTask,
    val audioCreator: AudioCreator,
    val output: FileReference,
) {

    fun start() {
        thread(name = "VideoAudioCreator") {
            try {
                run()
            } catch (ignored: ShutdownException) {
                LOGGER.debug("Rendering has been cancelled")
            }
        }
    }

    fun run() {
        val videoTask = videoBackgroundTask
        videoTask.start()
        // wait for the task to finish
        waitUntil(true) { videoTask.isDone || videoTask.isCancelled }
        if (isCancelled) {
            onFinished()
        } else if (audioCreator.hasStreams()) {
            audioCreator.createOrAppendAudio(output, videoCreator.output, true)
        } else {
            if (output != videoCreator.output) {
                output.delete()
                videoCreator.output.renameTo(output)
            }
            LOGGER.info("No audio found, saved result to $output.")
            onFinished()
        }
    }

    var onFinished: () -> Unit
        get() = audioCreator.onFinished
        set(value) {
            audioCreator.onFinished = value
        }

    var isCancelled
        get() = audioCreator.isCancelled || videoBackgroundTask.isCancelled
        set(value) {
            audioCreator.isCancelled = value
            videoBackgroundTask.isCancelled = value
        }

    companion object {
        private val LOGGER = LogManager.getLogger(VideoAudioCreator::class)
    }
}