package me.anno.studio.rems

import me.anno.language.translation.NameDesc
import me.anno.objects.Audio
import me.anno.studio.rems.RemsStudio.motionBlurSteps
import me.anno.studio.rems.RemsStudio.shutterPercentage
import me.anno.studio.rems.RemsStudio.targetOutputFile
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.base.menu.Menu.msg
import me.anno.utils.types.Strings.defaultImportType
import me.anno.utils.types.Strings.getImportType
import me.anno.video.AudioCreator
import me.anno.video.VideoAudioCreator
import me.anno.video.VideoCreator
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

object Rendering {

    var isRendering = false

    private val LOGGER = LogManager.getLogger(Rendering::class)

    fun renderPart(size: Int, ask: Boolean) {
        render(RemsStudio.targetWidth / size, RemsStudio.targetHeight / size, ask)
    }

    fun renderSetPercent(ask: Boolean) {
        render(
            max(2, (RemsStudio.project!!.targetWidth * RemsStudio.project!!.targetSizePercentage / 100).roundToInt()),
            max(2, (RemsStudio.project!!.targetHeight * RemsStudio.project!!.targetSizePercentage / 100).roundToInt()),
            ask
        )
    }

    fun render(width: Int, height: Int, ask: Boolean) {

        if (width % 2 != 0 || height % 2 != 0) return render(
            width / 2 * 2,
            height / 2 * 2,
            ask
        )

        if (isRendering) return onAlreadyRendering()

        val targetOutputFile = findTargetOutputFile(false)
        if (targetOutputFile.exists() && ask) {
            return askOverridingIsAllowed(targetOutputFile) {
                render(width, height, false)
            }
        }

        isRendering = true
        LOGGER.info("Rendering video at $width x $height")

        val duration = RemsStudio.targetDuration
        val tmpFile = File(
            targetOutputFile.parentFile,
            targetOutputFile.nameWithoutExtension + ".tmp." + targetOutputFile.extension
        )
        val fps = RemsStudio.targetFPS
        val totalFrameCount = max(1, (fps * duration).toInt() + 1)
        val sampleRate = 48000
        val audioSources = RemsStudio.root.listOfAll
            .filterIsInstance<Audio>()
            .filter { it.forcedMeta?.hasAudio == true }.toList()
        val creator = VideoAudioCreator(
            VideoCreator(
                width, height,
                RemsStudio.targetFPS, totalFrameCount,
                if (audioSources.isEmpty()) targetOutputFile else tmpFile
            ), duration, sampleRate, audioSources,
            motionBlurSteps, shutterPercentage, targetOutputFile
        )
        creator.onFinished = { isRendering = false }
        creator.start()

    }

    fun renderAudio(ask: Boolean) {

        if (isRendering) return onAlreadyRendering()

        val targetOutputFile = findTargetOutputFile(true)
        if (targetOutputFile.exists() && ask) {
            return askOverridingIsAllowed(targetOutputFile) {
                renderAudio(false)
            }
        }

        isRendering = true
        LOGGER.info("Rendering audio")

        val duration = RemsStudio.targetDuration
        val sampleRate = 48000
        val audioSources = RemsStudio.root.listOfAll
            .filterIsInstance<Audio>()
            .filter { it.forcedMeta?.hasAudio == true }.toList()
        AudioCreator(duration, sampleRate, audioSources).apply {
            onFinished = { isRendering = false }
            createOrAppendAudio(targetOutputFile, null)
        }

    }

    private fun onAlreadyRendering() {
        msg(
            NameDesc(
                "Rendering already in progress!",
                "If you think, this is an error, please restart!",
                "ui.warn.renderingInProgress"
            )
        )
    }

    private fun askOverridingIsAllowed(targetOutputFile: File, callback: () -> Unit) {
        ask(NameDesc("Override %1?").with("%1", targetOutputFile.name), callback)
    }

    private fun findTargetOutputFile(expectsAudio: Boolean): File {
        var targetOutputFile = targetOutputFile
        val defaultExtension = if (expectsAudio) ".mp3" else ".mp4"
        val defaultName = if (expectsAudio) "output.mp3" else "output.mp4"
        do {
            val file0 = targetOutputFile
            if (targetOutputFile.exists() && targetOutputFile.isDirectory) {
                targetOutputFile = File(targetOutputFile, defaultName)
            } else if (!targetOutputFile.name.contains('.')) {
                targetOutputFile = File(targetOutputFile, defaultExtension)
            }
        } while (file0 !== targetOutputFile)
        val targetType = if (expectsAudio) "Audio" else "Video"
        val importType = targetOutputFile.extension.getImportType()
        if (importType == defaultImportType && importType != "Video" && importType != "Audio") {
            LOGGER.warn("The file extension .${targetOutputFile.extension} is unknown! Your export may fail!")
            return targetOutputFile
        }
        if (importType != targetType) {
            // wrong extension -> place it automatically
            val fileName = targetOutputFile.nameWithoutExtension + defaultExtension
            return File(targetOutputFile.parentFile, fileName)
        }
        return targetOutputFile
    }

}