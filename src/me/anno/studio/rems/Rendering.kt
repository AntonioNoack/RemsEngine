package me.anno.studio.rems

import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.objects.Audio
import me.anno.objects.Transform
import me.anno.objects.Video
import me.anno.objects.modes.VideoType
import me.anno.studio.rems.RemsStudio.motionBlurSteps
import me.anno.studio.rems.RemsStudio.project
import me.anno.studio.rems.RemsStudio.root
import me.anno.studio.rems.RemsStudio.shutterPercentage
import me.anno.studio.rems.RemsStudio.targetOutputFile
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.utils.Threads.threadWithName
import me.anno.utils.types.Strings.defaultImportType
import me.anno.utils.types.Strings.getImportType
import me.anno.video.*
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

// todo f2 to rename something in the tree view
// todo test-playback button not working?
object Rendering {

    var isRendering = false
    val div = 4

    private val LOGGER = LogManager.getLogger(Rendering::class)

    fun renderPart(size: Int, ask: Boolean, callback: () -> Unit) {
        renderVideo(RemsStudio.targetWidth / size, RemsStudio.targetHeight / size, ask, callback)
    }

    fun renderSetPercent(ask: Boolean, callback: () -> Unit) {
        val project = project!!
        renderVideo(
            max(div, (project.targetWidth * project.targetSizePercentage / 100).roundToInt()),
            max(div, (project.targetHeight * project.targetSizePercentage / 100).roundToInt()),
            ask, callback
        )
    }

    fun filterAudio(scene: Transform): List<Audio> {
        return scene.listOfAll
            .filterIsInstance<Audio>()
            .filter {
                it.forcedMeta?.hasAudio == true && (it.amplitude.isAnimated || it.amplitude[0.0] * 32e3f > 1f)
            }.toList()
    }

    fun renderVideo(width: Int, height: Int, ask: Boolean, callback: () -> Unit) {

        if (width % div != 0 || height % div != 0) return renderVideo(
            width / div * div,
            height / div * div,
            ask, callback
        )

        if (isRendering) return onAlreadyRendering()

        val targetOutputFile = findTargetOutputFile(RenderType.VIDEO)
        if (targetOutputFile.exists && ask) {
            return askOverridingIsAllowed(targetOutputFile) {
                renderVideo(width, height, false, callback)
            }
        }

        val isGif = targetOutputFile.extension.equals("gif", true)

        isRendering = true
        LOGGER.info("Rendering video at $width x $height")

        val duration = RemsStudio.targetDuration
        val tmpFile = getTmpFile(targetOutputFile)
        val fps = RemsStudio.targetFPS
        val totalFrameCount = max(1, (fps * duration).toLong() + 1)
        val sampleRate = max(1, RemsStudio.targetSampleRate)

        val scene = root.clone()

        // todo make gifs with audio
        val audioSources = if (isGif) emptyList() else filterAudio(scene)

        val balance = project?.ffmpegBalance ?: FFMPEGEncodingBalance.M0
        val type = project?.ffmpegFlags ?: FFMPEGEncodingType.DEFAULT

        val videoCreator = VideoCreator(
            width, height,
            RemsStudio.targetFPS, totalFrameCount, balance, type,
            if (audioSources.isEmpty()) targetOutputFile else tmpFile
        )

        val creator = VideoAudioCreator(
            videoCreator, scene, duration, sampleRate, audioSources,
            motionBlurSteps, shutterPercentage, targetOutputFile
        )

        creator.onFinished = {
            isRendering = false
            callback()
        }

        videoCreator.init()
        creator.start()

    }

    fun getTmpFile(file: File) =
        File(file.parentFile, file.nameWithoutExtension + ".tmp." + targetOutputFile.extension)

    fun getTmpFile(file: FileReference) =
        getReference(file.getParent(), file.nameWithoutExtension + ".tmp." + targetOutputFile.extension)

    fun renderFrame(width: Int, height: Int, time: Double, ask: Boolean, callback: () -> Unit) {

        val targetOutputFile = findTargetOutputFile(RenderType.FRAME)
        if (targetOutputFile.exists && ask) {
            return askOverridingIsAllowed(targetOutputFile) {
                renderFrame(width, height, time, false, callback)
            }
        }

        LOGGER.info("Rendering frame at $time, $width x $height")

        FrameTask(
            width, height,
            RemsStudio.targetFPS,
            root.clone(),
            motionBlurSteps[time],
            shutterPercentage[time],
            time,
            targetOutputFile
        ).start(callback)

    }

