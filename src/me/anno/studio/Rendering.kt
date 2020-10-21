package me.anno.studio

import me.anno.gpu.GFX
import me.anno.video.VideoAudioCreator
import me.anno.video.VideoCreator
import org.apache.logging.log4j.LogManager
import java.io.File

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
        if(isRendering){
            GFX.openMenu("Rendering already in progress!", listOf(
                "Ok" to {}
            ))
            return
        }
        isRendering = true
        LOGGER.info("Rendering video at $width x $height")
        val tmpFile = File(
            RemsStudio.targetOutputFile.parentFile,
            RemsStudio.targetOutputFile.nameWithoutExtension + ".tmp." + RemsStudio.targetOutputFile.extension
        )
        val fps = RemsStudio.targetFPS
        val totalFrameCount = (fps * RemsStudio.targetDuration).toInt()
        val sampleRate = 48000
        val creator = VideoAudioCreator(
            VideoCreator(
                width, height,
                RemsStudio.targetFPS, totalFrameCount, tmpFile
            ), sampleRate, RemsStudio.targetOutputFile
        )
        creator.onFinished = { isRendering = false }
        creator.start()
    }

}