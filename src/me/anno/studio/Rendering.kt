package me.anno.studio

import me.anno.gpu.GFX
import me.anno.objects.Audio
import me.anno.studio.RemsStudio.motionBlurSteps
import me.anno.studio.RemsStudio.shutterPercentage
import me.anno.studio.RemsStudio.targetOutputFile
import me.anno.video.VideoAudioCreator
import me.anno.video.VideoCreator
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.math.max

object Rendering {

    private val LOGGER = LogManager.getLogger(Rendering::class)

    fun renderPart(size: Int) {
        render(RemsStudio.targetWidth / size, RemsStudio.targetHeight / size)
    }

    var isRendering = false
    fun render(width: Int, height: Int) {
        if (width % 2 != 0 || height % 2 != 0) return render(
            width / 2 * 2,
            height / 2 * 2
        )
        if (isRendering) {
            GFX.openMenu("Rendering already in progress!", listOf(
                "Ok" to {}
            ))
            return
        }
        isRendering = true
        LOGGER.info("Rendering video at $width x $height")
        val targetOutputFile = targetOutputFile
        val tmpFile = File(
            targetOutputFile.parentFile,
            targetOutputFile.nameWithoutExtension + ".tmp." + targetOutputFile.extension
        )
        val fps = RemsStudio.targetFPS
        val totalFrameCount = max(1, (fps * RemsStudio.targetDuration).toInt() + 1)
        val sampleRate = 48000
        val audioSources = RemsStudio.root.listOfAll
            .filterIsInstance<Audio>()
            .filter { it.forcedMeta?.hasAudio == true }.toList()
        val creator = VideoAudioCreator(
            VideoCreator(
                width, height,
                RemsStudio.targetFPS, totalFrameCount,
                if (audioSources.isEmpty()) targetOutputFile else tmpFile
            ), sampleRate, motionBlurSteps, shutterPercentage, audioSources, targetOutputFile
        )
        creator.onFinished = { isRendering = false }
        creator.start()
    }

}