    fun overrideAudio(video: FileReference, ask: Boolean, callback: () -> Unit) {

        if (isRendering) return onAlreadyRendering()

        val targetOutputFile = findTargetOutputFile(RenderType.VIDEO)
        if (targetOutputFile.exists && ask) {
            return askOverridingIsAllowed(targetOutputFile) {
                overrideAudio(video, false, callback)
            }
        }

        if (video == InvalidRef || !video.exists || video == targetOutputFile) {
            // ask which video should be selected
            val videos = root.listOfAll.filterIsInstance<Video>()
                .filter {
                    it.file != InvalidRef && it.file.exists &&
                            it.type == VideoType.VIDEO && it.file != video &&
                            it.file != targetOutputFile
                }.toList()
            when (videos.size) {
                0 -> {
                    // warning / file selector
                    LOGGER.warn("Could not find video in scene!")
                }
                // ok :)
                1 -> overrideAudio(videos[0].file, ask, callback)
                // we need to ask
                else -> {
                    // todo ask which video is the right one
                    openMenu(
                        NameDesc(
                            "Select the target video",
                            "Where the video part is defined; will also decide the length",
                            "ui.rendering.selectTargetVideo"
                        ),
                        videos.map {
                            MenuOption(NameDesc(it.name.ifEmpty { it.file.name }, it.file.absolutePath, "")) {
                                overrideAudio(it.file, ask, callback)
                            }
                        }
                    )
                }
            }
        } else {

            val meta = getMeta(video, false)!!

            isRendering = true
            LOGGER.info("Rendering audio onto video")

            val duration = meta.duration
            val sampleRate = max(1, RemsStudio.targetSampleRate)

            val scene = root.clone()
            val audioSources = filterAudio(scene)

            // if empty, skip?
            LOGGER.info("Found ${audioSources.size} audio sources")

            AudioCreator(scene, duration, sampleRate, audioSources).apply {
                onFinished = {
                    isRendering = false
                    callback()
                }
                threadWithName("Rendering::renderAudio()") {
                    createOrAppendAudio(targetOutputFile, video, false)
                }
            }

        }
    }

    fun renderAudio(ask: Boolean, callback: () -> Unit) {

        if (isRendering) return onAlreadyRendering()

        val targetOutputFile = findTargetOutputFile(RenderType.AUDIO)
        if (targetOutputFile.exists && ask) {
            return askOverridingIsAllowed(targetOutputFile) {
                renderAudio(false, callback)
            }
        }

        isRendering = true
        LOGGER.info("Rendering audio")

        val duration = RemsStudio.targetDuration
        val sampleRate = max(1, RemsStudio.targetSampleRate)

        val scene = root.clone()
        val audioSources = filterAudio(scene)

        // todo if is empty, send a warning instead of doing something

        AudioCreator(scene, duration, sampleRate, audioSources).apply {
            onFinished = {
                isRendering = false
                callback()
            }
            threadWithName("Rendering::renderAudio()") {
                createOrAppendAudio(targetOutputFile, null, false)
            }
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

    private fun askOverridingIsAllowed(targetOutputFile: FileReference, callback: () -> Unit) {
        ask(NameDesc("Override %1?").with("%1", targetOutputFile.name), callback)
    }

    enum class RenderType(
        val importType: String,
        val extension: String,
        val defaultName: String = "output.$extension"
    ) {
        VIDEO("Video", ".mp4"),
        AUDIO("Audio", ".mp3"),
        FRAME("Image", ".png")
    }

    fun findTargetOutputFile(type: RenderType): FileReference {
        var targetOutputFile = targetOutputFile
        val defaultExtension = type.extension
        val defaultName = type.defaultName
        do {
            val file0 = targetOutputFile
            if (targetOutputFile.exists && targetOutputFile.isDirectory) {
                targetOutputFile = getReference(targetOutputFile, defaultName)
            } else if (!targetOutputFile.name.contains('.')) {
                targetOutputFile = getReference(targetOutputFile, defaultExtension)
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
            return getReference(targetOutputFile.getParent(), fileName)
        }
        return targetOutputFile
    }

}