package me.anno.studio.rems

import me.anno.language.translation.NameDesc
import me.anno.objects.Audio
import me.anno.studio.rems.RemsStudio.motionBlurSteps
import me.anno.studio.rems.RemsStudio.project
import me.anno.studio.rems.RemsStudio.root
import me.anno.studio.rems.RemsStudio.shutterPercentage
import me.anno.studio.rems.RemsStudio.targetOutputFile
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.base.menu.Menu.msg
import me.anno.utils.types.Strings.defaultImportType
import me.anno.utils.types.Strings.getImportType
import me.anno.video.*
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

object Rendering {

    var isRendering = false

    private val LOGGER = LogManager.getLogger(Rendering::class)

    fun renderPart(size: Int, ask: Boolean) {
        renderVideo(RemsStudio.targetWidth / size, RemsStudio.targetHeight / size, ask)
    }

    fun renderSetPercent(ask: Boolean) {
        renderVideo(
            max(2, (RemsStudio.project!!.targetWidth * RemsStudio.project!!.targetSizePercentage / 100).roundToInt()),
            max(2, (RemsStudio.project!!.targetHeight * RemsStudio.project!!.targetSizePercentage / 100).roundToInt()),
            ask
        )
    }

    fun renderVideo(width: Int, height: Int, ask: Boolean) {

        if (width % 2 != 0 || height % 2 != 0) return renderVideo(
            width / 2 * 2,
            height / 2 * 2,
            ask
        )

        if (isRendering) return onAlreadyRendering()

        val targetOutputFile = findTargetOutputFile(RenderType.VIDEO)
        if (targetOutputFile.exists() && ask) {
            return askOverridingIsAllowed(targetOutputFile) {
                renderVideo(width, height, false)
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
        val scene = root.clone()!!
        val audioSources = scene.listOfAll
            .filterIsInstance<Audio>()
            .filter { it.forcedMeta?.hasAudio == true }.toList()
        val balance = project?.ffmpegBalance ?: FFMPEGEncodingBalance.M0
        val type = project?.ffmpegFlags ?: FFMPEGEncodingType.DEFAULT
        val creator = VideoAudioCreator(
            VideoCreator(
                width, height,
                RemsStudio.targetFPS, totalFrameCount, balance, type,
                if (audioSources.isEmpty()) targetOutputFile else tmpFile
            ), scene, duration, sampleRate, audioSources,
            motionBlurSteps, shutterPercentage, targetOutputFile
        )
        creator.onFinished = { isRendering = false }
        creator.start()

    }

    fun renderFrame(width: Int, height: Int, time: Double, ask: Boolean) {

        val targetOutputFile = findTargetOutputFile(RenderType.FRAME)
        if (targetOutputFile.exists() && ask) {
            return askOverridingIsAllowed(targetOutputFile) {
                renderFrame(width, height, time, false)
            }
        }

        LOGGER.info("Rendering frame at $time, $width x $height")

        FrameTask(
            width, height,
            RemsStudio.targetFPS,
            root.clone()!!,
            motionBlurSteps,
            shutterPercentage,
            time,
            targetOutputFile
        ).start()

    }

    fun renderAudio(ask: Boolean) {

        if (isRendering) return onAlreadyRendering()

        val targetOutputFile = findTargetOutputFile(RenderType.AUDIO)
        if (targetOutputFile.exists() && ask) {
            return askOverridingIsAllowed(targetOutputFile) {
                renderAudio(false)
            }
        }

        isRendering = true
        LOGGER.info("Rendering audio")

        val duration = RemsStudio.targetDuration
        val sampleRate = 48000
        val scene = root.clone()!!
        val audioSources = scene.listOfAll
            .filterIsInstance<Audio>()
            .filter { it.forcedMeta?.hasAudio == true }.toList()
        AudioCreator(scene, duration, sampleRate, audioSources).apply {
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

    enum class RenderType(val importType: String, val extension: String, val defaultName: String = "output.$extension") {
        VIDEO("Video", ".mp4"),
        AUDIO("Audio", ".mp3"),
        FRAME("Image", ".png")
    }

    private fun findTargetOutputFile(type: RenderType): File {
        var targetOutputFile = targetOutputFile
        val defaultExtension = type.extension
        val defaultName = type.defaultName
        do {
            val file0 = targetOutputFile
            if (targetOutputFile.exists() && targetOutputFile.isDirectory) {
                targetOutputFile = File(targetOutputFile, defaultName)
            } else if (!targetOutputFile.name.contains('.')) {
                targetOutputFile = File(targetOutputFile, defaultExtension)
            }
        } while (file0 !== targetOutputFile)
        val importType = targetOutputFile.extension.getImportType()
        if (importType == defaultImportType && RenderType.values().none { importType == it.importType }) {
            LOGGER.warn("The file extension .${targetOutputFile.extension} is unknown! Your export may fail!")
            return targetOutputFile
        }
        val targetType = type.importType
        if (importType != targetType) {
            // wrong extension -> place it automatically
            val fileName = targetOutputFile.nameWithoutExtension + defaultExtension
            return File(targetOutputFile.parentFile, fileName)
        }
        return targetOutputFile
    }

}